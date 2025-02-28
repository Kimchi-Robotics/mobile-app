

plugins {
    alias(libs.plugins.android.application);
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.kimchi.deliverybot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kimchi.deliverybot"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.github.MikeOrtiz:TouchImageView:3.6")

    implementation("io.grpc:grpc-stub:1.52.1")
    implementation("io.grpc:grpc-protobuf:1.52.1")
    implementation("io.grpc:grpc-okhttp:1.52.1")
    implementation("io.grpc:protoc-gen-grpc-kotlin:1.3.0")
    implementation("io.grpc:grpc-kotlin-stub:1.3.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.12")
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.androidx.storage)
    implementation(libs.androidx.foundation.layout.android)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.12"
    }
    plugins {
        create ("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.52.1"
        }
        create ("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create ("grpc") {option("lite")}
                create ("grpckt") {option("lite")}
            }
            it.builtins {
                create ("kotlin") {option("lite")}
                create ("java") {option("lite")}
            }
        }
    }
}