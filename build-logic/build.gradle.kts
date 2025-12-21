plugins {
    `kotlin-dsl`
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())

    testImplementation(libs.junit.vintage.api)
}
