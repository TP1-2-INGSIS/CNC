package cnc.printscript.profile

import cnc.printscript.LanguageVersion
import cnc.printscript.profile.v1_0.PrintScript10Profile
import cnc.printscript.profile.v1_1.PrintScript11Profile

object PrintScriptProfileRegistry {
    private val profiles: Map<LanguageVersion, PrintScriptProfile> = mapOf(
        LanguageVersion.V1_0 to PrintScript10Profile,
        LanguageVersion.V1_1 to PrintScript11Profile
    )

    fun forVersion(version: LanguageVersion): PrintScriptProfile =
        profiles[version] ?: error("No profile registered for version: $version")
}
