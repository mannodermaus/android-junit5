@file:Suppress("UNCHECKED_CAST")

import de.mannodermaus.gradle.plugins.junit5.Artifact
import de.mannodermaus.gradle.plugins.junit5.Artifacts
import de.mannodermaus.gradle.plugins.junit5.Platform.Android
import de.mannodermaus.gradle.plugins.junit5.Platform.Java

if (!extra.has("deployConfig")) {
  throw IllegalStateException("Deployed module '$name' requires a 'deployConfig'")
}

val deployConfig: Artifact by extra

// ------------------------------------------------------------------------------------------------
// Plugin Configuration
// ------------------------------------------------------------------------------------------------

apply(plugin = "com.jfrog.bintray")

when (deployConfig.platform) {
  is Java -> {
    apply(plugin = "maven")
    apply(plugin = "maven-publish")
  }
  is Android -> {
    apply(plugin = "com.github.dcendents.android-maven")
    apply(plugin = "digital.wup.android-maven-publish")
  }
}

// ------------------------------------------------------------------------------------------------
// Artifacts Configuration
// ------------------------------------------------------------------------------------------------

fun androidSourceDirs(named: String): Iterable<File> =
    extensions["android"].withGroovyBuilder {
      val sourceSets = this.getProperty("sourceSets") as NamedDomainObjectContainer<*>
      return sourceSets.withGroovyBuilder {
        val sourceSet = this.getProperty(named)
        return sourceSet.withGroovyBuilder {
          val java = this.getProperty("java")
          return java.withGroovyBuilder {
            this.getProperty("srcDirs") as Set<File>
          }
        }
      }
    }

fun androidBootClasspath(): Iterable<File> {
  val android = extensions["android"]
  return android.javaClass.getDeclaredMethod("getBootClasspath")
      .invoke(android) as Iterable<File>
}

// Include sources.jar archive in each release
val sourcesJarTaskName = "sourcesJar"
val sourcesJarTask = when (deployConfig.platform) {
  is Java -> {
    tasks.create(sourcesJarTaskName, Jar::class) {
      dependsOn.add("classes")
      baseName = deployConfig.artifactId
      classifier = "sources"
      from(project.the<SourceSetContainer>()["main"].allSource)
    }
  }
  is Android -> {
    tasks.create(sourcesJarTaskName, Jar::class) {
      baseName = deployConfig.artifactId
      classifier = "sources"
      from(androidSourceDirs("main"))
    }
  }
}

// Include javadoc.jar archive in each release
when (deployConfig.platform) {
  is Android -> {
    tasks.create("javadoc", Javadoc::class) {
      source = project.fileTree(androidSourceDirs("main"))
      setExcludes(setOf("**/*.kt"))
      classpath += project.files(androidBootClasspath().joinToString())
    }
  }
}

val javadocJarTask = tasks.create("javadocJar", Jar::class) {
  dependsOn.add("javadoc")
  baseName = deployConfig.artifactId
  classifier = "javadoc"
  from(tasks.getByName<Javadoc>("javadoc").destinationDir)
}

// ------------------------------------------------------------------------------------------------
// Publication Configuration
// ------------------------------------------------------------------------------------------------

project.apply {
  group = deployConfig.groupId
  version = deployConfig.currentVersion
  project.withGroovyBuilder {
    setProperty("archivesBaseName", deployConfig.artifactId)
  }
}

fun Project.publishing() = extensions.getByName("publishing") as PublishingExtension

publishing().apply {
  publications {
    create("library", MavenPublication::class) {
      groupId = deployConfig.groupId
      artifactId = deployConfig.artifactId
      version = deployConfig.currentVersion

      from(components.getByName(deployConfig.platform.name))
      artifact(sourcesJarTask)
      artifact(javadocJarTask)

      pom.withXml {
        val root = asNode()
        root.appendNode("description", deployConfig.description)
        root.appendNode("name", deployConfig.artifactId)
        root.appendNode("url", Artifacts.githubUrl)
      }
    }
  }
}

// Copy POM to location expected by Bintray
val copyPomTask = tasks.create("copyPom", Copy::class) {
  from("build/publications/library")
  into("build/poms")
  include("pom-default.xml")
}
tasks.getByName("publish").dependsOn.add(copyPomTask)

// ------------------------------------------------------------------------------------------------
// Target Configuration
// ------------------------------------------------------------------------------------------------

project.configure(project, closureOf<Project> {
  if (version.toString().endsWith("-SNAPSHOT")) {
    // Configure deployment of snapshot versions to Sonatype OSS
    tasks.getByName("bintrayUpload").enabled = false

    publishing().apply {
      repositories {
        maven {
          url = uri("https://oss.sonatype.org/content/repositories/snapshots")
          name = "snapshot"
          credentials {
            username = extra["deployment.sonatypeUser"] as String
            password = extra["deployment.sonatypePass"] as String
          }
        }
      }
    }
  } else {
    // Configure deployment of release versions to Bintray
    artifacts {
      add("archives", javadocJarTask)
      add("archives", sourcesJarTask)
    }

    extensions.getByName("bintray").withGroovyBuilder {
      setProperty("user", extra["deployment.bintrayUser"])
      setProperty("key", extra["deployment.bintrayKey"])
      setProperty("configurations", setOf("archives"))
      setProperty("dryRun", false)
      setProperty("publish", true)

      getProperty("pkg").withGroovyBuilder {
        setProperty("repo", "maven")
        setProperty("name", deployConfig.artifactId)
        setProperty("desc", deployConfig.description)
        setProperty("licenses", setOf(Artifacts.license))
        setProperty("githubRepo", Artifacts.githubRepo)
        setProperty("websiteUrl", Artifacts.githubUrl)
        setProperty("vcsUrl", "${Artifacts.githubUrl}.git")
        setProperty("issueTrackerUrl", "${Artifacts.githubUrl}/issues")
        setProperty("publicDownloadNumbers", true)

        getProperty("version").withGroovyBuilder {
          setProperty("name", deployConfig.currentVersion)
          setProperty("desc", deployConfig.description)
        }
      }
    }
  }
})
