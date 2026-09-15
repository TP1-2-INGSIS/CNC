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
    version = "1.1.0"
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
