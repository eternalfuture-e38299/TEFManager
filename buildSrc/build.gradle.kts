plugins {
    kotlin("jvm") version "2.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup:kotlinpoet:2.2.0")
    implementation("org.json:json:20251224")
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}