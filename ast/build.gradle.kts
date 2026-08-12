plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

// lo copie del app build.gradle.kts
dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
