plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "jacoco")

    // Apply detekt only after the Kotlin plugin is present
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        apply(plugin = "dev.detekt")

        configure<dev.detekt.gradle.extensions.DetektExtension> {
            config.setFrom(rootProject.files("detekt.yml"))
            buildUponDefaultConfig = true
            val baselineFile = file("detekt-baseline.xml")
            if (baselineFile.exists()) {
                baseline = baselineFile
            }
        }
    }

    // ── ktlint ───────────────────────────────────────────────────────────────
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
    }

    // ── jacoco ───────────────────────────────────────────────────────────────
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.withType<Test> {
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = "0.60".toBigDecimal()
                }
            }
        }
    }

    // ── Enganchar todo a check ───────────────────────────────────────────────
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(
            tasks.withType<JacocoCoverageVerification>()
        )
    }
}
