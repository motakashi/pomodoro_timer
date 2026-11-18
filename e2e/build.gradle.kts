plugins {
    java
}

group = "motakashi.androidapp.pomodorotimer"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation("io.appium:java-client:10.1.1")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(":app:assembleDebug")

    systemProperty(
        "appium.app",
        rootProject.layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk").asFile.absolutePath
    )
    systemProperty(
        "appium.serverUrl",
        providers.gradleProperty("appiumServerUrl")
            .orElse(providers.environmentVariable("APPIUM_SERVER_URL"))
            .orElse("http://127.0.0.1:4723")
            .get()
    )
    systemProperty(
        "appium.deviceName",
        providers.gradleProperty("appiumDeviceName")
            .orElse("Android Emulator")
            .get()
    )
}
