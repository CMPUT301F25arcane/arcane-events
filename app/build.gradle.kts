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
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.robolectric:robolectric:4.13")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
}

// JavaDoc task to generate HTML documentation
afterEvaluate {
    val javadocTask = tasks.register<Javadoc>("generateJavaDoc") {
        // Set source files from main source set
        val sourceSets = android.sourceSets.getByName("main")
        val sourceDirs = mutableListOf<File>()
        sourceDirs.addAll(sourceSets.java.srcDirs)
        
        // Add generated sources (ViewBinding, etc.) if they exist
        val generatedDirs = listOf(
            file("${project.layout.buildDirectory.get()}/generated/data_binding_base_class_source_out/debug/out"),
            file("${project.layout.buildDirectory.get()}/generated/ap_generated_sources/debug/out"),
            file("${project.layout.buildDirectory.get()}/generated/source/buildConfig/debug"),
            file("${project.layout.buildDirectory.get()}/generated/source/r/debug")
        )
        generatedDirs.forEach { dir ->
            if (dir.exists()) {
                sourceDirs.add(dir)
            }
        }
        
        setSource(sourceDirs)
        
        // Set destination directory
        setDestinationDir(file("${project.layout.buildDirectory.get()}/docs/javadoc"))
        
        // Get classpath from the first available variant (includes all dependencies)
        // This gets all resolved dependencies including Firebase, AndroidX, etc.
        val variant = android.applicationVariants.firstOrNull()
        val classpathFiles = mutableListOf<Any>()
        classpathFiles.add(android.bootClasspath)
        
        if (variant != null) {
            // Use the variant's compile classpath which has all resolved dependencies
            classpathFiles.add(variant.javaCompileProvider.get().classpath)
            
            // Also add generated sources (for ViewBinding classes) to classpath
            val generatedClasspathDirs = listOf(
                file("${project.layout.buildDirectory.get()}/generated/data_binding_base_class_source_out/debug/out"),
                file("${project.layout.buildDirectory.get()}/generated/ap_generated_sources/debug/out"),
                file("${project.layout.buildDirectory.get()}/generated/source/buildConfig/debug"),
                file("${project.layout.buildDirectory.get()}/generated/source/r/debug")
            )
            generatedClasspathDirs.forEach { dir ->
                if (dir.exists()) {
                    classpathFiles.add(dir)
                }
            }
        }
        
        classpath = project.files(classpathFiles)
        
        // JavaDoc options
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            memberLevel = JavadocMemberLevel.PUBLIC
            links("https://developer.android.com/reference/")
            links("https://firebase.google.com/docs/reference/android/")
            // Suppress all doclint checks and errors
            addStringOption("Xdoclint:none", "-quiet")
            addBooleanOption("author", true)
            addBooleanOption("version", true)
            // Continue even with errors
            addStringOption("Xmaxerrs", "10000")
            addStringOption("Xmaxwarns", "10000")
        }
        
        // Exclude generated files
        exclude("**/BuildConfig.java")
        exclude("**/R.java")
        
        // Fail on error (set to false if you want to continue despite warnings)
        isFailOnError = false
    }
    
    // Make JavaDoc depend on compilation so binding classes are generated
    val variant = android.applicationVariants.firstOrNull()
    if (variant != null) {
        javadocTask.configure {
            dependsOn(variant.javaCompileProvider)
        }
    }
}