import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
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
            implementation(libs.kotlinx.atomicfu)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "space.joechen"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(17)
    }
}

val publishGroupId = providers.gradleProperty("ONCEKMP_GROUP").orElse("space.joechen")
val publishArtifactId = providers.gradleProperty("ONCEKMP_ARTIFACT_ID").orElse("oncekmp")
val publishVersion = providers.gradleProperty("ONCEKMP_VERSION").orElse("0.1.0")
val publishRepoUrl = providers.gradleProperty("ONCEKMP_REPO_URL").orElse("https://github.com/joechen/OnceKmp")
val publishScmConnection = providers.gradleProperty("ONCEKMP_SCM_CONNECTION")
    .orElse("scm:git:git://github.com/joechen/OnceKmp.git")
val publishScmDeveloperConnection = providers.gradleProperty("ONCEKMP_SCM_DEVELOPER_CONNECTION")
    .orElse("scm:git:ssh://git@github.com/joechen/OnceKmp.git")
val shouldSignPublications = providers.gradleProperty("ONCEKMP_SIGN_PUBLICATIONS").orElse("true")

mavenPublishing {
    coordinates(
        publishGroupId.get(),
        publishArtifactId.get(),
        publishVersion.get(),
    )
    publishToMavenCentral()
    if (shouldSignPublications.get().toBoolean()) {
        signAllPublications()
    }

    pom {
        name.set("onceKmp")
        description.set("Kotlin Multiplatform library for one-off operations, inspired by jonfinerty/Once.")
        url.set(publishRepoUrl.get())
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("joechen")
                name.set("Joe Chen")
                email.set("mrjctech@gmail.com")
            }
        }

        scm {
            url.set(publishRepoUrl.get())
            connection.set(publishScmConnection.get())
            developerConnection.set(publishScmDeveloperConnection.get())
        }
    }
}
