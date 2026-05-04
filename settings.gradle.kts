pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
    includeBuild("kredacted-gradle-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kredacted"

include("kredacted-annotations")
include("kredacted-compiler")
include("sample")