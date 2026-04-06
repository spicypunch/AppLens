package kr.bluevisor.applens.ui.presentation

import kr.bluevisor.applens.model.AnalysisStatus
import kr.bluevisor.applens.model.AppAnalysisState
import kr.bluevisor.applens.model.FrameworkType

fun FrameworkType.displayLabel(): String {
    return when (this) {
        FrameworkType.FLUTTER -> "Flutter"
        FrameworkType.REACT_NATIVE -> "React Native"
        FrameworkType.XAMARIN -> "Xamarin"
        FrameworkType.CORDOVA -> "Cordova"
        FrameworkType.UNITY -> "Unity"
        FrameworkType.UNCLASSIFIED -> "Android"
    }
}

fun listChipLabelFor(state: AppAnalysisState?): String? {
    return when (state?.status) {
        AnalysisStatus.ANALYZING -> "Analyzing"
        AnalysisStatus.READY -> state.analysis?.framework?.displayLabel()
        else -> null
    }
}
