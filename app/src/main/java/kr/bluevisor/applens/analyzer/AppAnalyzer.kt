package kr.bluevisor.applens.analyzer

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kr.bluevisor.applens.model.AppAnalysis
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.AppType
import java.io.File
import java.util.zip.ZipFile

class AppAnalyzer(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    fun getInstalledApps(): List<AppInfo> {
        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }
        
        return installedApps.mapNotNull { packageInfo ->
            try {
                createAppInfo(packageInfo)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }
    
    private fun createAppInfo(packageInfo: PackageInfo): AppInfo {
        val applicationInfo = packageInfo.applicationInfo ?: return AppInfo(
            packageName = packageInfo.packageName,
            appName = packageInfo.packageName,
            icon = null,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            installTime = packageInfo.firstInstallTime,
            updateTime = packageInfo.lastUpdateTime,
            appSize = 0L,
            isSystemApp = false,
            targetSdkVersion = 1,
            minSdkVersion = 1
        )
        
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        val icon = try {
            packageManager.getApplicationIcon(applicationInfo)
        } catch (e: Exception) {
            null
        }
        
        return AppInfo(
            packageName = packageInfo.packageName,
            appName = appName,
            icon = icon,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            installTime = packageInfo.firstInstallTime,
            updateTime = packageInfo.lastUpdateTime,
            appSize = getAppSize(applicationInfo),
            isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            targetSdkVersion = applicationInfo.targetSdkVersion,
            minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                applicationInfo.minSdkVersion
            } else {
                1
            }
        )
    }
    
    private fun getAppSize(applicationInfo: ApplicationInfo): Long {
        return try {
            val apkFile = File(applicationInfo.publicSourceDir)
            apkFile.length()
        } catch (e: Exception) {
            0L
        }
    }
    
    fun analyzeApp(appInfo: AppInfo): AppAnalysis {
        val detectedFrameworks = mutableListOf<String>()
        val nativeLibraries = mutableListOf<String>()
        val usedLibraries = mutableListOf<String>()
        val permissions = mutableListOf<String>()
        
        var hasFlutterAssets = false
        var hasReactNativeAssets = false
        var hasXamarinAssets = false
        var hasCordovaAssets = false
        var hasUnityAssets = false
        
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_PERMISSIONS or
                         PackageManager.GET_ACTIVITIES or
                         PackageManager.GET_SERVICES or
                         PackageManager.GET_RECEIVERS).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS
                )
            }
            
            // Extract permissions
            packageInfo.requestedPermissions?.forEach { permission ->
                permissions.add(permission)
            }
            
            // Analyze APK contents
            val apkPath = packageInfo.applicationInfo?.publicSourceDir
            analyzeApkContents(apkPath, detectedFrameworks, nativeLibraries, usedLibraries, appInfo.appType).let { analysis ->
                hasFlutterAssets = analysis.hasFlutterAssets
                hasReactNativeAssets = analysis.hasReactNativeAssets
                hasXamarinAssets = analysis.hasXamarinAssets
                hasCordovaAssets = analysis.hasCordovaAssets
                hasUnityAssets = analysis.hasUnityAssets
            }
            
        } catch (e: Exception) {
            // Handle analysis errors
        }
        
        val appType = determineAppType(
            detectedFrameworks,
            hasFlutterAssets,
            hasReactNativeAssets,
            hasXamarinAssets,
            hasCordovaAssets,
            hasUnityAssets
        )
        
        val confidence = calculateConfidence(appType, detectedFrameworks.size, usedLibraries.size)
        
        return AppAnalysis(
            appInfo = appInfo.copy(appType = appType),
            detectedFrameworks = detectedFrameworks,
            nativeLibraries = nativeLibraries,
            usedLibraries = usedLibraries,
            permissions = permissions,
            hasFlutterAssets = hasFlutterAssets,
            hasReactNativeAssets = hasReactNativeAssets,
            hasXamarinAssets = hasXamarinAssets,
            hasCordovaAssets = hasCordovaAssets,
            hasUnityAssets = hasUnityAssets,
            analysisConfidence = confidence
        )
    }
    
    private data class ApkAnalysis(
        val hasFlutterAssets: Boolean = false,
        val hasReactNativeAssets: Boolean = false,
        val hasXamarinAssets: Boolean = false,
        val hasCordovaAssets: Boolean = false,
        val hasUnityAssets: Boolean = false
    )
    
    private fun analyzeApkContents(
        apkPath: String?,
        detectedFrameworks: MutableList<String>,
        nativeLibraries: MutableList<String>,
        usedLibraries: MutableList<String>,
        appType: AppType
    ): ApkAnalysis {
        if (apkPath == null) {
            return ApkAnalysis()
        }
        var hasFlutterAssets = false
        var hasReactNativeAssets = false
        var hasXamarinAssets = false
        var hasCordovaAssets = false
        var hasUnityAssets = false
        
        try {
            ZipFile(apkPath).use { zipFile ->
                val entries = zipFile.entries()
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name
                    
                    // Check for Flutter
                    if (entryName.contains("flutter_assets") || 
                        entryName.contains("libflutter.so") ||
                        entryName.contains("isolate_snapshot_data") ||
                        entryName.contains("kernel_blob.bin")) {
                        hasFlutterAssets = true
                        if (!detectedFrameworks.contains("Flutter")) {
                            detectedFrameworks.add("Flutter")
                        }
                    }
                    
                    // Check for React Native
                    if (entryName.contains("assets/index.android.bundle") ||
                        entryName.contains("libreactnativejni.so") ||
                        entryName.contains("libhermes.so") ||
                        entryName.contains("assets/index.bundle")) {
                        hasReactNativeAssets = true
                        if (!detectedFrameworks.contains("React Native")) {
                            detectedFrameworks.add("React Native")
                        }
                    }
                    
                    // Check for Xamarin
                    if (entryName.contains("assemblies/") ||
                        entryName.contains("libmonodroid.so") ||
                        entryName.contains("libxamarin-app.so")) {
                        hasXamarinAssets = true
                        if (!detectedFrameworks.contains("Xamarin")) {
                            detectedFrameworks.add("Xamarin")
                        }
                    }
                    
                    // Check for Cordova/PhoneGap
                    if (entryName.contains("assets/www/") ||
                        entryName.contains("cordova.js") ||
                        entryName.contains("phonegap.js")) {
                        hasCordovaAssets = true
                        if (!detectedFrameworks.contains("Cordova/PhoneGap")) {
                            detectedFrameworks.add("Cordova/PhoneGap")
                        }
                    }
                    
                    // Check for Unity
                    if (entryName.contains("libunity.so") ||
                        entryName.contains("assets/bin/Data/") ||
                        entryName.contains("libmono.so")) {
                        hasUnityAssets = true
                        if (!detectedFrameworks.contains("Unity")) {
                            detectedFrameworks.add("Unity")
                        }
                    }
                    
                    // Collect native libraries
                    if (entryName.startsWith("lib/") && entryName.endsWith(".so")) {
                        val libName = entryName.substringAfterLast("/")
                        if (!nativeLibraries.contains(libName)) {
                            nativeLibraries.add(libName)
                        }
                    }
                    
                    // Analyze libraries based on app type
                    analyzeLibrariesByType(entryName, appType, usedLibraries)
                }
            }
        } catch (e: Exception) {
            // Handle APK analysis errors
        }
        
        return ApkAnalysis(
            hasFlutterAssets,
            hasReactNativeAssets,
            hasXamarinAssets,
            hasCordovaAssets,
            hasUnityAssets
        )
    }
    
    private fun analyzeLibrariesByType(
        entryName: String,
        appType: AppType,
        usedLibraries: MutableList<String>
    ) {
        when (appType) {
            AppType.FLUTTER -> analyzeFlutterLibraries(entryName, usedLibraries)
            AppType.REACT_NATIVE -> analyzeReactNativeLibraries(entryName, usedLibraries)
            AppType.NATIVE_ANDROID -> analyzeAndroidLibraries(entryName, usedLibraries)
            AppType.XAMARIN -> analyzeXamarinLibraries(entryName, usedLibraries)
            AppType.CORDOVA -> analyzeCordovaLibraries(entryName, usedLibraries)
            AppType.UNITY -> analyzeUnityLibraries(entryName, usedLibraries)
            AppType.KMP -> analyzeKmpLibraries(entryName, usedLibraries)
            else -> analyzeAndroidLibraries(entryName, usedLibraries)
        }
    }
    
    private fun analyzeFlutterLibraries(entryName: String, usedLibraries: MutableList<String>) {
        val flutterLibraries = mapOf(
            "http" to "assets/packages/http/",
            "shared_preferences" to "assets/packages/shared_preferences/",
            "path_provider" to "assets/packages/path_provider/",
            "sqflite" to "assets/packages/sqflite/",
            "camera" to "assets/packages/camera/",
            "image_picker" to "assets/packages/image_picker/",
            "url_launcher" to "assets/packages/url_launcher/",
            "webview_flutter" to "assets/packages/webview_flutter/",
            "firebase_core" to "assets/packages/firebase_core/",
            "firebase_auth" to "assets/packages/firebase_auth/",
            "cloud_firestore" to "assets/packages/cloud_firestore/",
            "provider" to "assets/packages/provider/",
            "bloc" to "assets/packages/bloc/",
            "dio" to "assets/packages/dio/",
            "get" to "assets/packages/get/",
            "flutter_riverpod" to "assets/packages/flutter_riverpod/",
            "go_router" to "assets/packages/go_router/"
        )
        
        flutterLibraries.forEach { (libName, pattern) ->
            if (entryName.contains(pattern) && !usedLibraries.contains(libName)) {
                usedLibraries.add(libName)
            }
        }
    }
    
    private fun analyzeReactNativeLibraries(entryName: String, usedLibraries: MutableList<String>) {
        val rnLibraries = listOf(
            "react-navigation", "react-redux", "redux-toolkit", "@reduxjs/toolkit",
            "react-native-vector-icons", "react-native-gesture-handler", 
            "react-native-reanimated", "react-native-screens", "react-native-safe-area-context",
            "react-native-async-storage", "react-native-camera", "react-native-image-picker",
            "react-native-webview", "react-native-maps", "react-native-firebase",
            "@react-native-firebase", "react-native-push-notification"
        )
        
        if (entryName.contains("assets/index.android.bundle") || entryName.contains("assets/index.bundle")) {
            // For React Native, we would need to analyze the bundle content
            // This is a simplified approach
            rnLibraries.forEach { lib ->
                if (!usedLibraries.contains(lib)) {
                    usedLibraries.add(lib)
                }
            }
        }
    }
    
    private fun analyzeAndroidLibraries(entryName: String, usedLibraries: MutableList<String>) {
        val androidLibraries = mapOf(
            "androidx.lifecycle" to "androidx/lifecycle/",
            "androidx.navigation" to "androidx/navigation/",
            "androidx.room" to "androidx/room/",
            "androidx.work" to "androidx/work/",
            "androidx.compose" to "androidx/compose/",
            "androidx.camera" to "androidx/camera/",
            "androidx.biometric" to "androidx/biometric/",
            "retrofit2" to "retrofit2/",
            "okhttp3" to "okhttp3/",
            "gson" to "com/google/gson/",
            "glide" to "com/bumptech/glide/",
            "picasso" to "com/squareup/picasso/",
            "dagger" to "dagger/",
            "hilt" to "dagger/hilt/",
            "rxjava" to "io/reactivex/"
        )
        
        androidLibraries.forEach { (libName, pattern) ->
            if (entryName.contains(pattern) && !usedLibraries.contains(libName)) {
                usedLibraries.add(libName)
            }
        }
    }
    
    private fun analyzeXamarinLibraries(entryName: String, usedLibraries: MutableList<String>) {
        if (entryName.startsWith("assemblies/")) {
            val assemblyName = entryName.substringAfterLast("/").substringBeforeLast(".")
            if (assemblyName.isNotEmpty() && !usedLibraries.contains(assemblyName)) {
                usedLibraries.add(assemblyName)
            }
        }
    }
    
    private fun analyzeCordovaLibraries(entryName: String, usedLibraries: MutableList<String>) {
        val cordovaPlugins = listOf(
            "cordova-plugin-camera", "cordova-plugin-file", "cordova-plugin-geolocation",
            "cordova-plugin-device", "cordova-plugin-network-information", "cordova-plugin-battery-status",
            "cordova-plugin-vibration", "cordova-plugin-statusbar", "cordova-plugin-splashscreen"
        )
        
        if (entryName.contains("assets/www/plugins/")) {
            cordovaPlugins.forEach { plugin ->
                if (entryName.contains(plugin) && !usedLibraries.contains(plugin)) {
                    usedLibraries.add(plugin)
                }
            }
        }
    }
    
    private fun analyzeUnityLibraries(entryName: String, usedLibraries: MutableList<String>) {
        if (entryName.contains("assets/bin/Data/Managed/")) {
            val libraryName = entryName.substringAfterLast("/").substringBeforeLast(".")
            if (libraryName.isNotEmpty() && !usedLibraries.contains(libraryName)) {
                usedLibraries.add(libraryName)
            }
        }
    }
    
    private fun analyzeKmpLibraries(entryName: String, usedLibraries: MutableList<String>) {
        val kmpLibraries = listOf(
            "kotlinx.coroutines", "kotlinx.serialization", "ktor", "sqldelight",
            "koin", "kodein", "multiplatform-settings", "kermit"
        )
        
        kmpLibraries.forEach { lib ->
            if (entryName.contains(lib.replace(".", "/")) && !usedLibraries.contains(lib)) {
                usedLibraries.add(lib)
            }
        }
    }
    
    private fun determineAppType(
        detectedFrameworks: List<String>,
        hasFlutterAssets: Boolean,
        hasReactNativeAssets: Boolean,
        hasXamarinAssets: Boolean,
        hasCordovaAssets: Boolean,
        hasUnityAssets: Boolean
    ): AppType {
        return when {
            hasFlutterAssets || detectedFrameworks.contains("Flutter") -> AppType.FLUTTER
            hasReactNativeAssets || detectedFrameworks.contains("React Native") -> AppType.REACT_NATIVE
            hasXamarinAssets || detectedFrameworks.contains("Xamarin") -> AppType.XAMARIN
            hasCordovaAssets || detectedFrameworks.contains("Cordova/PhoneGap") -> AppType.CORDOVA
            hasUnityAssets || detectedFrameworks.contains("Unity") -> AppType.UNITY
            else -> AppType.NATIVE_ANDROID
        }
    }
    
    private fun calculateConfidence(appType: AppType, frameworkCount: Int, libraryCount: Int): Float {
        return when {
            appType != AppType.UNKNOWN && frameworkCount > 0 -> 0.9f
            appType != AppType.UNKNOWN -> 0.7f
            libraryCount > 0 -> 0.5f
            else -> 0.2f
        }
    }
}