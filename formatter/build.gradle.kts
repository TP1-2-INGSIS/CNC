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

    // El formatter es un consumidor del AST (hermano del interpreter/semantic).
    // Solo depende de :ast y :common — nunca de :parser ni :interpreter.
    implementation(project(":common"))
    implementation(project(":ast"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
