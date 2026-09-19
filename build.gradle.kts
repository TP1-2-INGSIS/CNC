plugins {
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

repositories {
    mavenCentral()
}

ktlint {
    debug.set(true)
    outputToConsole.set(true)
}

allprojects {
    group = "com.github.TP1-2-INGSIS"
    version = resolveVersion()
}

// The version at the commit's tag is resolved and used for the mavens package
fun resolveVersion(): String {
    // GITHUB_REF_NAME lo setea GitHub Actions: es el tag (v2.0.0) o la rama (main)
    val ref = System.getenv("GITHUB_REF_NAME") ?: "local"
    return if (ref.matches(Regex("v\\d+\\.\\d+\\.\\d+"))) {
        ref.removePrefix("v")            // v2.0.0 -> 2.0.0 (release)
    } else {
        "0.0.0-SNAPSHOT"                 // cualquier otra cosa -> snapshot
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("gpr") {
                    from(components["java"])
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/TP1-2-INGSIS/CNC")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
