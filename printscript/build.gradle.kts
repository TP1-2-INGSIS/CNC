plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
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
    api(project(":common"))
    api(project(":token"))
    api(project(":ast"))
    api(project(":lexer"))
    api(project(":parser"))
    api(project(":semantic"))
    api(project(":interpreter"))
    api(project(":formatter"))
    api(project(":linter"))
    api(project(":app"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
