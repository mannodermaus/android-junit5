package de.mannodermaus.gradle.plugins.junit5.dsl

import de.mannodermaus.gradle.plugins.junit5.internal.utils.IncludeExcludeContainer

public abstract class FiltersExtension {

    operator fun invoke(config: FiltersExtension.() -> Unit) {
        this.config()
    }

    /**
     * Class name patterns in the form of regular expressions for
     * classes that should be <em>included</em> in the test plan.
     *
     * <p>The patterns are combined using OR semantics, i.e. if the fully
     * qualified name of a class matches against at least one of the patterns,
     * the class will be included in the test plan.
     */
    internal val patterns = IncludeExcludeContainer()

    /**
     * Add a pattern to the list of <em>included</em> patterns
     */
    fun includePattern(pattern: String) {
        includePatterns(pattern)
    }

    /**
     * Add patterns to the list of <em>included</em> patterns
     */
    fun includePatterns(vararg patterns: String) {
        this.patterns.include(*patterns)
    }

    /**
     * Add a pattern to the list of <em>excluded</em> patterns
     */
    fun excludePattern(pattern: String) {
        excludePatterns(pattern)
    }

    /**
     * Add patterns to the list of <em>excluded</em> patterns
     */
    fun excludePatterns(vararg patterns: String) {
        this.patterns.exclude(*patterns)
    }

    /**
     * Included & Excluded JUnit 5 tags.
     */
    internal val tags = IncludeExcludeContainer()

    /**
     * Add tags to the list of <em>included</em> tags
     */
    fun includeTags(vararg tags: String) {
        this.tags.include(*tags)
    }

    /**
     * Add tags to the list of <em>excluded</em> tags
     */
    fun excludeTags(vararg tags: String) {
        this.tags.exclude(*tags)
    }

    /**
     * Included & Excluded JUnit 5 engines.
     */
    internal val engines = IncludeExcludeContainer()

    /**
     * Add engines to the list of <em>included</em> engines
     */
    fun includeEngines(vararg engines: String) {
        this.engines.include(*engines)
    }

    /**
     * Add engines to the list of <em>excluded</em> engines
     */
    fun excludeEngines(vararg engines: String) {
        this.engines.exclude(*engines)
    }
}
