plugins {
    java
    application
}

group = "io.raftkv"
version = "0.1.0-SNAPSHOT"
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
repositories { mavenCentral() }
dependencies { testImplementation("org.junit.jupiter:junit-jupiter:5.13.4"); testRuntimeOnly("org.junit.platform:junit-platform-launcher") }
tasks.test { useJUnitPlatform() }
application { mainClass = "io.raftkv.sim.Main" }
tasks.register<JavaExec>("sim") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args = (project.findProperty("simArgs") as String? ?: "--seed=1").split(" ")
}
