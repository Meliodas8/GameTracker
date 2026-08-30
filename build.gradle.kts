import java.util.Properties

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
    // el launcher que trae Gradle 8.13 es más viejo que el engine 5.12 y rompe el descubrimiento
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
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

/**
 * La version sale de version.properties, que es lo que el workflow reescribe a
 * partir del tag. Sin esto los ficheros publicados se llamaban siempre 1.0.
 */
val appVersion: String = Properties().apply {
    file("src/main/resources/dev/manel/gametracker/version.properties")
        .inputStream().use { load(it) }
}.getProperty("version") ?: "1.0.0"

/** Las tres tareas de empaquetado solo se diferencian en tipo, separador y extras. */
fun registerJpackage(
    taskName: String,
    type: String,
    pathSeparator: String = ":",
    extraModules: List<String> = emptyList(),
    extraArgs: List<String> = emptyList()
) {
    tasks.register<Exec>(taskName) {
        dependsOn("jar")
        val jar = tasks.jar.get().archiveFile.get().asFile
        val modules = listOf("javafx.controls", "javafx.fxml", "java.net.http", "jdk.crypto.ec") + extraModules
        commandLine(
            listOf(
                "jpackage",
                "--input", jar.parent,
                "--main-jar", jar.name,
                "--main-class", "dev.manel.gametracker.MainApp",
                "--name", "GameTracker",
                "--app-version", appVersion,
                "--type", type,
                "--dest", "build/dist",
                "--module-path", configurations.runtimeClasspath.get()
                    .filter { it.name.contains("javafx") }
                    .joinToString(pathSeparator) { it.absolutePath },
                "--add-modules", modules.joinToString(",")
            ) + extraArgs
        )
    }
}

registerJpackage("jpackageLinux", "deb")
registerJpackage("jpackageWindows", "exe", pathSeparator = ";",
    extraModules = listOf("jdk.crypto.mscapi"),
    extraArgs = listOf("--win-menu", "--win-shortcut"))
registerJpackage("jpackageMac", "dmg")
