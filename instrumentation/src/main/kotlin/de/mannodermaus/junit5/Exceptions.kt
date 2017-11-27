package de.mannodermaus.junit5

import android.app.Activity

class UnexpectedActivityException(expected: Class<out Activity>, actual: Class<*>)
  : IllegalArgumentException(
    "@ActivityTest type didn't match the requested parameter. " +
        "Expected '${expected.name}', was '${actual.name}'")

class ActivityAlreadyLaunchedException : IllegalStateException(
    "Activity has already been launched! " +
        "It must be finished by calling finishActivity() " +
        "before launchActivity can be called again.")

class ActivityNotLaunchedException : IllegalStateException(
    "Activity was not launched. If you manually finished it, " +
        "you must launch it again before finishing it.")

class MissingTestedParameterException(activityType: Class<out Activity>) : IllegalStateException(
    "launchActivity=false without a method parameter of type " +
        "${Tested::class.java.simpleName}<${activityType.simpleName}> " +
        "won't ever allow you to lazily launch the Activity. " +
        "Add it to the method signature!")
