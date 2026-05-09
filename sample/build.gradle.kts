plugins {
    id("io.github.yesitskev.kredacted")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(":kredacted-annotations:publishToMavenLocal")
    dependsOn(":kredacted-compiler:publishToMavenLocal")
}