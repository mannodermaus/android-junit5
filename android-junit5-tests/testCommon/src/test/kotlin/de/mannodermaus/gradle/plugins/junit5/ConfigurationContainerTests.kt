package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.ConfigurationKind.ANDROID_TEST
import de.mannodermaus.gradle.plugins.junit5.ConfigurationKind.TEST
import de.mannodermaus.gradle.plugins.junit5.ConfigurationScope.API
import de.mannodermaus.gradle.plugins.junit5.ConfigurationScope.COMPILE_ONLY
import de.mannodermaus.gradle.plugins.junit5.ConfigurationScope.IMPLEMENTATION
import de.mannodermaus.gradle.plugins.junit5.ConfigurationScope.RUNTIME_ONLY
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Configuration.State
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCollection.AntType
import org.gradle.api.file.FileTree
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskDependency
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File

class ConfigurationContainerTests {

  /* AGP 2 */

  private val comp: Configuration = MockConfiguration("compile")
  private val testComp: Configuration = MockConfiguration("testCompile")
  private val androidTestComp: Configuration = MockConfiguration("androidTestCompile")
  private val prov: Configuration = MockConfiguration("provided")
  private val testProv: Configuration = MockConfiguration("testProvided")
  private val androidTestProv: Configuration = MockConfiguration("androidTestProvided")
  private val apk: Configuration = MockConfiguration("apk")
  private val testApk: Configuration = MockConfiguration("testApk")
  private val androidTestApk: Configuration = MockConfiguration("androidTestApk")

  private val containerAgp2 = setOf(comp, prov, apk,
      testComp, testProv, testApk,
      androidTestComp, androidTestProv, androidTestApk)

  /* AGP 3 */

  private val api: Configuration = MockConfiguration("api")
  private val testApi: Configuration = MockConfiguration("testApi")
  private val androidTestApi: Configuration = MockConfiguration("androidTestApi")
  private val impl: Configuration = MockConfiguration("implementation")
  private val testImpl: Configuration = MockConfiguration("testImplementation")
  private val androidTestImpl: Configuration = MockConfiguration("androidTestImplementation")
  private val compOnly: Configuration = MockConfiguration("compileOnly")
  private val testCompOnly: Configuration = MockConfiguration("testCompileOnly")
  private val androidTestCompOnly: Configuration = MockConfiguration("androidTestCompileOnly")
  private val runtOnly: Configuration = MockConfiguration("runtimeOnly")
  private val testRuntOnly: Configuration = MockConfiguration("testRuntimeOnly")
  private val androidTestRuntOnly: Configuration = MockConfiguration("androidTestRuntimeOnly")

  private val containerAgp3 = setOf(api, impl, compOnly, runtOnly,
      testApi, testImpl, testCompOnly, testRuntOnly,
      androidTestApi, androidTestImpl, androidTestCompOnly, androidTestRuntOnly)

  /* AGP 2 */

  @Test
  fun agp2_noVariant_defaultKind_apiScope() {
    assertThat(containerAgp2.findConfiguration(scope = API), equalTo(comp))
  }

  @Test
  fun agp2_noVariant_defaultKind_implementationScope() {
    assertThat(containerAgp2.findConfiguration(scope = IMPLEMENTATION), equalTo(comp))
  }

  @Test
  fun agp2_noVariant_defaultKind_compileOnlyScope() {
    assertThat(containerAgp2.findConfiguration(scope = COMPILE_ONLY), equalTo(prov))
  }

  @Test
  fun agp2_noVariant_defaultKind_runtimeOnlyScope() {
    assertThat(containerAgp2.findConfiguration(scope = RUNTIME_ONLY), equalTo(apk))
  }

  @Test
  fun agp2_noVariant_testKind_apiScope() {
    assertThat(containerAgp2.findConfiguration(kind = TEST, scope = API), equalTo(testComp))
  }

  @Test
  fun agp2_noVariant_testKind_implementationScope() {
    assertThat(containerAgp2.findConfiguration(kind = TEST, scope = IMPLEMENTATION),
        equalTo(testComp))
  }

  @Test
  fun agp2_noVariant_testKind_compileOnlyScope() {
    assertThat(containerAgp2.findConfiguration(kind = TEST, scope = COMPILE_ONLY), equalTo(
        testProv))
  }

  @Test
  fun agp2_noVariant_testKind_runtimeOnlyScope() {
    assertThat(containerAgp2.findConfiguration(kind = TEST, scope = RUNTIME_ONLY), equalTo(
        testApk))
  }

  @Test
  fun agp2_noVariant_androidTestKind_apiScope() {
    assertThat(containerAgp2.findConfiguration(kind = ANDROID_TEST, scope = API),
        equalTo(androidTestComp))
  }

