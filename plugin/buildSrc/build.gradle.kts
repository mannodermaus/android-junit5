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
