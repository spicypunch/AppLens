package kr.bluevisor.applens.ui.presentation

import kr.bluevisor.applens.model.AnalysisStatus
import kr.bluevisor.applens.model.AppAnalysis
import kr.bluevisor.applens.model.AppAnalysisState
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.FrameworkType
import kr.bluevisor.applens.model.VisibilityScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisPresentationTest {

    @Test
    fun `list chip stays hidden before analysis and shows android afterwards`() {
        val notAnalyzed = AppAnalysisState(status = AnalysisStatus.NOT_ANALYZED)
        val ready = AppAnalysisState(
            status = AnalysisStatus.READY,
            analysis = unclassifiedAnalysis()
        )

        assertNull(listChipLabelFor(notAnalyzed))
        assertEquals("Android", listChipLabelFor(ready))
    }

    private fun unclassifiedAnalysis(): AppAnalysis {
        return AppAnalysis(
            appInfo = AppInfo(
                packageName = "example",
                appName = "Example",
                icon = null,
                versionName = "1.0",
                versionCode = 1,
                installTime = 0L,
                updateTime = 0L,
                appSize = 0L,
                isSystemApp = false,
                targetSdkVersion = 35,
                minSdkVersion = 24
            ),
            framework = FrameworkType.UNCLASSIFIED,
            frameworkEvidence = emptyList(),
            detectedLibraries = emptyList(),
            nativeLibraries = emptyList(),
            permissions = emptyList(),
            visibilityScope = VisibilityScope.FILTERED,
            analyzedApkCount = 1
        )
    }
}
