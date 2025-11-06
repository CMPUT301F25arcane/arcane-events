plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.arcane"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arcane"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
}

// JavaDoc task configuration
// Based on lab approach: add android.jar to classpath for JavaDoc generation
afterEvaluate {
    // Get Android SDK path and jar
    val androidSdkPath = android.sdkDirectory
    val androidJar = File(androidSdkPath, "platforms/android-${android.compileSdk}/android.jar")
    
    if (!androidJar.exists()) {
        throw GradleException("Android SDK jar not found at: ${androidJar.absolutePath}. Please ensure Android SDK platform ${android.compileSdk} is installed.")
    }
    
    tasks.register<Javadoc>("generateJavadoc") {
        // Set source files
        setSource(android.sourceSets.getByName("main").java.srcDirs)

        // Get the debug variant's JavaCompile task classpath (includes all dependencies)
        val debugVariant = android.applicationVariants.find { it.name == "debug" }
        val javaCompileTask = debugVariant?.let { variant ->
            tasks.findByName("compileDebugJavaWithJavac") as? JavaCompile
        }

        // Build classpath: use variant's compile classpath + android.jar
        if (javaCompileTask != null) {
            // Use the compile task's classpath which already has all dependencies
            classpath = javaCompileTask.classpath.plus(project.files(androidJar))
        } else {
            // Fallback: build classpath manually
            classpath = project.files(androidJar)
            val configNames = listOf(
                "debugRuntimeClasspath",
                "debugCompileClasspath",
                "runtimeClasspath",
                "compileClasspath"
            )
            configNames.forEach { configName ->
                val config = configurations.findByName(configName)
                if (config != null && config.isCanBeResolved) {
                    classpath = classpath.plus(config)
                }
            }
        }

        // Add generated directories
        val generatedDirs = listOf(
            File(project.buildDir, "generated/source/buildConfig/debug"),
            File(project.buildDir, "generated/source/r/debug"),
            File(project.buildDir, "generated/data_binding_base_class_source_out/debug/out"),
            File(project.buildDir, "intermediates/javac/debug/classes"),
            File(project.buildDir, "intermediates/javac/debug/compileDebugJavaWithJavac/classes")
        )
        generatedDirs.forEach { dir ->
            if (dir.exists()) {
                classpath = classpath.plus(project.files(dir))
            }
        }

        // Exclude generated files and test files from source
        exclude("**/R.java")
        exclude("**/BuildConfig.java")
        exclude("**/databinding/**")
        exclude("**/test/**")
        exclude("**/androidTest/**")

        // Set destination directory
        setDestinationDir(file("${project.buildDir}/docs/javadoc"))

        // Configure options
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            docEncoding = "UTF-8"
            charSet = "UTF-8"
            memberLevel = JavadocMemberLevel.PUBLIC
            links("https://developer.android.com/reference/")
            links("https://docs.oracle.com/javase/8/docs/api/")
            // Suppress warnings for missing Android SDK documentation
            addStringOption("Xdoclint:none", "-quiet")
        }

        // Fail on error
        isFailOnError = false

        // Depend on compilation task to ensure classes are generated
        val compileTask = tasks.findByName("compileDebugJavaWithJavac")
        if (compileTask != null) {
            dependsOn(compileTask)
        } else {
            // Try alternative task names
            val altTaskNames = listOf("compileDebugJava", "compileDebugSources", "assembleDebug")
            altTaskNames.forEach { taskName ->
                tasks.findByName(taskName)?.let { dependsOn(it) }
            }
        }
        
    }
    
    // Separate task to always open browser (runs even when JavaDoc is UP-TO-DATE)
    tasks.register("openJavadoc") {
        group = "documentation"
        description = "Opens the generated JavaDoc in the browser"
        
        // Always run this task (never UP-TO-DATE)
        outputs.upToDateWhen { false }
        
        // Depend on JavaDoc generation
        dependsOn("generateJavadoc")
        
        doLast {
            val javadocIndex = file("${project.buildDir}/docs/javadoc/index.html")
            val javadocUrl = "file://${javadocIndex.absolutePath.replace(" ", "%20")}"
            
            if (javadocIndex.exists()) {
                println("\n" + "=".repeat(70))
                println("🌐 Opening JavaDoc in browser...")
                println("=".repeat(70))
                
                // Force browser to open - try multiple methods aggressively
                val os = System.getProperty("os.name").lowercase()
                var browserOpened = false
                
                // Method 1: Use Runtime.exec (works better in IDEs)
                try {
                    if (os.contains("mac")) {
                        Runtime.getRuntime().exec(arrayOf("open", javadocIndex.absolutePath))
                        Thread.sleep(500) // Give it time to open
                        browserOpened = true
                    } else if (os.contains("win")) {
                        Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "", javadocIndex.absolutePath))
                        Thread.sleep(500)
                        browserOpened = true
                    } else {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", javadocIndex.absolutePath))
                        Thread.sleep(500)
                        browserOpened = true
                    }
                } catch (e: Exception) {
                    // Continue to next method
                }
                
                // Method 2: Use project.exec as backup
                if (!browserOpened) {
                    try {
                        when {
                            os.contains("mac") -> {
                                project.exec {
                                    commandLine("open", javadocIndex.absolutePath)
                                    isIgnoreExitValue = true
                                }
                                browserOpened = true
                            }
                            os.contains("win") -> {
                                project.exec {
                                    commandLine("cmd", "/c", "start", "", javadocIndex.absolutePath)
                                    isIgnoreExitValue = true
                                }
                                browserOpened = true
                            }
                            else -> {
                                project.exec {
                                    commandLine("xdg-open", javadocIndex.absolutePath)
                                    isIgnoreExitValue = true
                                }
                                browserOpened = true
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                
                if (browserOpened) {
                    println("✅ Browser opened successfully!")
                } else {
                    println("⚠️  Browser did not open automatically")
                }
                
                // Always print the URL
                println("\n" + "─".repeat(70))
                println("📍 JavaDoc URL:")
                println("   $javadocUrl")
                println("─".repeat(70))
                println("\n💡 If browser didn't open, copy the URL above and paste in your browser")
                println("=".repeat(70) + "\n")
            } else {
                println("\n⚠️  JavaDoc index file not found at: ${javadocIndex.absolutePath}")
                println("   Please run 'generateJavadoc' first")
            }
        }
    }
    
    // Make generateJavadoc also open browser by default
    tasks.named("generateJavadoc").configure {
        finalizedBy("openJavadoc")
    }
}