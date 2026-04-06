package kr.bluevisor.applens.analyzer

import android.content.pm.PackageInfo
import kr.bluevisor.applens.model.FrameworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactAnalyzerTest {

    @Test
    fun `detects flutter framework and asset backed packages`() {
        val analysis = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf(
                        "assets/flutter_assets/isolate_snapshot_data",
                        "assets/flutter_assets/packages/http/certs/cert.pem",
                        "assets/flutter_assets/packages/provider/images/icon.png",
                        "lib/arm64-v8a/libflutter.so"
                    )
                )
            )
        )

        assertEquals(FrameworkType.FLUTTER, analysis.framework)
        assertEquals(listOf("http", "provider"), analysis.detectedLibraries.map { it.name })
        assertTrue(analysis.frameworkEvidence.isNotEmpty())
    }

    @Test
    fun `detects react native from packaged artifacts`() {
        val analysis = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf(
                        "assets/index.android.bundle",
                        "lib/arm64-v8a/libreactnativejni.so",
                        "assets/node_modules/react-native-svg/index.js"
                    )
                )
            )
        )

        assertEquals(FrameworkType.REACT_NATIVE, analysis.framework)
        assertTrue(analysis.detectedLibraries.any { it.name == "react-native-svg" })
    }

    @Test
    fun `avoids false positives from loose substring matches`() {
        val analysis = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf(
                        "assets/text/react-navigation-notes.txt",
                        "assets/docs/okhttp-overview.md",
                        "assets/misc/flutter_assets_backup/readme.txt"
                    )
                )
            )
        )

        assertTrue(analysis.detectedLibraries.isEmpty())
        assertEquals(FrameworkType.UNCLASSIFIED, analysis.framework)
    }

    @Test
    fun `detects xamarin cordova and unity framework evidence`() {
        val xamarin = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf("assemblies/Xamarin.Forms.dll", "lib/arm64-v8a/libmonodroid.so")
                )
            )
        )
        val cordova = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf("assets/www/plugins/cordova-plugin-camera/www/Camera.js")
                )
            )
        )
        val unity = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf("assets/bin/Data/Managed/UnityEngine.dll", "lib/arm64-v8a/libunity.so")
                )
            )
        )

        assertEquals(FrameworkType.XAMARIN, xamarin.framework)
        assertEquals(FrameworkType.CORDOVA, cordova.framework)
        assertEquals(FrameworkType.UNITY, unity.framework)
        assertTrue(cordova.detectedLibraries.any { it.name == "cordova-plugin-camera" })
        assertTrue(unity.detectedLibraries.any { it.name == "UnityEngine" })
    }

    @Test
    fun `returns unclassified when no supported framework evidence exists`() {
        val analysis = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf("AndroidManifest.xml", "classes.dex")
                )
            )
        )

        assertEquals(FrameworkType.UNCLASSIFIED, analysis.framework)
        assertTrue(analysis.frameworkEvidence.isEmpty())
    }

    @Test
    fun `aggregates split apk native libraries by soname and abi`() {
        val analysis = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf("lib/arm64-v8a/libfoo.so", "lib/arm64-v8a/libbar.so")
                ),
                ApkSource(
                    sourceApkOrSplit = "split_config.arm64_v8a.apk",
                    entryNames = listOf("lib/arm64-v8a/libfoo.so")
                ),
                ApkSource(
                    sourceApkOrSplit = "split_config.armeabi_v7a.apk",
                    entryNames = listOf("lib/armeabi-v7a/libfoo.so")
                )
            )
        )

        assertEquals(3, analysis.nativeLibraries.size)
        assertTrue(analysis.nativeLibraries.any { it.name == "libfoo.so" && it.abi == "arm64-v8a" })
        assertTrue(analysis.nativeLibraries.any { it.name == "libfoo.so" && it.abi == "armeabi-v7a" })
    }

    @Test
    fun `maps permission flags without losing manifest details`() {
        val permissions = ArtifactAnalyzer.mapPermissions(
            listOf(
                RequestedPermissionInput(
                    name = "android.permission.CAMERA",
                    flags = PackageInfo.REQUESTED_PERMISSION_GRANTED or
                        PackageInfo.REQUESTED_PERMISSION_IMPLICIT or
                        PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION,
                    protectionLevel = "dangerous"
                ),
                RequestedPermissionInput(
                    name = "com.example.CUSTOM",
                    flags = 0,
                    protectionLevel = "unknown"
                )
            )
        )

        val camera = permissions.first { it.name == "android.permission.CAMERA" }
        val custom = permissions.first { it.name == "com.example.CUSTOM" }

        assertTrue(camera.granted)
        assertTrue(camera.implicit)
        assertTrue(camera.neverForLocation)
        assertEquals("dangerous", camera.protectionLevel)
        assertFalse(custom.granted)
        assertEquals("unknown", custom.protectionLevel)
    }

    @Test
    fun `does not inject synthetic libraries without evidence`() {
        val analysis = ArtifactAnalyzer.analyze(
            listOf(
                ApkSource(
                    sourceApkOrSplit = "base.apk",
                    entryNames = listOf("assets/bin/placeholder.txt")
                )
            )
        )

        assertTrue(analysis.detectedLibraries.isEmpty())
    }
}
