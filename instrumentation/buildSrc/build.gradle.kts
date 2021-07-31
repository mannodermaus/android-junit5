plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir(file("../../build-logic/src/main/kotlin"))
    }
}

gradlePlugin {
    plugins {
        register("explicit-api-mode") {
            id = "explicit-api-mode"
            implementationClass = "ExplicitApiModePlugin"
        }
    }
}
