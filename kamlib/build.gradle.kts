import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.vanniktech.maven.publish)
    id("org.jetbrains.dokka") version "1.9.20"
}

android {
    namespace = "com.example.kamlib"
    compileSdk = 34

    defaultConfig {
        aarMetadata {
            minCompileSdk = 24
        }
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    // The publishing block tells the Android Gradle Plugin what to publish.
    // The vanniktech plugin will use this configuration.
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar() // Dokka plugin will generate the Javadoc
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Configuration for the Maven Publish plugin
mavenPublishing {
    // Set the coordinates from gradle.properties
    coordinates(
        groupId = project.property("GROUP").toString(),
        artifactId = "kamvision", // This is usually the module name
        version = project.property("VERSION_NAME").toString()
    )

    // Configure the POM (Project Object Model) with metadata for Maven Central
    pom {
        name.set(project.property("POM_NAME").toString())
        description.set(project.property("POM_DESCRIPTION").toString())
        url.set(project.property("POM_URL").toString())

        licenses {
            license {
                name.set(project.property("POM_LICENCE_NAME").toString())
                url.set(project.property("POM_LICENCE_URL").toString())
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set(project.property("POM_DEVELOPER_ID").toString())
                name.set(project.property("POM_DEVELOPER_NAME").toString())
                email.set(project.property("POM_DEVELOPER_EMAIL").toString())
            }
        }

        scm {
            url.set(project.property("POM_SCM_URL").toString())
            connection.set(project.property("POM_SCM_CONNECTION").toString())
            developerConnection.set(project.property("POM_SCM_DEV_CONNECTION").toString())
        }
    }

    // This automatically configures the plugin to publish to the new Central Portal.
    // It reads your 'mavenCentralUsername' and 'mavenCentralPassword' (token) from gradle.properties.
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // This automatically finds your GPG signing configuration from gradle.properties and signs all artifacts.
    signAllPublications()
}
