plugins {
  alias(libs.plugins.kotlin.jvm)
}

repositories {
  mavenCentral()
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

// lo copie del app build.gradle.kts
dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")

  testImplementation(libs.junit.jupiter.engine)

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  implementation(libs.guava)

  // le agrego la dependencia de common
  implementation(project(":common"))
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
