plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(embeddedKotlin("gradle-plugin"))
    implementation("org.jlleitschuh.gradle:ktlint-gradle:14.2.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
}
