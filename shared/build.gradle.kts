import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    androidLibrary {
        namespace = "fr.stageacademy.quiz.shared"
        compileSdk = 36
        minSdk = 26
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    sourceSets.commonMain.dependencies {
        implementation(compose.animation)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.components.resources)
    }

    sourceSets.androidMain {
        kotlin.srcDir("src/jvmShared/kotlin")
    }
    sourceSets.jvmMain {
        kotlin.srcDir("src/jvmShared/kotlin")
    }

    sourceSets.androidMain.dependencies {
        val cameraXVersion = "1.4.2"
        implementation("androidx.activity:activity-compose:1.10.1")
        implementation("androidx.camera:camera-camera2:$cameraXVersion")
        implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
        implementation("androidx.camera:camera-view:$cameraXVersion")
    }

    sourceSets.jvmMain.dependencies {
        implementation(compose.desktop.currentOs)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
        implementation("com.github.sarxos:webcam-capture:0.3.12")
        implementation("org.json:json:20250517")
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}

compose.resources {
    packageOfResClass = "fr.stageacademy.quiz.resources"
}
