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

val secretFile = rootDir.resolve("gradle.secret.properties")
val secretProps: Map<String, String> = if (secretFile.exists()) {
    java.util.Properties().apply { secretFile.inputStream().use { load(it) } }
        .entries.associate { (k, v) -> k.toString() to v.toString() }
} else emptyMap()

gradle.beforeProject {
    secretProps.forEach { (k, v) ->
        if (!hasProperty(k)) extensions.extraProperties.set(k, v)
    }
}