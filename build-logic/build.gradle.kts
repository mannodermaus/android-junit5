plugins {
  `kotlin-dsl`
  java
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  testImplementation("junit:junit:+")
}
