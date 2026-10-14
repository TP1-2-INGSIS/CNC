package cnc.printscript

object PsVersioner {
    fun version(version: String): PrintScriptFacade = PrintScriptEngine(LanguageVersion.from(version))
}
