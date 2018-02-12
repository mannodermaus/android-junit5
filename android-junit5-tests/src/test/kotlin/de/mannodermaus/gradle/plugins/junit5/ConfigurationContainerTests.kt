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
import org.junit.jupiter.api.Test
import java.io.File

class ConfigurationContainerTests {

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

  private val container = setOf(api, impl, compOnly, runtOnly,
      testApi, testImpl, testCompOnly, testRuntOnly,
      androidTestApi, androidTestImpl, androidTestCompOnly, androidTestRuntOnly)

  @Test
  fun noVariant_defaultKind_apiScope() {
    assertThat(container.findConfiguration(scope = API), equalTo(api))
  }

  @Test
  fun noVariant_defaultKind_implementationScope() {
    assertThat(container.findConfiguration(scope = IMPLEMENTATION), equalTo(impl))
  }

  @Test
  fun noVariant_defaultKind_compileOnlyScope() {
    assertThat(container.findConfiguration(scope = COMPILE_ONLY), equalTo(compOnly))
  }

  @Test
  fun noVariant_defaultKind_runtimeOnlyScope() {
    assertThat(container.findConfiguration(scope = RUNTIME_ONLY), equalTo(runtOnly))
  }

  @Test
  fun noVariant_testKind_apiScope() {
    assertThat(container.findConfiguration(kind = TEST, scope = API), equalTo(testApi))
  }

  @Test
  fun noVariant_testKind_implementationScope() {
    assertThat(container.findConfiguration(kind = TEST, scope = IMPLEMENTATION),
        equalTo(testImpl))
  }

  @Test
  fun noVariant_testKind_compileOnlyScope() {
    assertThat(container.findConfiguration(kind = TEST, scope = COMPILE_ONLY), equalTo(
        testCompOnly))
  }

  @Test
  fun noVariant_testKind_runtimeOnlyScope() {
    assertThat(container.findConfiguration(kind = TEST, scope = RUNTIME_ONLY), equalTo(
        testRuntOnly))
  }

  @Test
  fun noVariant_androidTestKind_apiScope() {
    assertThat(container.findConfiguration(kind = ANDROID_TEST, scope = API),
        equalTo(androidTestApi))
  }

  @Test
  fun noVariant_androidTestKind_implementationScope() {
    assertThat(container.findConfiguration(kind = ANDROID_TEST, scope = IMPLEMENTATION),
        equalTo(androidTestImpl))
  }

  @Test
  fun noVariant_androidTestKind_compileOnlyScope() {
    assertThat(container.findConfiguration(kind = ANDROID_TEST, scope = COMPILE_ONLY), equalTo(
        androidTestCompOnly))
  }

  @Test
  fun noVariant_androidTestKind_runtimeOnlyScope() {
    assertThat(container.findConfiguration(kind = ANDROID_TEST, scope = RUNTIME_ONLY), equalTo(
        androidTestRuntOnly))
  }
}

private class MockConfiguration(private val baseName: String) : Configuration {

  override fun getName() = baseName

  override fun getUploadTaskName(): String {
    throw RuntimeException("not implemented")
  }

  override fun getIncoming(): ResolvableDependencies {
    throw RuntimeException("not implemented")
  }

  override fun getAll(): MutableSet<Configuration> {
    throw RuntimeException("not implemented")
  }

