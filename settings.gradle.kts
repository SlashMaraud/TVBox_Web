pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Cambiamos a 'PREFER_SETTINGS' para que mande este archivo sobre los demás
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Servidor oficial de Mozilla - FORZADO
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
        }
    }
}

rootProject.name = "TVBox_Web"
include(":app")
