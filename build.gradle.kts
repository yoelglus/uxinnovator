plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
}

// Workaround: build and run iOS app on the booted simulator (use from Android Studio terminal)
// Usage: ./gradlew runIos   (or override simulator: ./gradlew runIos -PsimulatorId=<UDID>)
// Workaround: build and run iOS app on the booted simulator (use from Android Studio terminal)
// Usage: ./gradlew runIos   (or override simulator: ./gradlew runIos -PsimulatorId=<UDID>)
tasks.register("runIos") {
    val simulatorId = providers.gradleProperty("simulatorId")
        .orElse("12D7AAD6-2F7E-431F-A7B3-8DCC70DA8209") // iPhone 17 iOS 26.5
    val userHome = providers.systemProperty("user.home")
    val projectPath = layout.projectDirectory.asFile.absolutePath

    doLast {
        val simId = simulatorId.get()
        val javaHome = "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
        val derivedData = "${userHome.get()}/Library/Developer/Xcode/DerivedData/iosApp-runIos"

        fun exec(vararg cmd: String, env: Map<String, String> = emptyMap(), ignoreError: Boolean = false) {
            val pb = ProcessBuilder(*cmd).inheritIO()
            pb.environment().putAll(env)
            val exit = pb.start().waitFor()
            if (!ignoreError && exit != 0) error("Command failed (exit $exit): ${cmd.toList()}")
        }

        exec("xcrun", "simctl", "boot", simId, ignoreError = true)
        exec(
            "xcrun", "xcodebuild",
            "-project", "$projectPath/iosApp/iosApp.xcodeproj",
            "-scheme", "iosApp",
            "-destination", "id=$simId",
            "-configuration", "Debug",
            "-derivedDataPath", derivedData,
            "build",
            env = mapOf("JAVA_HOME" to javaHome)
        )
        val productsDir = java.io.File("$derivedData/Build/Products/Debug-iphonesimulator")
        val app = productsDir.listFiles { f -> f.name.endsWith(".app") }?.firstOrNull()
            ?: error("No .app found in $productsDir")
        exec("xcrun", "simctl", "install", simId, app.absolutePath)
        exec("xcrun", "simctl", "launch", simId, "com.sliide.uxinnovator.UXInnovator")
    }
}