  override fun copy(): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun copy(p0: Spec<in Dependency>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun copy(p0: Closure<*>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getAttributes(): AttributeContainer {
    throw RuntimeException("not implemented")
  }

  override fun attributes(p0: Action<in AttributeContainer>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getResolutionStrategy(): ResolutionStrategy {
    throw RuntimeException("not implemented")
  }

  override fun resolutionStrategy(p0: Closure<*>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun resolutionStrategy(p0: Action<in ResolutionStrategy>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getAllArtifacts(): PublishArtifactSet {
    throw RuntimeException("not implemented")
  }

  override fun getDescription(): String? {
    throw RuntimeException("not implemented")
  }

  override fun filter(p0: Closure<*>?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun filter(p0: Spec<in File>?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun withDependencies(p0: Action<in DependencySet>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getTaskDependencyFromProjectDependency(p0: Boolean, p1: String?): TaskDependency {
    throw RuntimeException("not implemented")
  }

  override fun copyRecursive(): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun copyRecursive(p0: Spec<in Dependency>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun copyRecursive(p0: Closure<*>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun asType(p0: Class<*>?): Any {
    throw RuntimeException("not implemented")
  }

  override fun isEmpty(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun stopExecutionIfEmpty(): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun defaultDependencies(p0: Action<in DependencySet>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun fileCollection(p0: Spec<in Dependency>?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun fileCollection(p0: Closure<*>?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun fileCollection(vararg p0: Dependency?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun getArtifacts(): PublishArtifactSet {
    throw RuntimeException("not implemented")
  }

  override fun setVisible(p0: Boolean): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun setDescription(p0: String?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun setCanBeConsumed(p0: Boolean) {
    throw RuntimeException("not implemented")
  }

  override fun resolve(): MutableSet<File> {
    throw RuntimeException("not implemented")
  }

  override fun setExtendsFrom(p0: MutableIterable<Configuration>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getOutgoing(): ConfigurationPublications {
    throw RuntimeException("not implemented")
  }

  override fun isVisible(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun getResolvedConfiguration(): ResolvedConfiguration {
    throw RuntimeException("not implemented")
  }

  override fun exclude(p0: MutableMap<String, String>?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getFiles(): MutableSet<File> {
    throw RuntimeException("not implemented")
  }

  override fun contains(p0: File?): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun isCanBeResolved(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun getBuildDependencies(): TaskDependency {
    throw RuntimeException("not implemented")
  }

  override fun getExcludeRules(): MutableSet<ExcludeRule> {
    throw RuntimeException("not implemented")
  }

  override fun add(p0: FileCollection?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun iterator(): MutableIterator<File> {
    throw RuntimeException("not implemented")
  }

  override fun isTransitive(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun setTransitive(p0: Boolean): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun isCanBeConsumed(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun getSingleFile(): File {
    throw RuntimeException("not implemented")
  }

  override fun getState(): State {
    throw RuntimeException("not implemented")
  }

  override fun getHierarchy(): MutableSet<Configuration> {
    throw RuntimeException("not implemented")
  }

  override fun files(p0: Closure<*>?): MutableSet<File> {
    throw RuntimeException("not implemented")
  }

  override fun files(p0: Spec<in Dependency>?): MutableSet<File> {
    throw RuntimeException("not implemented")
  }

  override fun files(vararg p0: Dependency?): MutableSet<File> {
    throw RuntimeException("not implemented")
  }

  override fun getAsFileTree(): FileTree {
    throw RuntimeException("not implemented")
  }

  override fun addToAntBuilder(p0: Any?, p1: String?, p2: AntType?) {
    throw RuntimeException("not implemented")
  }

  override fun addToAntBuilder(p0: Any?, p1: String?): Any {
    throw RuntimeException("not implemented")
  }

  override fun setCanBeResolved(p0: Boolean) {
    throw RuntimeException("not implemented")
  }

  override fun minus(p0: FileCollection?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun outgoing(p0: Action<in ConfigurationPublications>?) {
    throw RuntimeException("not implemented")
  }

  override fun getAsPath(): String {
    throw RuntimeException("not implemented")
  }

  override fun getExtendsFrom(): MutableSet<Configuration> {
    throw RuntimeException("not implemented")
  }

  override fun extendsFrom(vararg p0: Configuration?): Configuration {
    throw RuntimeException("not implemented")
  }

  override fun getDependencies(): DependencySet {
    throw RuntimeException("not implemented")
  }

  override fun plus(p0: FileCollection?): FileCollection {
    throw RuntimeException("not implemented")
  }

  override fun getAllDependencies(): DependencySet {
    throw RuntimeException("not implemented")
  }

}
