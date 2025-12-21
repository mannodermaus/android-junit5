@file:Suppress("UNCHECKED_CAST")

import groovy.lang.Closure
import groovy.util.Node
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.io.File

/**
 * Configure deployment tasks and properties for a project using the provided [deployConfig].
 */
fun Project.configureDeployment(deployConfig: Deployed) {
    check(this != rootProject) { "This method can not be called on the root project" }

    val credentials = DeployedCredentials(this)

    // Configure root project (this only happens once and will raise an error on inconsistent data)
    rootProject.configureRootDeployment(deployConfig, credentials)

    // Apply deployment config for each type of project
    plugins.withId("com.android.library") { configureAndroidDeployment(deployConfig, credentials) }
    plugins.withId("java-gradle-plugin") { configurePluginDeployment(deployConfig, credentials) }
}

/* Private */

private fun Project.configureRootDeployment(
    deployConfig: Deployed,
    credentials: DeployedCredentials
) {
    check(this == rootProject) { "This method can only be called on the root project" }

    // Validate the integrity of published versions
    // (all subprojects must use the same group ID and version number or else an error is raised)
    if (version != "unspecified") {
        check(version == deployConfig.currentVersion && group == deployConfig.groupId) {
            "A subproject tried to set '${deployConfig.groupId}:${deployConfig.currentVersion}' " +
                    "as the coordinates for the artifacts of the repository, but '$group:$version' was already set " +
                    "previously by a different subproject. As per the requirements of the Nexus Publishing plugin, " +
                    "all subprojects must use the same version number! Please check Environment.kt for inconsistencies!"
        }

        // Already configured and correct
        return
    }

    // One-time initialization beyond this point
    group = deployConfig.groupId
    version = deployConfig.currentVersion

    centralPublishing(
        packageGroup = deployConfig.groupId,
        stagingProfileId = credentials.sonatypeStagingProfileId,
        username = credentials.centralUsername,
        password = credentials.centralPassword
    )
}

private fun Project.configureCommonDeployment(
    deployConfig: Deployed,
    credentials: DeployedCredentials
) {
    apply {
        plugin("maven-publish")
        plugin("signing")
        plugin("org.jetbrains.dokka")
    }

    // Setup publication details
    group = deployConfig.groupId
    version = deployConfig.currentVersion

    // Setup code signing
    ext["signing.keyId"] = credentials.signingKeyId
    ext["signing.password"] = credentials.signingPassword
    ext["signing.secretKeyRingFile"] = credentials.signingKeyRingFile
    signing {
        sign(publishing.publications)
    }
}

private fun Project.configureAndroidDeployment(
    deployConfig: Deployed,
    credentials: DeployedCredentials
) {
    val android = AndroidDsl(this)
    configureCommonDeployment(deployConfig, credentials)

    // Create a publication for each variant
    SupportedJUnit.values().forEach { junit ->
        val variantName = "${junit.label}Release"

        android.publishing.singleVariant(variantName) {
            withSourcesJar()
            withJavadocJar()
        }

        afterEvaluate {
            publishing {
                publications {
                    register<MavenPublication>(junit.label) {
                        from(components.getByName(variantName))
                        groupId = deployConfig.groupId
                        artifactId = buildString {
                            // Attach optional suffix to Android artifacts
                            // to distinguish between different JUnit targets
                            append(deployConfig.artifactId)
                            junit.artifactIdSuffix?.let {
                                append('-')
                                append(it)
                            }
                        }
                        version = deployConfig.currentVersion

                        configurePom(deployConfig)
                    }
                }
            }
        }
    }
}

private fun Project.configurePluginDeployment(
    deployConfig: Deployed,
    credentials: DeployedCredentials
) {
    configureCommonDeployment(deployConfig, credentials)

    // Create artifact tasks
    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")

        // This declaration includes Kotlin & Groovy source directories
        from(sourceSets.main.allJava.srcDirs)
    }

    // Create javadoc artifact
    val javadocJar = tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")

        // Connect to Dokka for generation of docs
        from(layout.buildDirectory.dir("dokka/html"))
        dependsOn("dokkaGenerate")
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }

    // Connect signing task to the JAR produced by the artifact-producing task
    tasks.withType(Sign::class.java).configureEach {
        dependsOn("assemble")
    }

    publishing {
        publications {
            all {
                if (this is MavenPublication) {
                    if (name == "pluginMaven") {
                        applyPublicationDetails(
                            project = this@configurePluginDeployment,
                            deployConfig = deployConfig,
                            contentJar = layout.buildDirectory.file("libs/${project.name}-$version.jar"),
                            sourcesJar = sourcesJar,
                            javadocJar = javadocJar
                        )
                    }

                    // Always extend POM details to satisfy Maven Central's POM validation
                    // (they require a bunch of metadata that isn't filled out by default)
                    configurePom(deployConfig)
                }
            }
        }
    }
}

