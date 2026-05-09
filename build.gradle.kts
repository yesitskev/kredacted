plugins {
    kotlin("jvm") version "2.3.10"
    id("org.jetbrains.dokka") version "2.2.0"
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    dokka(project(":kredacted-annotations"))
}

kotlin {
    jvmToolchain(17)
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }
}

val publishedModules = setOf("kredacted-annotations", "kredacted-compiler")

subprojects {
    if (name !in publishedModules) return@subprojects

    apply(plugin = "com.vanniktech.maven.publish")

    extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral(
            com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL,
            automaticRelease = false,
        )
        signAllPublications()
    }
}

tasks.test {
    useJUnitPlatform()
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.projectDirectory.dir("docs"))
    }
}
