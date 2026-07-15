plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

val javaVersion = 25

repositories {
    maven("https://jitpack.io")
    // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
}

dependencies {
    //compileOnly(libs.jetbrains.annotations)
    //compileOnly(libs.jspecify)
    implementation("com.github.saltAgain:LifeCommon:v1.1.51")


    compileOnly(fileTree("libs") {
        include("*.jar")
    })
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependencies {

        exclude(dependency("com.hypixel.hytale:Server:.*"))
        exclude(dependency("dev.scaffoldit:.*:.*"))

        exclude(dependency("curse.maven:hyui-.*:.*"))
        exclude(dependency("curse.maven:multiplehud-.*:.*"))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
    withSourcesJar()
}