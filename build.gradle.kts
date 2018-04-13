import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
  application
  kotlin("jvm") version "1.2.31"
}

application {
    mainClassName = "net.flaviusb.gitreadtocloudefs.GitreadTocloudefsKt"
}

group = "net.flaviusb"

version = "0.0.1"

repositories {
    jcenter()
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
}

kotlin {
  experimental.coroutines = Coroutines.ENABLE
}

