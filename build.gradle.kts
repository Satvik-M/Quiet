buildscript {
    dependencies {
        // Pin the built-in Kotlin version AGP 9's Kotlin support uses,
        // rather than relying on AGP's undocumented default.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
