plugins {
    jacoco
}

subprojects {
    apply(plugin = "jacoco")

    tasks.withType<Test>().configureEach {
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.withType<Test>())
    }

    tasks.withType<JacocoCoverageVerification>().configureEach {
        violationRules {
            rule {
                limit { minimum = "0.80".toBigDecimal() }
            }
        }
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(tasks.withType<JacocoCoverageVerification>())
    }
}
