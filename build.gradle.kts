import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
  application
  kotlin("jvm") version "1.2.31"
}

application {
    mainClassName = "net.flaviusb.gitreadtocloudefs.GitreadtocloudefsKt"
}

tasks.withType<Jar> {
  manifest {
    attributes(mapOf(
                "Main-Class" to "net.flaviusb.gitreadtocloudefs.GitreadtocloudefsKt"
    ))
  }
}

group = "net.flaviusb"

version = "0.0.1"

repositories {
    jcenter()
    mavenCentral()
    maven(url="http://dl.bintray.com/kotlin/kotlin-eap")
    maven(url="https://kotlin.bintray.com/kotlinx")
}
val kotlinVersion = "1.2.30"
val coroutines_version = "0.21.1"
val serialization_version = "0.4.1"

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    compile("org.eclipse.jgit:org.eclipse.jgit:4.11.0.201803080745-r")
    compile("org.eclipse.jgit:org.eclipse.jgit.archive:4.11.0.201803080745-r")
    compile("commons-io:commons-io:2.6")
    compile("org.slf4j:slf4j-simple:1.7.25")
    compile("org.tukaani:xz:1.8")
    compile(fileTree("unmanaged_jars"))
    compile(kotlin("reflect", kotlinVersion))
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    compile("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serialization_version")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")
    compile("org.apache.httpcomponents:httpmime:4.3.5")
    compile("org.bouncycastle:bcprov-jdk15on:1.59")
    compile("org.xerial:sqlite-jdbc:3.21.0.1")
}

kotlin {
  experimental.coroutines = Coroutines.ENABLE
}

