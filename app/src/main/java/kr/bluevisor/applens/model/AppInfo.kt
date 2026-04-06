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
    val minSdkVersion: Int
)

fun AppInfo.analysisCacheKey(): String {
    return "$packageName:$versionCode:$updateTime"
}

enum class AnalysisStatus {
    NOT_ANALYZED,
    ANALYZING,
    READY,
    FAILED
}

enum class FrameworkType {
    FLUTTER,
    REACT_NATIVE,
    XAMARIN,
    CORDOVA,
    UNITY,
    UNCLASSIFIED
}

enum class VisibilityScope {
    FULL_DEVICE,
    FILTERED
}

data class ObservedArtifact(
    val name: String,
    val matchedPath: String,
    val sourceApkOrSplit: String,
    val abi: String? = null
)

data class PermissionDetails(
    val name: String,
    val granted: Boolean,
    val implicit: Boolean,
    val neverForLocation: Boolean,
    val protectionLevel: String
)

data class AppAnalysis(
    val appInfo: AppInfo,
    val framework: FrameworkType,
    val frameworkEvidence: List<ObservedArtifact>,
    val detectedLibraries: List<ObservedArtifact>,
    val nativeLibraries: List<ObservedArtifact>,
    val permissions: List<PermissionDetails>,
    val visibilityScope: VisibilityScope,
    val analyzedApkCount: Int
)

data class AppAnalysisState(
    val status: AnalysisStatus,
    val analysis: AppAnalysis? = null,
    val errorMessage: String? = null
)
