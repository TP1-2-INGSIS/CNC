plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("jacoco-conventions")
    id("code-quality-conventions")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Las interfaces tienen parámetros por defecto: sin esto Kotlin genera clases
// `DefaultImpls` (compatibilidad con Java) que ningún consumidor Kotlin usa.
kotlin {
    compilerOptions {
        jvmDefault.set(org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY)
    }
}

// Contratos agnósticos del lenguaje: fachadas, perfil y versión.
// Se usa `api` porque las firmas públicas exponen tipos de estos módulos.
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

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
