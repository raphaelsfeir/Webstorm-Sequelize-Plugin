plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "io.github.raphaelsfeir"
version = "1.0.0"

repositories { mavenCentral() }

intellij {
    type.set("IC")
    version.set("2024.2")
    plugins.set(listOf("org.jetbrains.plugins.terminal"))
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks {
    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("")
        changeNotes.set("""
        <ul>
            <li>First release of Sequelize Runner plugin</li>
            <li>Added tool window with migration/seed actions</li>
            <li>Environment support (dev/prod)</li>
        </ul>
    """.trimIndent())
    }
    runPluginVerifier {
        ideVersions.set(listOf("WS-2024.2"))
    }
    test { useJUnitPlatform() }
}

kotlin {
    jvmToolchain(17)
}
