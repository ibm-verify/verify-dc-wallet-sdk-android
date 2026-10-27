/*
 * Copyright (c) 2024-2025 European Commission
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

pluginManagement {
    val artifactoryUser: String = System.getenv("ARTIFACTORY_USER") ?: ""
    val artifactoryPassword: String = System.getenv("ARTIFACTORY_PASSWORD") ?: ""
    val hasArtifactoryCredentials = artifactoryUser.isNotEmpty() && artifactoryPassword.isNotEmpty()

    includeBuild("build-logic")
    repositories {
        if (hasArtifactoryCredentials) {
            maven {
                url = uri("https://na.artifactory.swg-devops.com/artifactory/ibm-verify-mobile-team-maven-virtual")
                credentials {
                    username = artifactoryUser
                    password = artifactoryPassword
                }
            }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    val artifactoryUser: String = System.getenv("ARTIFACTORY_USER") ?: ""
    val artifactoryPassword: String = System.getenv("ARTIFACTORY_PASSWORD") ?: ""
    val hasArtifactoryCredentials = artifactoryUser.isNotEmpty() && artifactoryPassword.isNotEmpty()

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (hasArtifactoryCredentials) {
            maven {
                url = uri("https://na.artifactory.swg-devops.com/artifactory/ibm-verify-mobile-team-maven-virtual")
                credentials {
                    username = artifactoryUser
                    password = artifactoryPassword
                }
            }
        }
        google()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            mavenContent { snapshotsOnly() }
        }
    }
}

rootProject.name = "eudi-lib-android-wallet-core"
include(":wallet-core")
include(":document-manager")
include(":transfer-manager")
