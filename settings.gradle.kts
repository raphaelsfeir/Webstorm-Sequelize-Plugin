pluginManagement {
    repositories {
        gradlePluginPortal()   // <-- indispensable pour org.jetbrains.intellij
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

plugins {
    // tu peux garder Foojay pour la résolution des toolchains JDK
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "webstorm-sequelize-plugin"
