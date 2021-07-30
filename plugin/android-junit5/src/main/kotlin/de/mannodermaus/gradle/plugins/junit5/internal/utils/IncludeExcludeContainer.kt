package de.mannodermaus.gradle.plugins.junit5.internal.utils

internal class IncludeExcludeContainer {

    private val _include = mutableSetOf<String>()
    private val _exclude = mutableSetOf<String>()

    val include
        get() = _include.toSet()

    fun include(vararg items: String) = this.apply {
        this._include.addAll(items)
        this._exclude.removeAll(items)
    }

    val exclude
        get() = _exclude.toSet()

    fun exclude(vararg items: String) = this.apply {
        this._exclude.addAll(items)
        this._include.removeAll(items)
    }

    fun isEmpty() =
            _include.isEmpty() && _exclude.isEmpty()

    operator fun plus(other: IncludeExcludeContainer): IncludeExcludeContainer {
        // Fast path, where nothing needs to be merged
        if (this.isEmpty()) return other
        if (other.isEmpty()) return this

        // Slow path, where rules need to be merged
        val result = IncludeExcludeContainer()

        result._include.addAll(this.include)
        result._include.addAll(other.include)
        result._include.removeAll(other.exclude)

        result._exclude.addAll(this.exclude)
        result._exclude.addAll(other.exclude)
        result._exclude.removeAll(other.include)

        return result
    }

    override fun toString(): String {
        return "${super.toString()}(include=$_include, exclude=$_exclude)"
    }
}
