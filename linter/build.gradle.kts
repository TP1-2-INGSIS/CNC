plugins {
    alias(libs.plugins.kotlin.jvm)
    id("jacoco-conventions")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
    implementation(libs.gson)
    implementation(project(":ast"))
    implementation(project(":common"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
