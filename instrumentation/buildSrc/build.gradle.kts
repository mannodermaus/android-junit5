plugins {
  `kotlin-dsl`
}

repositories {
  jcenter()
}

sourceSets {
  main {
    java.srcDir(file("../../buildSrc/src/main/kotlin"))
  }
}
