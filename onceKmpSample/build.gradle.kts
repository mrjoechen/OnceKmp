import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "space.joechen.oncekmpsample"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTestBuilder {}
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        commonMain.dependencies {
            implementation(project(":onceKmp"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
