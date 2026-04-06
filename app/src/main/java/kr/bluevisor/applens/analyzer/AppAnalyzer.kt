package kr.bluevisor.applens.analyzer

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import kr.bluevisor.applens.BuildConfig
import kr.bluevisor.applens.model.AppAnalysis
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.FrameworkType
import kr.bluevisor.applens.model.ObservedArtifact
import kr.bluevisor.applens.model.PermissionDetails
import kr.bluevisor.applens.model.VisibilityScope
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
            runCatching { createAppInfo(packageInfo) }.getOrNull()
        }.sortedBy { it.appName.lowercase() }
    }

    fun analyzeApp(appInfo: AppInfo): AppAnalysis {
        val packageInfo = getPackageInfo(appInfo.packageName)
        val applicationInfo = packageInfo.applicationInfo ?: error("Missing ApplicationInfo")
        val apkSources = getApkPaths(applicationInfo).mapNotNull(::readApkSource)
        val scanAnalysis = ArtifactAnalyzer.analyze(apkSources)

        return AppAnalysis(
            appInfo = appInfo,
            framework = scanAnalysis.framework,
            frameworkEvidence = scanAnalysis.frameworkEvidence,
            detectedLibraries = scanAnalysis.detectedLibraries,
            nativeLibraries = scanAnalysis.nativeLibraries,
            permissions = ArtifactAnalyzer.mapPermissions(buildPermissionInputs(packageInfo)),
            visibilityScope = if (BuildConfig.IS_PACKAGE_VISIBILITY_LIMITED) {
                VisibilityScope.FILTERED
            } else {
                VisibilityScope.FULL_DEVICE
            },
            analyzedApkCount = scanAnalysis.analyzedApkCount
        )
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    }

    private fun createAppInfo(packageInfo: PackageInfo): AppInfo {
        val applicationInfo = packageInfo.applicationInfo ?: error("Missing ApplicationInfo")
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        val icon = runCatching { packageManager.getApplicationIcon(applicationInfo) }.getOrNull()

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
            appSize = getPackagedSize(applicationInfo),
            isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            targetSdkVersion = applicationInfo.targetSdkVersion,
            minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                applicationInfo.minSdkVersion
            } else {
                1
            }
        )
    }

    private fun getPackagedSize(applicationInfo: ApplicationInfo): Long {
        return getApkPaths(applicationInfo).sumOf { path ->
            runCatching { File(path).length() }.getOrDefault(0L)
        }
    }

    private fun getApkPaths(applicationInfo: ApplicationInfo): List<String> {
        return buildList {
            applicationInfo.publicSourceDir?.let(::add)
            applicationInfo.splitPublicSourceDirs?.forEach(::add)
        }.filter { it.isNotBlank() }.distinct()
    }

    private fun readApkSource(apkPath: String): ApkSource? {
        return runCatching {
            ZipFile(apkPath).use { zipFile ->
                ApkSource(
                    sourceApkOrSplit = File(apkPath).name,
                    entryNames = zipFile.entries().asSequence().map { it.name }.toList()
                )
            }
        }.getOrNull()
    }

    private fun buildPermissionInputs(packageInfo: PackageInfo): List<RequestedPermissionInput> {
        val requestedPermissions = packageInfo.requestedPermissions.orEmpty()
        val flags = packageInfo.requestedPermissionsFlags ?: IntArray(requestedPermissions.size)

        return requestedPermissions.mapIndexed { index, permissionName ->
            RequestedPermissionInput(
                name = permissionName,
                flags = flags.getOrElse(index) { 0 },
                protectionLevel = resolveProtectionLevel(permissionName)
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveProtectionLevel(permissionName: String): String {
        return runCatching {
            val permissionInfo = packageManager.getPermissionInfo(permissionName, 0)
            when (permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) {
                PermissionInfo.PROTECTION_NORMAL -> "normal"
                PermissionInfo.PROTECTION_DANGEROUS -> "dangerous"
                PermissionInfo.PROTECTION_SIGNATURE -> "signature"
                else -> "other"
            }
        }.getOrDefault("unknown")
    }
}

internal data class ApkSource(
    val sourceApkOrSplit: String,
    val entryNames: List<String>
)

internal data class RequestedPermissionInput(
    val name: String,
    val flags: Int,
    val protectionLevel: String
)

internal data class ScanAnalysis(
    val framework: FrameworkType,
    val frameworkEvidence: List<ObservedArtifact>,
    val detectedLibraries: List<ObservedArtifact>,
    val nativeLibraries: List<ObservedArtifact>,
    val analyzedApkCount: Int
)

internal object ArtifactAnalyzer {
    private val flutterPackageRegex =
        Regex("""(?:^|/)flutter_assets/packages/([^/]+)/""")
    private val cordovaPluginRegex =
        Regex("""assets/www/plugins/([^/]+)/""")
    private val unityManagedRegex =
        Regex("""assets/bin/Data/Managed/([^/]+)\.(dll|winmd)$""")
    private val xamarinAssemblyRegex =
        Regex("""assemblies/([^/]+)\.(dll|exe)$""")
    private val nativeLibraryRegex =
        Regex("""^lib/([^/]+)/([^/]+\.so)$""")
    private val metaInfLibraryRegex =
        Regex("""^META-INF/(?:com/)?([^/]+)/""")

    private val frameworkPriority = listOf(
        FrameworkType.FLUTTER,
        FrameworkType.REACT_NATIVE,
        FrameworkType.XAMARIN,
        FrameworkType.CORDOVA,
        FrameworkType.UNITY
    )

    private val reactNativePatterns = linkedMapOf(
        "react-navigation" to listOf("react-navigation"),
        "react-redux" to listOf("react-redux"),
        "redux-toolkit" to listOf("@reduxjs/toolkit"),
        "react-native-vector-icons" to listOf("react-native-vector-icons"),
        "react-native-gesture-handler" to listOf("react-native-gesture-handler"),
        "react-native-reanimated" to listOf("react-native-reanimated"),
        "react-native-screens" to listOf("react-native-screens"),
        "react-native-safe-area-context" to listOf("react-native-safe-area-context"),
        "react-native-async-storage" to listOf("@react-native-async-storage"),
        "react-native-camera" to listOf("react-native-camera"),
        "react-native-image-picker" to listOf("react-native-image-picker"),
        "react-native-webview" to listOf("react-native-webview"),
        "react-native-maps" to listOf("react-native-maps"),
        "react-native-firebase" to listOf("@react-native-firebase"),
        "react-native-push-notification" to listOf("react-native-push-notification"),
        "react-native-linear-gradient" to listOf("react-native-linear-gradient"),
        "react-native-svg" to listOf("react-native-svg"),
        "lottie-react-native" to listOf("lottie-react-native")
    )

    private val reactNativePrefixes = listOf(
        "assets/node_modules/",
        "assets/src/",
        "assets/index.android.bundle/",
        "assets/index.bundle/"
    )

    private val packagedLibrariesByMetaInfPrefix = linkedMapOf(
        "androidx.lifecycle" to "androidx.lifecycle",
        "androidx.navigation" to "androidx.navigation",
        "androidx.room" to "androidx.room",
        "androidx.work" to "androidx.work",
        "androidx.compose" to "androidx.compose",
        "androidx.camera" to "androidx.camera",
        "androidx.biometric" to "androidx.biometric",
        "retrofit" to "retrofit2",
        "okhttp" to "okhttp3",
        "gson" to "gson",
        "glide" to "glide",
        "picasso" to "picasso",
        "dagger" to "dagger",
        "rxjava" to "rxjava"
    )

    private val packagedLibrariesByNativeLibPrefix = linkedMapOf(
        "libktor" to "ktor",
        "libsqliter" to "androidx.room",
        "libokhttp" to "okhttp3"
    )

    fun analyze(sources: List<ApkSource>): ScanAnalysis {
        val frameworkEvidence = frameworkPriority.associateWith { linkedMapOf<String, ObservedArtifact>() }
        val detectedLibraries = linkedMapOf<String, ObservedArtifact>()
        val nativeLibraries = linkedMapOf<String, ObservedArtifact>()

        sources.forEach { source ->
            source.entryNames.forEach { entryName ->
                collectFrameworkEvidence(frameworkEvidence, source.sourceApkOrSplit, entryName)
                collectDetectedLibraries(detectedLibraries, source.sourceApkOrSplit, entryName)
                collectNativeLibraries(nativeLibraries, source.sourceApkOrSplit, entryName)
            }
        }

        val framework = frameworkPriority.maxByOrNull { framework ->
            frameworkEvidence.getValue(framework).size * 100 + (frameworkPriority.size - frameworkPriority.indexOf(framework))
        }?.takeIf { frameworkEvidence.getValue(it).isNotEmpty() } ?: FrameworkType.UNCLASSIFIED

        return ScanAnalysis(
            framework = framework,
            frameworkEvidence = frameworkEvidence[framework]?.values?.toList().orEmpty(),
            detectedLibraries = detectedLibraries.values.toList(),
            nativeLibraries = nativeLibraries.values.toList(),
            analyzedApkCount = sources.size
        )
    }

    fun mapPermissions(inputs: List<RequestedPermissionInput>): List<PermissionDetails> {
        return inputs.map { input ->
            PermissionDetails(
                name = input.name,
                granted = input.flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0,
                implicit = input.flags and PackageInfo.REQUESTED_PERMISSION_IMPLICIT != 0,
                neverForLocation = input.flags and PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION != 0,
                protectionLevel = input.protectionLevel
            )
        }.sortedBy { it.name }
    }

    private fun collectFrameworkEvidence(
        frameworkEvidence: Map<FrameworkType, MutableMap<String, ObservedArtifact>>,
        sourceApkOrSplit: String,
        entryName: String
    ) {
        when {
            entryName.contains("assets/flutter_assets/") ||
                    entryName.contains("flutter_assets/") ||
                    entryName.endsWith("libflutter.so") ||
                    entryName.contains("isolate_snapshot_data") ||
                    entryName.contains("kernel_blob.bin") -> {
                addFrameworkEvidence(
                    frameworkEvidence,
                    FrameworkType.FLUTTER,
                    sourceApkOrSplit,
                    entryName,
                    entryName.substringAfterLast("/")
                )
            }

            entryName.contains("assets/index.android.bundle") ||
                    entryName.contains("assets/index.bundle") ||
                    entryName.endsWith("libreactnativejni.so") ||
                    entryName.endsWith("libhermes.so") -> {
                addFrameworkEvidence(
                    frameworkEvidence,
                    FrameworkType.REACT_NATIVE,
                    sourceApkOrSplit,
                    entryName,
                    entryName.substringAfterLast("/")
                )
            }

            entryName.contains("assemblies/") ||
                    entryName.endsWith("libmonodroid.so") ||
                    entryName.endsWith("libxamarin-app.so") -> {
                addFrameworkEvidence(
                    frameworkEvidence,
                    FrameworkType.XAMARIN,
                    sourceApkOrSplit,
                    entryName,
                    entryName.substringAfterLast("/")
                )
            }

            entryName.contains("assets/www/") ||
                    entryName.endsWith("cordova.js") ||
                    entryName.endsWith("phonegap.js") -> {
                addFrameworkEvidence(
                    frameworkEvidence,
                    FrameworkType.CORDOVA,
                    sourceApkOrSplit,
                    entryName,
                    entryName.substringAfterLast("/")
                )
            }

            entryName.endsWith("libunity.so") ||
                    entryName.endsWith("libil2cpp.so") ||
                    entryName.contains("assets/bin/Data/") -> {
                addFrameworkEvidence(
                    frameworkEvidence,
                    FrameworkType.UNITY,
                    sourceApkOrSplit,
                    entryName,
                    entryName.substringAfterLast("/")
                )
            }
        }
    }

    private fun collectDetectedLibraries(
        detectedLibraries: MutableMap<String, ObservedArtifact>,
        sourceApkOrSplit: String,
        entryName: String
    ) {
        flutterPackageRegex.find(entryName)?.groupValues?.get(1)?.let { packageName ->
            putArtifactIfMissing(
                detectedLibraries,
                key = "flutter:$packageName",
                artifact = ObservedArtifact(
                    name = packageName,
                    matchedPath = entryName,
                    sourceApkOrSplit = sourceApkOrSplit
                )
            )
        }

        xamarinAssemblyRegex.find(entryName)?.groupValues?.get(1)?.let { assemblyName ->
            putArtifactIfMissing(
                detectedLibraries,
                key = "xamarin:$assemblyName",
                artifact = ObservedArtifact(
                    name = assemblyName,
                    matchedPath = entryName,
                    sourceApkOrSplit = sourceApkOrSplit
                )
            )
        }

        cordovaPluginRegex.find(entryName)?.groupValues?.get(1)?.let { pluginName ->
            putArtifactIfMissing(
                detectedLibraries,
                key = "cordova:$pluginName",
                artifact = ObservedArtifact(
                    name = pluginName,
                    matchedPath = entryName,
                    sourceApkOrSplit = sourceApkOrSplit
                )
            )
        }

        unityManagedRegex.find(entryName)?.groupValues?.get(1)?.let { libraryName ->
            putArtifactIfMissing(
                detectedLibraries,
                key = "unity:$libraryName",
                artifact = ObservedArtifact(
                    name = libraryName,
                    matchedPath = entryName,
                    sourceApkOrSplit = sourceApkOrSplit
                )
            )
        }

        reactNativePatterns.forEach { (name, patterns) ->
            if (hasReactNativeArtifactMatch(entryName, patterns)) {
                putArtifactIfMissing(
                    detectedLibraries,
                    key = "rn:$name",
                    artifact = ObservedArtifact(
                        name = name,
                        matchedPath = entryName,
                        sourceApkOrSplit = sourceApkOrSplit
                    )
                )
            }
        }

        resolvePackagedLibraryName(entryName)?.let { libraryName ->
            putArtifactIfMissing(
                detectedLibraries,
                key = "packaged:$libraryName",
                artifact = ObservedArtifact(
                    name = libraryName,
                    matchedPath = entryName,
                    sourceApkOrSplit = sourceApkOrSplit
                )
            )
        }
    }

    private fun collectNativeLibraries(
        nativeLibraries: MutableMap<String, ObservedArtifact>,
        sourceApkOrSplit: String,
        entryName: String
    ) {
        val match = nativeLibraryRegex.find(entryName) ?: return
        val abi = match.groupValues[1]
        val libraryName = match.groupValues[2]

        putArtifactIfMissing(
            nativeLibraries,
            key = "$libraryName:$abi",
            artifact = ObservedArtifact(
                name = libraryName,
                matchedPath = entryName,
                sourceApkOrSplit = sourceApkOrSplit,
                abi = abi
            )
        )
    }

    private fun addFrameworkEvidence(
        frameworkEvidence: Map<FrameworkType, MutableMap<String, ObservedArtifact>>,
        framework: FrameworkType,
        sourceApkOrSplit: String,
        entryName: String,
        signalName: String
    ) {
        putArtifactIfMissing(
            frameworkEvidence.getValue(framework),
            key = "$signalName:$entryName",
            artifact = ObservedArtifact(
                name = signalName,
                matchedPath = entryName,
                sourceApkOrSplit = sourceApkOrSplit
            )
        )
    }

    private fun putArtifactIfMissing(
        target: MutableMap<String, ObservedArtifact>,
        key: String,
        artifact: ObservedArtifact
    ) {
        if (key !in target) {
            target[key] = artifact
        }
    }

    private fun hasReactNativeArtifactMatch(
        entryName: String,
        patterns: List<String>
    ): Boolean {
        if (entryName.startsWith("lib/") && entryName.endsWith(".so")) {
            val normalizedName = entryName.substringAfterLast("/").lowercase()
            return patterns.any { pattern ->
                val token = pattern.substringAfterLast("/").lowercase()
                normalizedName.contains(token.replace("-", "").replace("_", ""))
            }
        }

        return reactNativePrefixes.any { prefix -> entryName.startsWith(prefix) } &&
            patterns.any { pattern ->
                entryName.contains("/$pattern/") ||
                    entryName.endsWith("/$pattern") ||
                    entryName.contains("/${pattern.replace("-", "_")}/")
            }
    }

    private fun resolvePackagedLibraryName(entryName: String): String? {
        if (entryName.startsWith("META-INF/")) {
            val prefix = metaInfLibraryRegex.find(entryName)?.groupValues?.getOrNull(1) ?: return null
            return packagedLibrariesByMetaInfPrefix[prefix]
        }

        if (entryName.startsWith("lib/") && entryName.endsWith(".so")) {
            val libraryName = entryName.substringAfterLast("/").substringBeforeLast(".so")
            return packagedLibrariesByNativeLibPrefix.entries
                .firstOrNull { (prefix, _) -> libraryName.startsWith(prefix) }
                ?.value
        }

        return when {
            entryName.startsWith("kotlinx/coroutines/") -> "kotlinx.coroutines"
            entryName.startsWith("kotlinx/serialization/") -> "kotlinx.serialization"
            entryName.startsWith("io/ktor/") -> "ktor"
            else -> null
        }
    }
}
