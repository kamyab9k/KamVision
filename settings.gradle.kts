pluginManagement {
    repositories {
        google()
        maven {
            url = uri("https://maven.myket.ir")
        }
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.myket.ir")
        }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "KamVision"
include(":app")
include(":kamlib")
