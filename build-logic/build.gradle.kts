plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.maven.publish.gradle.plugin)
    // expose the typed version catalog accessor (`libs`) to precompiled script plugins
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    // build-logic is a separate included build, so the constraints in the root build.gradle.kts
    // `buildscript {}` block do not reach its classpath. Mend HIGH/CRITICAL findings on AGP's
    // transitive build-tooling dependencies are addressed here. Build classpath only; none of
    // these are packaged in a release artifact.
    constraints {
        listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on").forEach {
            implementation("org.bouncycastle:$it:1.86") {
                because("CVE-2025-14813, CVE-2026-5598, CVE-2026-71888, CVE-2026-71889 and others: Bouncy Castle < 1.86")
            }
        }
        listOf("jackson-core", "jackson-databind").forEach {
            implementation("com.fasterxml.jackson.core:$it:2.22.4") {
                because("CVE-2026-110879 (jackson-databind CRITICAL) and CVE-2026-89425 (jackson-core)")
            }
        }
        implementation("com.fasterxml.jackson.core:jackson-annotations:2.22") {
            because("Must match jackson-databind 2.22.x")
        }
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.4") {
            because("Must match jackson-databind 2.22.x")
        }
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.22.4") {
            because("Must match jackson-databind 2.22.x")
        }
        implementation("org.jsoup:jsoup:1.23.2") {
            because("CVE-2026-75140: jsoup < 1.23.2")
        }
        implementation("com.google.guava:guava:33.7.2-jre") {
            because("CVE-2026-102554: guava < 33.7.2")
        }
    }
}
