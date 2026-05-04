package kredacted

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class KredactedGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.dependencies.add(
            "implementation",
            "${BuildConfig.GROUP}:kredacted-annotations:${BuildConfig.VERSION}",
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "kredacted.compiler"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.GROUP,
        artifactId = "kredacted-compiler",
        version = BuildConfig.VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }
}
