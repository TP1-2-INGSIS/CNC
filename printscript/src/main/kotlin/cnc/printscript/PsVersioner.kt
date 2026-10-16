package cnc.printscript

import cnc.language.LanguageFacade

object PsVersioner {
    fun version(version: String): LanguageFacade = PrintScriptFacade(PrintScriptVersion.from(version))
}
