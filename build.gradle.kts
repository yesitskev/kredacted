plugins {
    kotlin("jvm") version "2.3.10"
    id("org.jetbrains.dokka") version "2.2.0"
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
    }
}

val publishedModules = setOf("kredacted-annotations", "kredacted-compiler")

subprojects {
    if (name !in publishedModules) return@subprojects

    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["kotlin"])
                artifact(javadocJar)

                providers.gradleProperty("POM_ARTIFACT_ID").orNull?.let { artifactId = it }

                pom {
                    name.set(providers.gradleProperty("POM_NAME"))
                    description.set(providers.gradleProperty("POM_DESCRIPTION"))
                    url.set(providers.gradleProperty("POM_URL"))
                    inceptionYear.set(providers.gradleProperty("POM_INCEPTION_YEAR"))

                    licenses {
                        license {
                            name.set(providers.gradleProperty("POM_LICENCE_NAME"))
                            url.set(providers.gradleProperty("POM_LICENCE_URL"))
                            distribution.set(providers.gradleProperty("POM_LICENCE_DIST"))
                        }
                    }

                    scm {
                        url.set(providers.gradleProperty("POM_SCM_URL"))
                        connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
                        developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
                    }

                    issueManagement {
                        system.set(providers.gradleProperty("POM_ISSUE_SYSTEM"))
                        url.set(providers.gradleProperty("POM_ISSUE_URL"))
                    }

                    developers {
                        developer {
                            id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                            name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                            url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
                        }
                    }
                }
            }
        }

        repositories {
            // Local staging directory — drop-in for the new Sonatype Central Portal:
            // run `:<module>:publishMavenPublicationToCentralPortalRepository`, zip the
            // resulting build/central-portal/<group>/... tree, and upload at
            // https://central.sonatype.com/publishing.
            maven {
                name = "centralPortal"
                url = uri(rootProject.layout.buildDirectory.dir("central-portal"))
            }

            // Legacy Sonatype OSSRH; usable for namespaces registered before the
            // Central Portal migration. Credentials read from `ossrhUsername` /
            // `ossrhPassword` Gradle properties (typically set via env vars in CI).
            maven {
                name = "ossrh"
                url = uri(
                    if (version.toString().endsWith("SNAPSHOT")) {
                        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    } else {
                        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    }
                )
                credentials {
                    username = providers.gradleProperty("ossrhUsername").orNull
                    password = providers.gradleProperty("ossrhPassword").orNull
                }
            }
        }
    }

    extensions.configure<SigningExtension> {
        val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
        val signingKeyId = providers.gradleProperty("signingInMemoryKeyId").orNull
        val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull

        if (!signingKey.isNullOrBlank()) {
            if (signingKeyId.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKey, signingPassword)
            } else {
                useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            }
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
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
