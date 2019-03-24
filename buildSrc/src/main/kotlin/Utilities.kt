import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile

fun Project.fixCompileTaskChain() {
  setupCompileChain(
      sourceCompileName = "compileKotlin",
      targetCompileName = "compileGroovy",
      javaCompileName = "compileJava",
      classesTaskName = "classes")

  setupCompileChain(
      sourceCompileName = "compileTestGroovy",
      targetCompileName = "compileTestKotlin",
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

  logger.info("Remove dependency '$targetCompile' -> '$javaCompileName'")
  targetCompile.dependsOn.remove(javaCompileName)

  logger.info("Add dependency '$sourceCompile' -> '$targetCompile' (can call from left into right)")
  sourceCompile.dependsOn.add(targetCompile)

  sourceCompile.classpath += project.files(targetCompile.destinationDir)
  classesTask.dependsOn.add(sourceCompile)
}
