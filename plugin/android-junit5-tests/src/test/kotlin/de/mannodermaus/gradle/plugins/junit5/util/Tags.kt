package de.mannodermaus.gradle.plugins.junit5.util

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Created by Marcel Schnelle on 2018/06/20.
 */
@Retention
@Target(CLASS, FUNCTION)
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
annotation class OnlyOnLocalMachine
