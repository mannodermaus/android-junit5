package de.mannodermaus.gradle.plugins.junit5.extensions

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionByName
import org.gradle.api.Project

internal val Project.android
    get() = extensionByName<CommonExtension<*, *, *, *, *>>("android")

internal val Project.androidApp
    get() = extensionByName<ApplicationExtension>("android")
