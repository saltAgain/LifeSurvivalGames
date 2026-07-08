rootProject.name = "template"

plugins {
    // See documentation on https://scaffoldit.dev
    id("dev.scaffoldit") version "0.2.+"
}

// Would you like to do a split project?
// Create a folder named "common", then configure details with `common { }`

hytale {
    usePatchline("release")
    useVersion("latest")

    repositories {
        // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
    }

    dependencies {
        // Any external dependency you also want to include
        implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")
        implementation("com.zaxxer:HikariCP:6.3.0")
    }

    manifest {
        Group = "Saltt"
        Name = "template"
        Main = "gg.emberfall.template.Main"
    }
}