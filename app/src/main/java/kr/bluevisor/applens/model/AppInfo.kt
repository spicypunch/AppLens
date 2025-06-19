package kr.bluevisor.applens.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val appSize: Long,
    val isSystemApp: Boolean,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val appType: AppType = AppType.UNKNOWN
)

enum class AppType {
    FLUTTER,
    REACT_NATIVE,
    XAMARIN,
    CORDOVA,
    IONIC,
    NATIVE_ANDROID,
    KMP,
    UNITY,
    UNKNOWN
}

data class AppAnalysis(
    val appInfo: AppInfo,
    val detectedFrameworks: List<String>,
    val nativeLibraries: List<String>,
    val usedLibraries: List<String>,
    val permissions: List<String>,
    val hasFlutterAssets: Boolean,
    val hasReactNativeAssets: Boolean,
    val hasXamarinAssets: Boolean,
    val hasCordovaAssets: Boolean,
    val hasUnityAssets: Boolean,
    val analysisConfidence: Float
)