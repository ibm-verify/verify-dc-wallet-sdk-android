import com.android.build.api.dsl.LibraryExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("com.android.library")
}

val libs = the<LibrariesForLibs>()
val NAMESPACE: String by project
val GROUP: String by project
val POM_DESCRIPTION: String by project

group = GROUP
version = providers.gradleProperty("VERSION_NAME").get()

extensions.configure<LibraryExtension> {
    namespace = NAMESPACE
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "$NAMESPACE.test"
        testHandleProfiling = true
        testFunctionalTest = true
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("debug") {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = false
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }

    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
            excludes += listOf("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        if (this is org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions) {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
        }
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.time.ExperimentalTime"
        )
    }
}

// Disable AGP's javaDocReleaseGeneration (it hits a "PermittedSubclasses requires ASM9"
// error on this codebase). The javadoc.jar is still produced (as required for publishing) but
// is empty; Dokka, which previously filled it, was removed to shrink the build-tooling attack surface.
afterEvaluate {
    tasks.findByName("javaDocReleaseGeneration")?.enabled = false
}
