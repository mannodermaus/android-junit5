@file:Suppress("UNCHECKED_CAST")

import groovy.util.Node
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.plugins.signing.SigningExtension
import java.io.File

/**
 * Configure deployment tasks and properties for a project using the provided [deployConfig].
 */
fun Project.configureDeployment(deployConfig: Deployed) {
    if (this == rootProject) {
        throw IllegalStateException("This method can not be called on the root project")
    }

    val credentials = DeployedCredentials(this)

    // Configure root project (this only happens once
    // and will raise an error on inconsistent data)
    rootProject.configureRootDeployment(deployConfig, credentials)

    val isAndroid = plugins.findPlugin("com.android.library") != null
    val isGradlePlugin = plugins.hasPlugin("java-gradle-plugin")

    apply {
        plugin("maven-publish")
        plugin("signing")
        plugin("org.jetbrains.dokka")
    }

    // Create artifact tasks
    val androidSourcesJar = tasks.create("androidSourcesJar", Jar::class.java) {
        archiveClassifier.set("sources")

        if (isAndroid) {
            from(android.sourceSets.main.java.srcDirs)
        } else {
            from(sourceSets.main.java.srcDirs)
        }
    }

    val javadocJar = tasks.create("javadocJar", Jar::class.java) {
        archiveClassifier.set("javadoc")

        val dokkaJavadoc = tasks.getByName("dokkaJavadoc")
        dependsOn(dokkaJavadoc)
        from(dokkaJavadoc.property("outputDirectory"))
    }

    artifacts {
        add("archives", androidSourcesJar)
        add("archives", javadocJar)
    }

    // Setup publication details
    group = deployConfig.groupId
    version = deployConfig.currentVersion

    publishing {
        publications {
            // For Gradle Plugin projects, there already is a 'pluginMaven' publication
            // pre-configured by the Java Gradle plugin, which we will extend with more properties and details.
            // For other projects, a new publication must be created instead
            if (isGradlePlugin) {
                all {
                    if (this !is MavenPublication) return@all

                    if (name == "pluginMaven") {
                        applyPublicationDetails(
                            project = this@configureDeployment,
                            deployConfig = deployConfig,
                            isAndroid = isAndroid,
                            androidSourcesJar = androidSourcesJar,
                            javadocJar = javadocJar
                        )
                    }

                    // Always extend POM details to satisfy Maven Central's POM validation
                    // (they require a bunch of metadata for each POM, which isn't filled out by default)
                    configurePom(deployConfig)
                }
            } else {
                create("release", MavenPublication::class.java)
                        .applyPublicationDetails(
                                project = this@configureDeployment,
                                deployConfig = deployConfig,
                                isAndroid = isAndroid,
                                androidSourcesJar = androidSourcesJar,
                                javadocJar = javadocJar
                        )
                        .configurePom(deployConfig)
            }
        }
    }

    // Setup code signing
    ext["signing.keyId"] = credentials.signingKeyId
    ext["signing.password"] = credentials.signingPassword
    ext["signing.secretKeyRingFile"] = credentials.signingKeyRingFile
    signing {
        sign(publishing.publications)
    }
}

/* Private */

private fun Project.configureRootDeployment(deployConfig: Deployed, credentials: DeployedCredentials) {
    if (this != rootProject) {
        throw IllegalStateException("This method can only be called on the root project")
    }

    // Validate the integrity of published versions
    // (all subprojects must use the same group ID and version number or else an error is raised)
    if (version != "unspecified") {
        if (version != deployConfig.currentVersion || group != deployConfig.groupId) {
            throw IllegalStateException("A subproject tried to set '${deployConfig.groupId}:${deployConfig.currentVersion}' " +
                    "as the coordinates for the artifacts of the repository, but '$group:$version' was already set " +
                    "previously by a different subproject. As per the requirements of the Nexus Publishing plugin, " +
                    "all subprojects must use the same version number! Please check Artifacts.kt for inconsistencies!")
        } else {
            // Already configured and correct
            return
        }
    }

    // One-time initialization beyond this point
    group = deployConfig.groupId
    version = deployConfig.currentVersion

    nexusPublishing(
            packageGroup = deployConfig.groupId,
            stagingProfileId = credentials.sonatypeStagingProfileId,
            sonatypeUsername = credentials.ossrhUsername,
            sonatypePassword = credentials.ossrhPassword
    )
}

private fun MavenPublication.applyPublicationDetails(
        project: Project,
        deployConfig: Deployed,
        isAndroid: Boolean,
        androidSourcesJar: Jar,
        javadocJar: Jar
) = also {
    groupId = deployConfig.groupId
    artifactId = deployConfig.artifactId
    version = deployConfig.currentVersion

    artifacts.clear()
    if (isAndroid) {
        artifact("${project.buildDir}/outputs/aar/${project.name}-release.aar")
    } else {
        artifact("${project.buildDir}/libs/${project.name}-${version}.jar")
    }
    artifact(androidSourcesJar)
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
                    val dependencies =
                        project.configurations.getByName("implementation").allDependencies +
                                project.configurations.getByName("runtimeOnly").allDependencies

                    dependencies
                        .filter { it.name != "unspecified" }
                        .forEach {
                            with(dependenciesNode.appendNode("dependency")) {
                                appendNode("groupId", it.group)
                                appendNode("artifactId", it.name)
                                appendNode("version", it.version)
                                appendNode("scope", "runtime")
                            }
                        }
                }
            }
        }
    }
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

        url.set(Artifacts.githubUrl)

        licenses {
            license {
                name.set(Artifacts.license)
                url.set("${Artifacts.githubUrl}/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("mannodermaus")
                name.set("Marcel Schnelle")
            }
        }

        scm {
            connection.set("scm:git:${Artifacts.githubRepo}.git")
            developerConnection.set("scm:git:ssh://github.com/${Artifacts.githubRepo}.git")
            url.set("${Artifacts.githubUrl}/tree/main")
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

    class SourceSetDsl(android: ExtensionAware) {
        private val delegate = android.javaClass.getDeclaredMethod("getSourceSets")
                .also { it.isAccessible = true }
                .invoke(android) as NamedDomainObjectCollection<Any>

        val main = MainDsl(delegate)

        class MainDsl(sourceSets: NamedDomainObjectCollection<Any>) {
            private val delegate = sourceSets.named("main").get()

            val java = JavaDsl(delegate)

            class JavaDsl(main: Any) {
                private val delegate = main.javaClass.getDeclaredMethod("getJava").invoke(main)

                val srcDirs: Set<File> = delegate.javaClass
                        .getDeclaredMethod("getSrcDirs")
                        .invoke(delegate) as Set<File>
            }
        }
    }
}

private val Project.android
    get() = AndroidDsl(this)

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

//private fun Project.nexusStaging(
//    packageGroup: String,
//    stagingProfileId: String?,
//    username: String?,
//    password: String?
//) {
//    rootProject.extensions.getByName("nexusStaging").withGroovyBuilder {
//        setProperty("packageGroup", packageGroup)
//        setProperty("stagingProfileId", stagingProfileId)
//        setProperty("username", username)
//        setProperty("password", password)
//    }
//}

private fun Project.nexusPublishing(
        packageGroup: String,
        stagingProfileId: String?,
        sonatypeUsername: String?,
        sonatypePassword: String?
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
                        .invoke(delegate, sonatypeUsername)

                cls.getDeclaredMethod("setPassword", Any::class.java)
                        .also { it.isAccessible = true }
                        .invoke(delegate, sonatypePassword)
            }
        }
    }
}
