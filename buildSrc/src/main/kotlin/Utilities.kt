import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.compile.AbstractCompile

/* RepositoryHandler */

fun RepositoryHandler.jitpack() =
    this.maven(object: Action<MavenArtifactRepository> {
      override fun execute(repo: MavenArtifactRepository) {
        repo.setUrl("https://jitpack.io")
      }
    })

fun RepositoryHandler.sonatypeSnapshots() =
    this.maven(object: Action<MavenArtifactRepository> {
      override fun execute(repo: MavenArtifactRepository) {
        repo.setUrl("https://oss.sonatype.org/content/repositories/snapshots")
      }
    })

/* Project */

fun Project.fixCompileTaskChain() {
  setupCompileChain(
      sourceCompileName = "compileKotlin",
      targetCompileName = "compileGroovy",
      javaCompileName = "compileJava",
      classesTaskName = "classes")

  setupCompileChain(
      sourceCompileName = "compileTestKotlin",
      targetCompileName = "compileTestGroovy",
      javaCompileName = "compileTestJava",
      classesTaskName = "testClasses")
}

/**
 * @param sourceCompileName The sources in this task may call into the target
 * @param targetCompileName The sources in this task must not call into the source
 * @param javaCompileName Name of the neutral Java task
 * @param classesTaskName Name of the neutral classes task
 */
private fun Project.setupCompileChain(sourceCompileName: String,
                                      targetCompileName: String,
                                      javaCompileName: String,
                                      classesTaskName: String) {
  val targetCompile = tasks.getByName(targetCompileName) as AbstractCompile
  val sourceCompile = tasks.getByName(sourceCompileName) as AbstractCompile
  val classesTask = tasks.getByName(classesTaskName)

  // Allow calling the source language's classes from the target language.
  // In this case, we allow calling Kotlin from Groovy - it has to be noted however,
  // that the other way does not work!
  targetCompile.classpath += project.files(sourceCompile.destinationDir)
}
