// TODO: revisar si es correcto tenerlo aca 
//        vvv

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

repositories {
    mavenCentral()
}

ktlint {
    debug.set(true)
    outputToConsole.set(true)
}
// Root build file - convention plugins are defined in buildSrc/
