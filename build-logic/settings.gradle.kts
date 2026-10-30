// Resolves the plugins applied in build-logic/build.gradle.kts (e.g. `kotlin-dsl`) and their
// transitive buildscript classpath. dependencyResolutionManagement below does NOT apply to
// plugin resolution, so without this block Gradle falls back to the public Plugin Portal.
pluginManagement {
    val artifactoryUser: String = System.getenv("ARTIFACTORY_USER") ?: ""
    val artifactoryPassword: String = System.getenv("ARTIFACTORY_PASSWORD") ?: ""
    val hasArtifactoryCredentials = artifactoryUser.isNotEmpty() && artifactoryPassword.isNotEmpty()

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
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    val artifactoryUser: String = System.getenv("ARTIFACTORY_USER") ?: ""
    val artifactoryPassword: String = System.getenv("ARTIFACTORY_PASSWORD") ?: ""
    val hasArtifactoryCredentials = artifactoryUser.isNotEmpty() && artifactoryPassword.isNotEmpty()

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
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
