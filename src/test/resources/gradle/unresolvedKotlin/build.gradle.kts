plugins {
    java
    `maven-publish`
}

fun javaProjects() = subprojects.filter {
    File(it.projectDir, "src").isDirectory
}

val currentVersion: String by project

allprojects {
    group = "org.jfrog.test.gradle.publish"
    version = currentVersion
    status = "Integration"

    repositories {
        mavenCentral()
    }
}

configure(javaProjects()) {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation("missing:dependency:404")
    }

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("mavenJava") {
                from(components.getByName("java"))
                artifact(file("$rootDir/gradle.properties"))
            }
        }
    }
}

project("api") {
    apply(plugin = "ivy-publish")

    configure<PublishingExtension> {
        publications {
            register<IvyPublication>("ivyJava") {
                from(components.getByName("java"))

                artifact(file("$rootDir/settings.gradle.kts")) {
                    name = "gradle-settings"
                    extension = "txt"
                    type = "text"
                }
                // The config below will add a extra attribute to the ivy.xml
                // See http://ant.apache.org/ivy/history/latest-milestone/concept.html#extra
                descriptor.withXml {
                    val info = asNode().get("info") as groovy.util.NodeList
                    val first = info.first() as groovy.util.Node
                    first.attributes()["e:architecture"] = "amd64"
                }
            }
        }
    }
}

