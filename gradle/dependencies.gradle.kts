// Common properties
val apacheCommonsVersion = "2.6"
val junitPlatformVersion = "1.3.1"
val junitJupiterVersion = "5.3.1"
val junitVintageVersion = "5.3.1"
val kotlinVersion = "1.2.61"
val spekVersion = "1.2.1"

// Android Environment
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// A Note to my forgetful self:
//
// When updating these values, make sure
// to always update the CI config, too
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
extra["android.buildToolsVersion"] = "28.0.3"
extra["android.compileSdkVersion"] = "android-28"
extra["android.javaMaxHeapSize"] = "3g"
extra["android.targetSdkVersion"] = 28

// Plugins
extra["plugins.android"] = "com.android.tools.build:gradle:3.2.1"
extra["plugins.androidMaven"] = "digital.wup:android-maven-publish:3.6.2"
extra["plugins.bintray"] = "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4"
extra["plugins.dcendentsMaven"] = "com.github.dcendents:android-maven-gradle-plugin:2.1"
extra["plugins.versions"] = "com.github.ben-manes:gradle-versions-plugin:0.20.0"

// Dependencies
extra["libs.annimonStream"] = "com.annimon:stream:1.2.1"
extra["libs.commonsIO"] = "commons-io:commons-io:$apacheCommonsVersion"
extra["libs.commonsLang"] = "commons-lang:commons-lang:$apacheCommonsVersion"
extra["libs.javaSemver"] = "com.github.zafarkhaja:java-semver:0.9.0"

extra["libs.androidTestRunner"] = "com.android.support.test:runner:1.0.2"
extra["libs.assertjAndroid"] = "com.squareup.assertj:assertj-android:1.2.0"
extra["libs.assertjCore"] = "org.assertj:assertj-core:3.11.1"
extra["libs.espresso"] = "com.android.support.test.espresso:espresso-core:3.0.1"
extra["libs.junit4"] = "junit:junit:4.12"
extra["libs.junitJupiterApi"] = "org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion"
extra["libs.junitJupiterEngine"] = "org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion"
extra["libs.junitJupiterParams"] = "org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion"
extra["libs.junitPioneer"] = "org.junit-pioneer:junit-pioneer:0.2.2"
extra["libs.junitPlatformCommons"] = "org.junit.platform:junit-platform-commons:$junitPlatformVersion"
extra["libs.junitPlatformEngine"] = "org.junit.platform:junit-platform-engine:$junitPlatformVersion"
extra["libs.junitPlatformLauncher"] = "org.junit.platform:junit-platform-launcher:$junitPlatformVersion"
extra["libs.junitPlatformRunner"] = "org.junit.platform:junit-platform-runner:$junitPlatformVersion"
extra["libs.junitVintageEngine"] = "org.junit.vintage:junit-vintage-engine:$junitVintageVersion"
extra["libs.mockito"] = "org.mockito:mockito-core:2.19.0"
extra["libs.spekApi"] = "org.jetbrains.spek:spek-api:$spekVersion"
extra["libs.spekEngine"] = "org.jetbrains.spek:spek-engine:$spekVersion"

// TODO Needed?
// extra["libs.junitPlatformConsole"] = "org.junit.platform:junit-platform-console:$junitPlatformVersion"

// Versions
extra["versions.kotlin"] = kotlinVersion
