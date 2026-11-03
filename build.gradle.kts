/*
 * Copyright (c) 2024 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jk1.license.filter.ExcludeTransitiveDependenciesFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ReduceDuplicateLicensesFilter
import com.github.jk1.license.render.InventoryMarkdownReportRenderer

// Mend HIGH/CRITICAL findings on build-tooling transitives (AGP lint / sdk-common and the Dokka
// plugin). Build classpath only; none of these are packaged in a release artifact.
buildscript {
    dependencies {
        constraints {
            listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on").forEach {
                classpath("org.bouncycastle:$it:1.86") {
                    because("CVE-2025-14813, CVE-2026-5598, CVE-2026-71888, CVE-2026-71889 and others: Bouncy Castle < 1.86")
                }
            }
            listOf("jackson-core", "jackson-databind").forEach {
                classpath("com.fasterxml.jackson.core:$it:2.22.4") {
                    because("CVE-2026-110879 (jackson-databind CRITICAL) and CVE-2026-89425 (jackson-core)")
                }
            }
            classpath("com.fasterxml.jackson.core:jackson-annotations:2.22") {
                because("Must match jackson-databind 2.22.x")
            }
            classpath("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.4") {
                because("Must match jackson-databind 2.22.x")
            }
            classpath("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.22.4") {
                because("Must match jackson-databind 2.22.x")
            }
            classpath("org.jsoup:jsoup:1.23.2") {
                because("CVE-2026-75140: jsoup < 1.23.2")
            }
            classpath("com.google.guava:guava:33.7.2-jre") {
                because("CVE-2026-102554: guava < 33.7.2")
            }
        }
    }
}

plugins {
    alias(libs.plugins.dependency.license.report)
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.dependencycheck) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
}

// Mend HIGH/CRITICAL findings on the Android Lint and Unified Test Platform (UTP) tooling
// configurations of every module, and on the Dokka generator classpaths. These pull in old Netty
// (grpc-netty), Guava, Jackson, jsoup, FreeMarker and Bouncy Castle. Scoped to those tooling configurations so
// the library's own runtime dependency graph (and the published POMs) is unchanged.
allprojects {
    configurations.configureEach {
        val isTooling = name.startsWith("unified-test-platform") ||
            name.contains("lint", ignoreCase = true) ||
            name.contains("dokka", ignoreCase = true)
        if (isTooling) {
            resolutionStrategy {
                force("io.netty:netty-codec-http2:4.2.20.Final")
                force("io.netty:netty-codec-compression:4.2.20.Final")
                force("io.netty:netty-handler-proxy:4.2.20.Final")
                force("com.google.guava:guava:33.7.2-jre")
                force("org.bouncycastle:bcprov-jdk18on:1.86")
                force("org.bouncycastle:bcpkix-jdk18on:1.86")
                force("org.bouncycastle:bcutil-jdk18on:1.86")
                force("com.fasterxml.jackson.core:jackson-core:2.22.4")
                force("com.fasterxml.jackson.core:jackson-databind:2.22.4")
                force("org.jsoup:jsoup:1.23.2")
                force("org.freemarker:freemarker:2.3.35")
            }
        }
    }
}

licenseReport {
    projects = project.subprojects.toTypedArray()
    unionParentPomLicenses = false
    excludeBoms = true
    excludeOwnGroup = true
    filters = arrayOf(
        LicenseBundleNormalizer(),
        ReduceDuplicateLicensesFilter(),
        ExcludeTransitiveDependenciesFilter()
    )
    configurations = arrayOf("releaseRuntimeClasspath")
    renderers = arrayOf(InventoryMarkdownReportRenderer("licenses.md", "EUDI Wallet Libraries"))
}

tasks.generateLicenseReport.configure {
    doLast {
        copy {
            from(layout.buildDirectory.file("reports/dependency-license/licenses.md"))
            into(rootDir)
        }
    }
}
