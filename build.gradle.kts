// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}
buildscript {
    dependencies {
        classpath(libs.javapoet)
    }
}

tasks.register("installGitHooks") {
    description = "Configures Git to use project hooks from scripts/hooks"
    group = "setup"
    doLast {
        providers.exec {
            commandLine("git", "config", "core.hooksPath", "scripts/hooks")
        }
        println("Git hooks installed successfully!")
    }
}

tasks.named("prepareKotlinBuildScriptModel") {
    dependsOn("installGitHooks")
}

true // Needed to make the Suppress annotation work for the plugins block