  @Test
  fun agp2_noVariant_androidTestKind_implementationScope() {
    assertThat(containerAgp2.findConfiguration(kind = ANDROID_TEST, scope = IMPLEMENTATION),
        equalTo(androidTestComp))
  }

  @Test
  fun agp2_noVariant_androidTestKind_compileOnlyScope() {
    assertThat(containerAgp2.findConfiguration(kind = ANDROID_TEST, scope = COMPILE_ONLY), equalTo(
        androidTestProv))
  }

  @Test
  fun agp2_noVariant_androidTestKind_runtimeOnlyScope() {
    assertThat(containerAgp2.findConfiguration(kind = ANDROID_TEST, scope = RUNTIME_ONLY), equalTo(
        androidTestApk))
  }

  /* AGP 3 */

  @Test
  fun agp3_noVariant_defaultKind_apiScope() {
    assertThat(containerAgp3.findConfiguration(scope = API), equalTo(api))
  }

  @Test
  fun agp3_noVariant_defaultKind_implementationScope() {
    assertThat(containerAgp3.findConfiguration(scope = IMPLEMENTATION), equalTo(impl))
  }

  @Test
  fun agp3_noVariant_defaultKind_compileOnlyScope() {
    assertThat(containerAgp3.findConfiguration(scope = COMPILE_ONLY), equalTo(compOnly))
  }

  @Test
  fun agp3_noVariant_defaultKind_runtimeOnlyScope() {
    assertThat(containerAgp3.findConfiguration(scope = RUNTIME_ONLY), equalTo(runtOnly))
  }

  @Test
  fun agp3_noVariant_testKind_apiScope() {
    assertThat(containerAgp3.findConfiguration(kind = TEST, scope = API), equalTo(testApi))
  }

  @Test
  fun agp3_noVariant_testKind_implementationScope() {
    assertThat(containerAgp3.findConfiguration(kind = TEST, scope = IMPLEMENTATION),
        equalTo(testImpl))
  }

  @Test
  fun agp3_noVariant_testKind_compileOnlyScope() {
    assertThat(containerAgp3.findConfiguration(kind = TEST, scope = COMPILE_ONLY), equalTo(
        testCompOnly))
  }

  @Test
  fun agp3_noVariant_testKind_runtimeOnlyScope() {
    assertThat(containerAgp3.findConfiguration(kind = TEST, scope = RUNTIME_ONLY), equalTo(
        testRuntOnly))
  }

  @Test
  fun agp3_noVariant_androidTestKind_apiScope() {
    assertThat(containerAgp3.findConfiguration(kind = ANDROID_TEST, scope = API),
        equalTo(androidTestApi))
  }

  @Test
  fun agp3_noVariant_androidTestKind_implementationScope() {
    assertThat(containerAgp3.findConfiguration(kind = ANDROID_TEST, scope = IMPLEMENTATION),
        equalTo(androidTestImpl))
  }

  @Test
  fun agp3_noVariant_androidTestKind_compileOnlyScope() {
    assertThat(containerAgp3.findConfiguration(kind = ANDROID_TEST, scope = COMPILE_ONLY), equalTo(
        androidTestCompOnly))
  }

  @Test
  fun agp3_noVariant_androidTestKind_runtimeOnlyScope() {
    assertThat(containerAgp3.findConfiguration(kind = ANDROID_TEST, scope = RUNTIME_ONLY), equalTo(
        androidTestRuntOnly))
  }
}

private class MockConfiguration(private val baseName: String) : Configuration {

  override fun getName() = baseName

  override fun getUploadTaskName(): String {
    TODO("not implemented")
  }

  override fun getIncoming(): ResolvableDependencies {
    TODO("not implemented")
  }

  override fun getAll(): MutableSet<Configuration> {
    TODO("not implemented")
  }

  override fun copy(): Configuration {
    TODO("not implemented")
  }

  override fun copy(p0: Spec<in Dependency>?): Configuration {
    TODO("not implemented")
  }

  override fun copy(p0: Closure<*>?): Configuration {
    TODO("not implemented")
  }

  override fun getAttributes(): AttributeContainer {
    TODO("not implemented")
  }

  override fun attributes(p0: Action<in AttributeContainer>?): Configuration {
    TODO("not implemented")
  }

  override fun getResolutionStrategy(): ResolutionStrategy {
    TODO("not implemented")
  }

  override fun resolutionStrategy(p0: Closure<*>?): Configuration {
    TODO("not implemented")
  }

  override fun resolutionStrategy(p0: Action<in ResolutionStrategy>?): Configuration {
    TODO("not implemented")
  }

  override fun getAllArtifacts(): PublishArtifactSet {
    TODO("not implemented")
  }

  override fun getDescription(): String? {
    TODO("not implemented")
  }

  override fun filter(p0: Closure<*>?): FileCollection {
    TODO("not implemented")
  }

  override fun filter(p0: Spec<in File>?): FileCollection {
    TODO("not implemented")
  }

