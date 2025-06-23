package kr.bluevisor.applens.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.bluevisor.applens.analyzer.AppAnalyzer
import kr.bluevisor.applens.model.AppAnalysis
import kr.bluevisor.applens.model.AppInfo

class AppDetailViewModel : ViewModel() {

    private val _analysis = MutableStateFlow<AppAnalysis?>(null)
    val analysis: StateFlow<AppAnalysis?> = _analysis.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun analyzeApp(context: Context, appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            try {
                val analyzer = AppAnalyzer(context)
                val result = analyzer.analyzeApp(appInfo)
                _analysis.value = result
            } catch (e: Exception) {
                _analysis.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}