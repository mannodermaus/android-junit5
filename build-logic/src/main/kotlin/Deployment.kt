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
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.plugins.signing.SigningExtension
import java.io.File

/**
 * Configure deployment tasks and properties for a project using the provided [deployConfig].
 */
fun Project.configureDeployment(deployConfig: Deployed) {
    val credentials = DeployedCredentials(this)

    val isSnapshot = version.toString().endsWith("SNAPSHOT")
    val isAndroid = plugins.findPlugin("com.android.library") != null
    val isGradlePlugin = plugins.hasPlugin("java-gradle-plugin")

    apply {
        plugin("de.marcphilipp.nexus-publish")
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
                    if (this is MavenPublication && name == "pluginMaven") {
                        applyPublicationDetails(
                            project = this@configureDeployment,
                            deployConfig = deployConfig,
                            isAndroid = isAndroid,
                            androidSourcesJar = androidSourcesJar,
                            javadocJar = javadocJar
                        )
                    }
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
            }
        }
        repositories {
            maven {
                name = "central"

                setUrl(
                    if (isSnapshot) {
                        "https://oss.sonatype.org/content/repositories/snapshots/"
                    } else {
                        "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                    }
                )

                credentials {
                    username = credentials.ossrhUsername
                    password = credentials.ossrhPassword
                }
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

    // Setup deployment
    nexusStaging(
        packageGroup = deployConfig.groupId,
        stagingProfileId = credentials.sonatypeStagingProfileId,
        username = credentials.ossrhUsername,
        password = credentials.ossrhPassword
    )

    nexusPublishing(
        sonatypeUsername = credentials.ossrhUsername,
        sonatypePassword = credentials.ossrhPassword
    )

    // Catch-all deployment task for multiple modules
    val deployTask = tasks.maybeCreate("deploy")
    val publishTask = tasks.getByName("publishToSonatype")

    deployTask.finalizedBy(publishTask)
    if (!isSnapshot) {
        publishTask.finalizedBy(":closeAndReleaseRepository")
    }
}

/* Private */

private fun MavenPublication.applyPublicationDetails(
    project: Project,
    deployConfig: Deployed,
    isAndroid: Boolean,
    androidSourcesJar: Jar,
    javadocJar: Jar
) {
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

    pom {
        name.set(deployConfig.artifactId)
        description.set(deployConfig.description)
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

private fun Project.nexusStaging(
    packageGroup: String,
    stagingProfileId: String?,
    username: String?,
    password: String?
) {
    rootProject.extensions.getByName("nexusStaging").withGroovyBuilder {
        setProperty("packageGroup", packageGroup)
        setProperty("stagingProfileId", stagingProfileId)
        setProperty("username", username)
        setProperty("password", password)
    }
}

private fun Project.nexusPublishing(sonatypeUsername: String?, sonatypePassword: String?) {
    extensions.getByName("nexusPublishing").withGroovyBuilder {
        "repositories" {
            "sonatype"(delegateClosureOf<Any> {
                // 🤮
                javaClass.getDeclaredMethod("setUsername", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(this, sonatypeUsername)

                javaClass.getDeclaredMethod("setPassword", Any::class.java)
                    .also { it.isAccessible = true }
                    .invoke(this, sonatypePassword)
            })
        }
    }
}
