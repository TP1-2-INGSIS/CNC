package cnc.printscript.profile

import cnc.language.LanguageProfile
import cnc.printscript.PrintScriptVersion
import cnc.printscript.profile.v1_0.PrintScript10Profile
import cnc.printscript.profile.v1_1.PrintScript11Profile

object PrintScriptProfileRegistry {
    private val profiles: Map<PrintScriptVersion, LanguageProfile> =
        mapOf(
            PrintScriptVersion.V1_0 to PrintScript10Profile,
            PrintScriptVersion.V1_1 to PrintScript11Profile,
        )

    fun forVersion(version: PrintScriptVersion): LanguageProfile =
        profiles[version] ?: error("No profile registered for version: $version")
}
