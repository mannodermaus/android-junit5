dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../build-logic/gradle/libs.versions.toml"))
        }
    }
}
