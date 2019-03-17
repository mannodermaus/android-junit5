object Android {
  const val buildToolsVersion = "28.0.3"
  const val compileSdkVersion = "android-28"
  const val javaMaxHeapSize = "3g"

  const val targetSdkVersion = 28
  const val sampleMinSdkVersion = 14
  val testRunnerMinSdkVersion = (Artifacts.Instrumentation.Runner.platform as Platform.Android).minSdk
  val testCoreMinSdkVersion = (Artifacts.Instrumentation.Core.platform as Platform.Android).minSdk
  val instrumentationMinSdkVersion = (Artifacts.Instrumentation.Library.platform as Platform.Android).minSdk
}