private fun MavenPublication.applyPublicationDetails(
    project: Project,
    deployConfig: Deployed,
    contentJar: Provider<RegularFile>,
    sourcesJar: TaskProvider<*>,
    javadocJar: TaskProvider<*>
) = also {
    groupId = deployConfig.groupId
    artifactId = deployConfig.artifactId
    version = deployConfig.currentVersion

    // Attach artifacts
    artifacts.clear()
    artifact(contentJar)
    artifact(sourcesJar)
    artifact(javadocJar)

    // Attach dependency information
    pom {
        withXml {
            with(asNode()) {
                // Only add dependencies manually if there aren't any already
                if (children().filterIsInstance<Node>()
                        .none { it.name().toString().endsWith("dependencies") }
                ) {
                    val dependenciesNode = appendNode("dependencies")

                    val compileDeps = project.configurations.getByName("api").allDependencies
                    val runtimeDeps =
                        project.configurations.getByName("implementation").allDependencies +
                                project.configurations.getByName("runtimeOnly").allDependencies -
                                compileDeps

                    val dependencies = mapOf(
                        "runtime" to runtimeDeps,
                        "compile" to compileDeps
                    )

                    dependencies
                        .mapValues { entry -> entry.value.filter { it.name != "unspecified" } }
                        .forEach { (scope, dependencies) ->
                            dependencies.forEach { dep ->
                                // Do not allow BOM dependencies for our own packaged libraries,
                                // instead its artifact versions should be unrolled explicitly
                                require("-bom" !in dep.name) {
                                    "Found a BOM declaration in the dependencies of project" +
                                            "${project.path}: $dep. Prefer declaring its " +
                                            "transitive artifacts explicitly by " +
                                            "adding a version constraint to them."
                                }

                                with(dependenciesNode.appendNode("dependency")) {
                                    if (dep is ProjectDependency) {
                                        appendProjectDependencyCoordinates(dep)
                                    } else {
                                        appendExternalDependencyCoordinates(dep)
                                    }

                                    appendNode("scope", scope)
                                }
                            }
                        }
                }
            }
        }
    }
}

private fun Node.appendProjectDependencyCoordinates(dep: ProjectDependency) {
    // Find the external coordinates for the given project dependency
    val projectName = dep.name

    val config = Artifacts.Instrumentation::class.java
        .getMethod("get${projectName.uppercaseFirstChar()}")
        .invoke(Artifacts.Instrumentation)
            as Deployed

    appendNode("groupId", config.groupId)
    appendNode("artifactId", config.artifactId)
    appendNode("version", config.currentVersion)
}

private fun Node.appendExternalDependencyCoordinates(dep: Dependency) {
    appendNode("groupId", dep.group)
    appendNode("artifactId", dep.name)
    dep.version?.let { appendNode("version", it) }
}

