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
