plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "dev.manel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("dev.manel.gametracker.MainApp")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "dev.manel.gametracker.MainApp"
    }
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Exec>("jpackageLinux") {
    dependsOn("jar")
    commandLine(
        "jpackage",
        "--input", "build/libs",
        "--main-jar", "GameTracker-1.0-SNAPSHOT.jar",
        "--main-class", "dev.manel.gametracker.MainApp",
        "--name", "GameTracker",
        "--app-version", "1.0",
        "--type", "deb",
        "--dest", "build/dist",
        "--module-path", configurations.runtimeClasspath.get()
            .filter { it.name.contains("javafx") }
            .joinToString(":") { it.absolutePath },
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}

tasks.register<Exec>("jpackageWindows") {
    dependsOn("jar")
    commandLine(
        "jpackage",
        "--input", "build/libs",
        "--main-jar", "GameTracker-1.0-SNAPSHOT.jar",
        "--main-class", "dev.manel.gametracker.MainApp",
        "--name", "GameTracker",
        "--app-version", "1.0",
        "--type", "exe",
        "--dest", "build/dist",
        "--module-path", configurations.runtimeClasspath.get()
            .filter { it.name.contains("javafx") }
            .joinToString(";") { it.absolutePath },
        "--add-modules", "javafx.controls,javafx.fxml",
        "--win-menu",
        "--win-shortcut"
    )
}

tasks.register<Exec>("jpackageMac") {
    dependsOn("jar")
    commandLine(
        "jpackage",
        "--input", "build/libs",
        "--main-jar", "GameTracker-1.0-SNAPSHOT.jar",
        "--main-class", "dev.manel.gametracker.MainApp",
        "--name", "GameTracker",
        "--app-version", "1.0",
        "--type", "dmg",
        "--dest", "build/dist",
        "--module-path", configurations.runtimeClasspath.get()
            .filter { it.name.contains("javafx") }
            .joinToString(":") { it.absolutePath },
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}