private fun MavenPublication.configurePom(deployConfig: Deployed) = also {
    pom {
        // Name and description cannot be set directly through the property, since they somehow aren't applied
        // to Gradle Plugin Marker's POM file (maybe that plugin removes them somehow). Therefore,
        // use the XML builder for this node as these properties are still required by Maven Central
        withXml {
            with(asNode()) {
                appendNode("name").setValue(deployConfig.artifactId)
                appendNode("description").setValue(deployConfig.description)
            }
        }

        url.set(Artifacts.GITHUB_URL)

        licenses {
            license {
                name.set(Artifacts.LICENSE)
                url.set("${Artifacts.GITHUB_URL}/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("mannodermaus")
                name.set("Marcel Schnelle")
            }
        }

        scm {
            connection.set("scm:git:${Artifacts.GITHUB_REPO}.git")
            developerConnection.set("scm:git:ssh://github.com/${Artifacts.GITHUB_REPO}.git")
            url.set("${Artifacts.GITHUB_URL}/tree/main")
        }
    }
}

/*
 * Type-safe accessor hacks for Kotlin's strictness, accessing properties without having
 * access to these external plugins. This is ridiculously ugly, so readers beware.
 */

// Properties

private val Project.ext: ExtraPropertiesExtension
    get() = extensions.getByName("ext") as ExtraPropertiesExtension

/**
 * Allows us to retain the untyped Groovy API even in the stricter Kotlin context
 * ("android.sourceSets.main.java.srcDirs")
 */
private class AndroidDsl(project: Project) {
    private val delegate = project.extensions.getByName("android") as ExtensionAware

    val sourceSets = SourceSetDsl(delegate)
    val publishing = PublishingDsl(delegate)
    val productFlavors = ProductFlavorsDsl(delegate)

    class SourceSetDsl(android: ExtensionAware) {
        private val delegate = android.javaClass.getDeclaredMethod("getSourceSets")
            .also { it.isAccessible = true }
            .invoke(android) as NamedDomainObjectCollection<Any>

        val main = MainDsl(delegate)

        class MainDsl(sourceSets: NamedDomainObjectCollection<Any>) {
            private val delegate = sourceSets.named("main").get()

            val kotlin = KotlinDsl(delegate)

            class KotlinDsl(main: Any) {
                val srcDirs = main.javaClass
                    .getDeclaredMethod("getKotlinDirectories")
                    .invoke(main) as Set<File>
            }
        }
    }

    class PublishingDsl(android: ExtensionAware) {
        private val delegate = android.javaClass.getDeclaredMethod("getPublishing")
            .also { it.isAccessible = true }
            .invoke(android)

        fun singleVariant(name: String, block: SingleVariantDsl.() -> Unit) {
            delegate.javaClass
                .getDeclaredMethod("singleVariant", String::class.java, Closure::class.java)
                .also { it.isAccessible = true }
                .invoke(delegate, name, closureOf<Any> {
                    SingleVariantDsl(this).block()
                })
        }

        fun multipleVariants(block: MultipleVariantsDsl.() -> Unit) {
            MultipleVariantsDsl(delegate).block()
        }

        class SingleVariantDsl(private val delegate: Any) {
            fun withSourcesJar() {
                delegate.javaClass.declaredMethods
                    .first { it.name.startsWith("withSourcesJar") }
                    .also { it.isAccessible = true }
                    .invoke(delegate, true)
            }

            fun withJavadocJar() {
                delegate.javaClass.declaredMethods
                    .first { it.name.startsWith("withJavadocJar") }
                    .also { it.isAccessible = true }
                    .invoke(delegate, true)
            }
        }

        class MultipleVariantsDsl(publishing: Any) {
            private lateinit var delegate: Any

            init {
                publishing.javaClass
                    .getDeclaredMethod("multipleVariants", Closure::class.java)
                    .also { it.isAccessible = true }
                    .invoke(publishing, closureOf<Any> { delegate = this })
            }

            fun allVariants() {
                delegate.javaClass.declaredMethods
                    .first { it.name.startsWith("allVariants") }
                    .also { it.isAccessible = true }
                    .invoke(delegate, true)
            }

            fun includeBuildTypeValues(vararg buildTypes: String) {
                delegate.javaClass.declaredMethods
                    .first { it.name.startsWith("setIncludedBuildTypes") }
                    .also { it.isAccessible = true }
                    .invoke(delegate, buildTypes.toSet())
            }

            fun withSourcesJar() {
                delegate.javaClass.declaredMethods
                    .first { it.name.startsWith("withSourcesJar") }
                    .also { it.isAccessible = true }
                    .invoke(delegate, true)
            }

            fun withJavadocJar() {
                delegate.javaClass.declaredMethods
                    .first { it.name.startsWith("withJavadocJar") }
                    .also { it.isAccessible = true }
                    .invoke(delegate, true)
            }
        }
    }

    class ProductFlavorsDsl(android: ExtensionAware) {
        private val delegate = android.javaClass.getDeclaredMethod("getProductFlavors")
            .also { it.isAccessible = true }
            .invoke(android) as NamedDomainObjectCollection<Any>

        fun all(block: SupportedJUnit.() -> Unit) {
            delegate.all {
                val flavorName = this.javaClass.getDeclaredMethod("getName").invoke(this) as String
                val junit = SupportedJUnit.fromLabel(flavorName)
                block(junit)
            }
        }
    }
}

private class SourceSetDsl(project: Project) {
    private val delegate = project.extensions.getByName("sourceSets") as SourceSetContainer

    val main: SourceSet = delegate.named("main").get()
}

private val Project.sourceSets
    get() = SourceSetDsl(this)

// Publishing Plugin facade

private fun Project.publishing(action: PublishingExtension.() -> Unit) {
    extensions.configure("publishing", action)
}

private val Project.publishing: PublishingExtension
    get() = extensions.getByName("publishing") as PublishingExtension

// Signing Plugin facade

private fun Project.signing(action: SigningExtension.() -> Unit) {
    extensions.configure("signing", action)
}

// Nexus Staging & Publishing Plugins facade

private fun Project.centralPublishing(
    packageGroup: String,
    stagingProfileId: String?,
    username: String?,
    password: String?
) {
    extensions.getByName("nexusPublishing").withGroovyBuilder {
        setProperty("packageGroup", packageGroup)

        "repositories" {
            "sonatype" {
                // 🤮
                val cls = delegate.javaClass

                cls.getDeclaredMethod("setStagingProfileId", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(delegate, stagingProfileId)

                cls.getDeclaredMethod("setUsername", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(delegate, username)

                cls.getDeclaredMethod("setPassword", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(delegate, password)

                cls.getDeclaredMethod("setNexusUrl", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(
                        delegate,
                        uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
                    )

                cls.getDeclaredMethod("setSnapshotRepositoryUrl", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(
                        delegate,
                        uri("https://central.sonatype.com/repository/maven-snapshots/")
                    )
            }
        }
    }
}
