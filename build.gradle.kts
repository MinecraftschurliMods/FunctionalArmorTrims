@file:Suppress("PropertyName")

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import net.minecraftforge.gradle.userdev.DependencyManagementExtension
import net.minecraftforge.gradle.userdev.tasks.JarJar
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    idea
    eclipse
    `maven-publish`
    id("net.minecraftforge.gradle")
    id("org.spongepowered.mixin")
    id("org.parchmentmc.librarian.forgegradle")
}

// region helpers

val modGroup: String by project
val modId: String by project
val modName: String by project
val modVersion: String by project
val modAuthors: String by project
val githubRepo: String by project
val javaVersion: String by project
val mixinVersion: String by project
val mappingsChannel: String by project
val mappingsVersion: String by project
val mcVersion: String by project
val forgeVersion: String by project
val vendor: String by project

val issueTrackerUrl by extra("https://github.com/${githubRepo}/issues")

val SourceSetContainer.api: NamedDomainObjectProvider<SourceSet>
    get() = named("api")

val SourceSetContainer.data: NamedDomainObjectProvider<SourceSet>
    get() = named("data")

val Provider<SourceSet>.output: Provider<SourceSetOutput>
    get() = map { it.output }

val Provider<SourceSet>.allSource: Provider<SourceDirectorySet>
    get() = map { it.allSource }

val Provider<SourceSet>.compileClasspath: Provider<FileCollection>
    get() = map { it.compileClasspath }

fun DependencyManagementExtension.deobf(dependency: Any, configure: Dependency.() -> Unit) = deobf(dependency, closureOf(configure))

fun Project.fileTree(dir: String, include: String) = fileTree("dir" to dir, "include" to include)

// endregion

// region project

group = modGroup
version = "${mcVersion}-${modVersion}"
base.archivesName.set(modId)

if (System.getenv("RELEASE_TYPE") != null) {
    status = System.getenv("RELEASE_TYPE").lowercase()
    if (status == "snapshot") status = (status as String).uppercase()
} else {
    status = "SNAPSHOT"
}

if (status != "release") {
    version = "${version}-${status}"
}

// endregion

mixin {
    add(sourceSets.main.get(), "${modId}.refmap.json")
    config("${modId}.mixins.json")
    dumpTargetOnFailure = true
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
        if (System.getenv("GITHUB_ACTIONS") == null || System.getenv("GITHUB_ACTIONS").isEmpty()) {
            vendor.set(JvmVendorSpec.matching("JetBrains s.r.o."))
        } else {
            vendor.set(JvmVendorSpec.ADOPTIUM)
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir(file("src/main/generated"))
            exclude(".cache")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Sponge maven"
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
        content {
            includeGroup("org.spongepowered")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${mcVersion}-${forgeVersion}")
    annotationProcessor("org.spongepowered:mixin:${mixinVersion}:processor")
    compileOnly("org.jetbrains:annotations:23.0.0")
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)
    copyIdeResources.set(true)
    runs {
        create("client") {
            workingDirectory(file("run"))
            property("forge.logging.console.level", "debug")
            mods.register(modId) {
                source(sourceSets.main.get())
            }
        }
        create("server") {
            workingDirectory(file("run"))
            property("forge.logging.console.level", "debug")
            mods.register(modId) {
                source(sourceSets.main.get())
            }
        }
        create("data") {
            workingDirectory(file("run"))
            property("forge.logging.console.level", "debug")
            args("--mod", modId, "--all", "--output", file("src/main/generated/"), "--existing", file("src/main/resources/"))
            mods.register(modId) {
                source(sourceSets.main.get())
            }
        }
    }
}

tasks {
    withType<Jar>().configureEach {
        manifest {
            attributes(mapOf(
                "Maven-Artifact" to "${modGroup}:${base.archivesName}:${project.version}",
                "Specification-Title" to base.archivesName,
                "Specification-Vendor" to vendor,
                "Specification-Version" to "1",
                "Implementation-Title" to base.archivesName,
                "Implementation-Version" to modVersion,
                "Implementation-Vendor" to vendor,
                "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                "Built-On" to "${mcVersion}-${forgeVersion}",
                "Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                "FMLModType" to "MOD"
            ))
        }
    }

    named<Jar>("jar") {
        finalizedBy("reobfJar")
    }

    processResources {
        val buildProps = project.properties.toMutableMap()
        buildProps.values.removeIf { it !is CharSequence && it !is Number && it !is Boolean }
        inputs.properties(buildProps)

        filesMatching("META-INF/mods.toml") {
            expand(buildProps)
        }
        // minify json files
        doLast {
            fileTree(dir = outputs.files.asPath, include = "**/*.json").forEach {
                it.writeText(JsonOutput.toJson(JsonSlurper().parse(it)))
            }
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}



publishing {
    publications.create<MavenPublication>("${base.archivesName.get()}ToMaven") {
        groupId = project.group as String
        artifactId = base.archivesName.get()
        version = project.version as String
        from(components["java"])
        fg.component(this)
        pom {
            name.set(modName)
            url.set("https://github.com/${githubRepo}")
            packaging = "jar"
            scm {
                connection.set("scm:git:git://github.com/${githubRepo}.git")
                developerConnection.set("scm:git:git@github.com:${githubRepo}.git")
                url.set("https://github.com/${githubRepo}")
            }
            issueManagement {
                system.set("github")
                url.set(issueTrackerUrl)
            }
            organization {
                name.set("Minecraftschurli Mods")
                url.set("https://github.com/MinecraftschurliMods")
            }
            developers {
                developer {
                    id.set("minecraftschurli")
                    name.set("Minecraftschurli")
                    url.set("https://github.com/Minecraftschurli")
                    email.set("minecraftschurli@gmail.com")
                    organization.set("Minecraftschurli Mods")
                    organizationUrl.set("https://github.com/MinecraftschurliMods")
                    timezone.set("Europe/Vienna")
                }
            }
        }
    }
    repositories {
        maven {
            if ((System.getenv("MAVEN_USER") != null)
                && (System.getenv("MAVEN_PASSWORD") != null)
                && (System.getenv("MAVEN_URL") != null)) {
                url = uri(System.getenv("MAVEN_URL"))
                credentials {
                    username = System.getenv("MAVEN_USER")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            } else {
                url = uri("$buildDir/repo")
            }
        }
    }
}

