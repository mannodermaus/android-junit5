package de.mannodermaus.gradle.plugins.junit5.dsl

import de.mannodermaus.gradle.plugins.junit5.internal.utils.IncludeExcludeContainer

public abstract class FiltersExtension {

    /**
     * Class name patterns in the form of regular expressions for classes that should be
     * <em>included</em> in the test plan.
     *
     * <p>The patterns are combined using OR semantics, i.e. if the fully qualified name of a class
     * matches against at least one of the patterns, the class will be included in the test plan.
     */
    internal val patterns = IncludeExcludeContainer()

    /** Add a pattern to the list of <em>included</em> patterns */
    public fun includePattern(pattern: String) {
        includePatterns(pattern)
    }

    /** Add patterns to the list of <em>included</em> patterns */
    public fun includePatterns(vararg patterns: String) {
        this.patterns.include(*patterns)
    }

    /** Add a pattern to the list of <em>excluded</em> patterns */
    public fun excludePattern(pattern: String) {
        excludePatterns(pattern)
    }

    /** Add patterns to the list of <em>excluded</em> patterns */
    public fun excludePatterns(vararg patterns: String) {
        this.patterns.exclude(*patterns)
    }

    /** Included & Excluded JUnit 5 tags. */
    internal val tags = IncludeExcludeContainer()

    /** Add tags to the list of <em>included</em> tags */
    public fun includeTags(vararg tags: String) {
        this.tags.include(*tags)
    }

    /** Add tags to the list of <em>excluded</em> tags */
    public fun excludeTags(vararg tags: String) {
        this.tags.exclude(*tags)
    }

    /** Included & Excluded JUnit 5 engines. */
    internal val engines = IncludeExcludeContainer()

    /** Add engines to the list of <em>included</em> engines */
    public fun includeEngines(vararg engines: String) {
        this.engines.include(*engines)
    }

    /** Add engines to the list of <em>excluded</em> engines */
    public fun excludeEngines(vararg engines: String) {
        this.engines.exclude(*engines)
    }
}
