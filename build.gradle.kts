import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

version = "1.0.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        // oldest supported platform; the plugin runs on newer IDEs too
        intellijIdeaCommunity("2024.2.6")
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // Java 21 bytecode: IDEs from 2024.2 on
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

    // ./gradlew verifyPlugin: checks the plugin against the IDE it has to run in
    pluginVerification {
        ides {
            create(IntelliJPlatformType.PhpStorm, "2026.2.3")
        }
    }
}
