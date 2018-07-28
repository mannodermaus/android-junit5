package de.mannodermaus.gradle.plugins.junit5.util

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Created by Marcel Schnelle on 2018/06/20.
 * Copyright © 2018 TenTen Technologies Limited. All rights reserved.
 */
@Retention
@Target(CLASS, FUNCTION)
@EnabledIfEnvironmentVariable(named = "CI", matches = "false")
annotation class OnlyOnLocalMachine
