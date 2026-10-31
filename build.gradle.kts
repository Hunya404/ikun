import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipFile

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19"
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
}
version = "1.0.0"
group = "com.github.ikun"
base { archivesName.set("ikun") }
repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases/")
}
dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation("net.fabricmc.fabric-api:fabric-api:0.155.2+26.1.2")
    implementation("net.fabricmc:fabric-language-kotlin:1.13.13+kotlin.2.4.10")
    compileOnly("com.terraformersmc:modmenu:18.0.0")
    testImplementation(kotlin("test"))
}
tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_25) } }
tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
tasks.named<Jar>("jar") {
    archiveVersion.set("${project.version}-26.1.2")
    from("LICENSE") { rename { "LICENSE_ikun" } }
    from("licenses") { into("licenses") }
    from("NOTICE.md")
}

val releaseJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
val verifyModSize = tasks.register("verifyModSize") {
    group = "verification"
    description = "Require the distributed mod to stay below 1 MB without bundled libraries."
    dependsOn(tasks.named("jar"))
    inputs.file(releaseJar)
    doLast {
        val file = releaseJar.get().asFile
        check(file.length() < 1_000_000L) { "${file.name} is ${file.length()} bytes; must be below 1,000,000 bytes." }
        ZipFile(file).use { zip ->
            check(zip.entries().asSequence().none { it.name.endsWith(".jar") }) { "The mod must not bundle runtime libraries." }
        }
        logger.lifecycle("Verified ${file.name}: ${file.length()} bytes (< 1 MB), no bundled libraries.")
    }
}
tasks.named("check") { dependsOn(verifyModSize) }
