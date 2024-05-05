package de.mannodermaus.junit5.internal.runners

import org.junit.platform.launcher.LauncherDiscoveryRequest

/**
 * A special kind of [LauncherDiscoveryRequest] that can also report
 * if its discovery is made in the context of an Android "isolated method run"
 * (i.e. when only running a single test inside the Android instrumentation).
 */
internal class AndroidLauncherDiscoveryRequest(
    delegate: LauncherDiscoveryRequest,
    val isIsolatedMethodRun: Boolean,
) : LauncherDiscoveryRequest by delegate
