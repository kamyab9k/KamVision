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
            withSourcesJar()
            withJavadocJar()
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
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.kamyab9k"
                artifactId = "KamVision"
                version = project.version.toString()

                pom {
                    name.set("KamVision")
                    description.set("Android library for Camera2 API in Jetpack compose.")
                    url.set("https://github.com/kamyab9k/KamVision")
                    inceptionYear.set("2025")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("kamyab9k")
                            name.set("Kamyab Khosravi")
                            email.set("m.khosravi.dev@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/kamyab9k/KamVision.git")
                        developerConnection.set("scm:git:ssh://github.com/kamyab9k/KamVision.git")
                        url.set("https://github.com/kamyab9k/KamVision")
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