// kamlib/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish") // Maven Publish plugin
//    id("signing")       // Signing plugin
//    id("org.jetbrains.dokka") version "1.9.20" // Dokka plugin
}

android {
    namespace = "io.github.kamyab9k.kamlib"
    compileSdk = 34

    defaultConfig {
        aarMetadata {
            minCompileSdk = 24
        }
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    publishing {
        singleVariant("release") {
            withSourcesJar() // Includes source code in the published artifact
            withJavadocJar() // Includes Javadoc in the published artifact
        }
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


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") { // Change "maven" to "release" to match singleVariant
                from(components["release"])

                // JitPack expects groupId, artifactId, and version based on GitHub info.
                // While you can set them here, JitPack often overrides them to conform
                // to its standard: com.github.YOUR_USERNAME:YOUR_REPO_NAME:YOUR_TAG.
                // Keeping these can help with local testing, but they're not strictly
                // necessary for JitPack to build if the convention is followed.

                // For JitPack, it's CRUCIAL that groupId is com.github.YOUR_GITHUB_USERNAME
                // and artifactId is YOUR_REPO_NAME.
                // version should match your Git tag.

                groupId = "com.github.kamyab9k" // Must be com.github.YOUR_GITHUB_USERNAME
                artifactId = "KamVision"       // Must be YOUR_REPO_NAME (case-sensitive)
                version = project.version.toString() // JitPack uses Git tag for version.
                // Use project.version or define directly.
                // For consistency, define `version` at the project level,
                // or use a property like `rootProject.extra["VERSION_NAME"]`
                // if you truly want to use a property, though JitPack ignores it.

                // You can keep the POM metadata if you want, JitPack will generate a POM.
                // However, for JitPack, much of this is redundant as it's primarily used
                // for Maven Central compliance. It won't hurt to keep it though.
                pom {
                    name.set(project.properties["POM_NAME"].toString())
                    description.set(project.properties["POM_DESCRIPTION"].toString())
                    url.set(project.properties["POM_URL"].toString())
                    inceptionYear.set(project.properties["POM_INCEPTION_YEAR"].toString())

                    licenses {
                        license {
                            name.set(project.properties["POM_LICENCE_NAME"].toString())
                            url.set(project.properties["POM_LICENCE_URL"].toString())
                            distribution.set(project.properties["POM_LICENSE_DIST"].toString())
                        }
                    }

                    developers {
                        developer {
                            id.set(project.properties["POM_DEVELOPER_ID"].toString())
                            name.set(project.properties["POM_DEVELOPER_NAME"].toString())
                            email.set(project.properties["POM_DEVELOPER_EMAIL"].toString())
                        }
                    }

                    scm {
                        connection.set(project.properties["POM_SCM_CONNECTION"].toString())
                        developerConnection.set(project.properties["POM_SCM_DEV_CONNECTION"].toString())
                        url.set(project.properties["POM_SCM_URL"].toString())
                    }
                }
            }
        }

    }

}




// Below is for publishing to Maven Central which is postponed
//
//
//afterEvaluate {
//    publishing {
//        publications {
//            create<MavenPublication>("maven") {
//                from(components["release"])
//
//                groupId = project.properties["GROUP"].toString()
//                artifactId = project.properties["POM_ARTIFACT_ID"].toString()
//                version = project.properties["VERSION_NAME"].toString()
//
//                pom {
//                    name.set(project.properties["POM_NAME"].toString())
//                    description.set(project.properties["POM_DESCRIPTION"].toString())
//                    url.set(project.properties["POM_URL"].toString())
//                    inceptionYear.set(project.properties["POM_INCEPTION_YEAR"].toString())
//
//                    licenses {
//                        license {
//                            name.set(project.properties["POM_LICENCE_NAME"].toString())
//                            url.set(project.properties["POM_LICENCE_URL"].toString())
//                            distribution.set(project.properties["POM_LICENSE_DIST"].toString())
//                        }
//                    }
//
//                    developers {
//                        developer {
//                            id.set(project.properties["POM_DEVELOPER_ID"].toString())
//                            name.set(project.properties["POM_DEVELOPER_NAME"].toString())
//                            email.set(project.properties["POM_DEVELOPER_EMAIL"].toString())
//                        }
//                    }
//
//                    scm {
//                        connection.set(project.properties["POM_SCM_CONNECTION"].toString())
//                        developerConnection.set(project.properties["POM_SCM_DEV_CONNECTION"].toString())
//                        url.set(project.properties["POM_SCM_URL"].toString())
//                    }
//                }
//            }
//        }
//
//        // Configure repositories for publishing
//        repositories {
//            maven {
//                val releasesRepoUrl =
//                    uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
//                // For snapshots, https://central.sonatype.com/repository/maven-snapshots/ is the direct repo,
//                // but the staging API URL might also work for compatibility.
//                val snapshotsRepoUrl =
//                    uri("https://central.sonatype.com/repository/maven-snapshots/")
//
//                url = if (version.toString()
//                        .endsWith("SNAPSHOT")
//                ) snapshotsRepoUrl else releasesRepoUrl
//
//                credentials {
//                    username = project.properties["mavenCentralUsername"].toString()
//                    password = project.properties["mavenCentralPassword"].toString()
//                }
//            }
//        }
//    }
//
//    signing {
//        useGpgCmd()
//        sign(publishing.publications["maven"])
//    }
//}