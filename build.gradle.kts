import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
  application
  kotlin("jvm") version "1.2.31"
}

application {
    mainClassName = "net.flaviusb.gitreadtocloudefs.GitReadToCloudEFS"
}

group = "net.flaviusb"

version = "0.0.1"

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib", "1.2.31"))
    compile("org.eclipse.jgit:org.eclipse.jgit:4.11.0.201803080745-r")
    compile("org.eclipse.jgit:org.eclipse.jgit.archive:4.11.0.201803080745-r")
    compile("commons-io:commons-io:2.6")
    compile("org.slf4j:slf4j-simple:1.7.25")
    compile("org.tukaani:xz:1.8")
}

kotlin {
  experimental.coroutines = Coroutines.ENABLE
}