  override fun withDependencies(p0: Action<in DependencySet>?): Configuration {
    TODO("not implemented")
  }

  override fun getTaskDependencyFromProjectDependency(p0: Boolean, p1: String?): TaskDependency {
    TODO("not implemented")
  }

  override fun copyRecursive(): Configuration {
    TODO("not implemented")
  }

  override fun copyRecursive(p0: Spec<in Dependency>?): Configuration {
    TODO("not implemented")
  }

  override fun copyRecursive(p0: Closure<*>?): Configuration {
    TODO("not implemented")
  }

  override fun asType(p0: Class<*>?): Any {
    TODO("not implemented")
  }

  override fun isEmpty(): Boolean {
    TODO("not implemented")
  }

  override fun stopExecutionIfEmpty(): FileCollection {
    TODO("not implemented")
  }

  override fun defaultDependencies(p0: Action<in DependencySet>?): Configuration {
    TODO("not implemented")
  }

  override fun fileCollection(p0: Spec<in Dependency>?): FileCollection {
    TODO("not implemented")
  }

  override fun fileCollection(p0: Closure<*>?): FileCollection {
    TODO("not implemented")
  }

  override fun fileCollection(vararg p0: Dependency?): FileCollection {
    TODO("not implemented")
  }

  override fun getArtifacts(): PublishArtifactSet {
    TODO("not implemented")
  }

  override fun setVisible(p0: Boolean): Configuration {
    TODO("not implemented")
  }

  override fun setDescription(p0: String?): Configuration {
    TODO("not implemented")
  }

  override fun setCanBeConsumed(p0: Boolean) {
    TODO("not implemented")
  }

  override fun resolve(): MutableSet<File> {
    TODO("not implemented")
  }

  override fun setExtendsFrom(p0: MutableIterable<Configuration>?): Configuration {
    TODO("not implemented")
  }

  override fun getOutgoing(): ConfigurationPublications {
    TODO("not implemented")
  }

  override fun isVisible(): Boolean {
    TODO("not implemented")
  }

  override fun getResolvedConfiguration(): ResolvedConfiguration {
    TODO("not implemented")
  }

  override fun exclude(p0: MutableMap<String, String>?): Configuration {
    TODO("not implemented")
  }

  override fun getFiles(): MutableSet<File> {
    TODO("not implemented")
  }

  override fun contains(p0: File?): Boolean {
    TODO("not implemented")
  }

  override fun isCanBeResolved(): Boolean {
    TODO("not implemented")
  }

  override fun getBuildDependencies(): TaskDependency {
    TODO("not implemented")
  }

  override fun getExcludeRules(): MutableSet<ExcludeRule> {
    TODO("not implemented")
  }

  override fun add(p0: FileCollection?): FileCollection {
    TODO("not implemented")
  }

  override fun iterator(): MutableIterator<File> {
    TODO("not implemented")
  }

  override fun isTransitive(): Boolean {
    TODO("not implemented")
  }

  override fun setTransitive(p0: Boolean): Configuration {
    TODO("not implemented")
  }

  override fun isCanBeConsumed(): Boolean {
    TODO("not implemented")
  }

  override fun getSingleFile(): File {
    TODO("not implemented")
  }

  override fun getState(): State {
    TODO("not implemented")
  }

  override fun getHierarchy(): MutableSet<Configuration> {
    TODO("not implemented")
  }

  override fun files(p0: Closure<*>?): MutableSet<File> {
    TODO("not implemented")
  }

  override fun files(p0: Spec<in Dependency>?): MutableSet<File> {
    TODO("not implemented")
  }

  override fun files(vararg p0: Dependency?): MutableSet<File> {
    TODO("not implemented")
  }

  override fun getAsFileTree(): FileTree {
    TODO("not implemented")
  }

  override fun addToAntBuilder(p0: Any?, p1: String?, p2: AntType?) {
    TODO("not implemented")
  }

  override fun addToAntBuilder(p0: Any?, p1: String?): Any {
    TODO("not implemented")
  }

  override fun setCanBeResolved(p0: Boolean) {
    TODO("not implemented")
  }

  override fun minus(p0: FileCollection?): FileCollection {
    TODO("not implemented")
  }

  override fun outgoing(p0: Action<in ConfigurationPublications>?) {
    TODO("not implemented")
  }

  override fun getAsPath(): String {
    TODO("not implemented")
  }

  override fun getExtendsFrom(): MutableSet<Configuration> {
    TODO("not implemented")
  }

  override fun extendsFrom(vararg p0: Configuration?): Configuration {
    TODO("not implemented")
  }

  override fun getDependencies(): DependencySet {
    TODO("not implemented")
  }

  override fun plus(p0: FileCollection?): FileCollection {
    TODO("not implemented")
  }

  override fun getAllDependencies(): DependencySet {
    TODO("not implemented")
  }

}
