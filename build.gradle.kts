repositories {
    mavenCentral()
}

allprojects {
    group = "com.github.TP1-2-INGSIS"
    version = resolveVersion()
}

// The version is resolved for Maven packages and distributions:
// 1. Gradle property -PreleaseVersion=X.Y.Z
// 2. Environment variable RELEASE_VERSION=X.Y.Z or VERSION=X.Y.Z
// 3. Git tag from GITHUB_REF_NAME (vX.Y.Z)
// 4. Fallback: 0.0.0-SNAPSHOT
fun resolveVersion(): String {
    val propVersion = findProperty("releaseVersion") as? String
    if (!propVersion.isNullOrBlank()) {
        return propVersion.removePrefix("v")
    }

    val envVersion = System.getenv("RELEASE_VERSION") ?: System.getenv("VERSION")
    if (!envVersion.isNullOrBlank()) {
        return envVersion.removePrefix("v")
    }

    val ref = System.getenv("GITHUB_REF_NAME") ?: "local"
    return if (ref.matches(Regex("v\\d+\\.\\d+\\.\\d+.*"))) {
        ref.removePrefix("v")
    } else {
        "0.0.0-SNAPSHOT"
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
