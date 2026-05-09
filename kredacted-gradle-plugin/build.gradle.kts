import java.util.Properties

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.10"
    id("com.gradle.plugin-publish") version "1.3.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.10")
}

// kredacted-gradle-plugin is an `includeBuild` from the parent settings.gradle.kts,
// so it does not automatically inherit the parent's gradle.properties. We read the
// shared metadata explicitly so version/group/url/etc. live in a single file.
val rootProps = Properties().apply {
    rootDir.parentFile.resolve("gradle.properties").inputStream().use { load(it) }
}

group = rootProps.getProperty("group") ?: error("`group` missing from root gradle.properties")
version = rootProps.getProperty("version") ?: error("`version` missing from root gradle.properties")

val pluginUrl = rootProps.getProperty("POM_URL").orEmpty()
val pluginScmUrl = rootProps.getProperty("POM_SCM_URL").orEmpty()

gradlePlugin {
    website.set(pluginUrl)
    vcsUrl.set(pluginScmUrl)

    plugins {
        create("kredacted") {
            id = "io.github.yesitskev.kredacted"
            displayName = "Kredacted"
            description = "Kotlin compiler plugin that rewrites toString on @Redacted " +
                "classes and properties to mask sensitive values."
            tags.set(
                listOf(
                    "kotlin",
                    "kotlin-compiler-plugin",
                    "redaction",
                    "tostring",
                    "logging",
                    "security",
                )
            )
            implementationClass = "kredacted.KredactedGradlePlugin"
        }
    }
}

// Generate a BuildConfig.kt so the plugin can reference its own group/version
// instead of hardcoding it in two places.
val groupValue = project.group.toString()
val versionValue = project.version.toString()

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConfig/kotlin/main")
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("kredacted/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package kredacted

            internal object BuildConfig {
                const val GROUP: String = "$groupValue"
                const val VERSION: String = "$versionValue"
            }

            """.trimIndent()
        )
    }
}

kotlin {
    sourceSets["main"].kotlin.srcDir(generateBuildConfig)
}
