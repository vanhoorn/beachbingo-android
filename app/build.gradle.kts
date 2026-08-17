import java.util.Properties

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

android {
    namespace = "com.bestfriends.beachbingo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bestfriends.beachbingo"
        minSdk = 26
        targetSdk = 35
        versionCode = 19
        versionName = "0.19.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.zxing.android.embedded)
    implementation(libs.konfetti.compose)
}

kapt {
    correctErrorTypes = true
}

// ── Theme lint ──────────────────────────────────────────────────────────────
// Fails the build if any .kt file outside ui/theme/ contains:
//   - Color(0x...)        → use a named constant from ui/theme/Color.kt
//   - fontSize = N.sp     → use MaterialTheme.typography.* or a constant from ui/theme/TextDimens.kt
tasks.register("checkHardcodedTheme") {
    group = "verification"
    description = "Fails if Color(0x...) or fontSize=N.sp appear outside ui/theme/"
    doLast {
        val srcDir = file("src/main/java")
        val themeDir = file("src/main/java/com/bestfriends/beachbingo/ui/theme")
        val colorPattern = Regex("""Color\(0x""")
        val fontSizePattern = Regex("""fontSize\s*=\s*\d+\.sp""")
        val violations = mutableListOf<String>()

        srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.absolutePath.startsWith(themeDir.absolutePath) }
            .forEach { file ->
                file.readLines().forEachIndexed { idx, line ->
                    if (colorPattern.containsMatchIn(line))
                        violations += "${file.relativeTo(srcDir)}:${idx + 1} — hardcoded Color(0x…) → use a named constant from ui/theme/Color.kt"
                    if (fontSizePattern.containsMatchIn(line))
                        violations += "${file.relativeTo(srcDir)}:${idx + 1} — hardcoded fontSize → use MaterialTheme.typography.* or a constant from ui/theme/TextDimens.kt"
                }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "\n\n⚠️  checkHardcodedTheme: ${violations.size} violation(s):\n\n" +
                violations.joinToString("\n") + "\n"
            )
        }
        println("✓ checkHardcodedTheme: no violations found.")
    }
}

tasks.named("check") { dependsOn("checkHardcodedTheme") }