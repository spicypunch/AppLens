package kr.bluevisor.applens.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.bluevisor.applens.BuildConfig
import kr.bluevisor.applens.analyzer.AppAnalyzer
import kr.bluevisor.applens.model.AnalysisStatus
import kr.bluevisor.applens.model.AppAnalysisState
import kr.bluevisor.applens.model.AppFilter
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.FilterState
import kr.bluevisor.applens.model.SortOrder
import kr.bluevisor.applens.model.analysisCacheKey

class AppLensViewModel(application: Application) : AndroidViewModel(application) {

    private val analyzer = AppAnalyzer(application)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _analysisStates = MutableStateFlow<Map<String, AppAnalysisState>>(emptyMap())
    val analysisStates: StateFlow<Map<String, AppAnalysisState>> = _analysisStates.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _scrollPosition = MutableStateFlow(0)
    val scrollPosition: StateFlow<Int> = _scrollPosition.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = combine(
        _allApps,
        _filterState
    ) { allApps, filterState ->
        filterAndSortApps(allApps, filterState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isPackageVisibilityLimited: Boolean
        get() = BuildConfig.IS_PACKAGE_VISIBILITY_LIMITED

    fun loadApps() {
        if (_isLoading.value || _allApps.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            try {
                _allApps.value = analyzer.getInstalledApps()
            } catch (_: Exception) {
                _allApps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectApp(appInfo: AppInfo) {
        _selectedApp.value = appInfo
        ensureAnalysis(appInfo)
    }

    fun clearSelection() {
        _selectedApp.value = null
    }

    fun ensureAnalysis(appInfo: AppInfo) {
        val cacheKey = appInfo.analysisCacheKey()
        val currentState = _analysisStates.value[cacheKey]

        if (currentState?.status == AnalysisStatus.ANALYZING ||
            currentState?.status == AnalysisStatus.READY
        ) {
            return
        }

        _analysisStates.update { states ->
            states + (cacheKey to AppAnalysisState(status = AnalysisStatus.ANALYZING))
        }

        viewModelScope.launch(Dispatchers.IO) {
            val nextState = runCatching {
                AppAnalysisState(
                    status = AnalysisStatus.READY,
                    analysis = analyzer.analyzeApp(appInfo)
                )
            }.getOrElse { throwable ->
                AppAnalysisState(
                    status = AnalysisStatus.FAILED,
                    errorMessage = throwable.message ?: "Analysis failed"
                )
            }

            _analysisStates.update { states ->
                states + (cacheKey to nextState)
            }
        }
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = sortOrder)
    }

    fun updateAppFilter(appFilter: AppFilter) {
        _filterState.value = _filterState.value.copy(appFilter = appFilter)
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateScrollPosition(position: Int) {
        _scrollPosition.value = position
    }

    private fun filterAndSortApps(apps: List<AppInfo>, filterState: FilterState): List<AppInfo> {
        var filteredApps = apps

        filteredApps = when (filterState.appFilter) {
            AppFilter.ALL -> filteredApps
            AppFilter.USER_APPS_ONLY -> filteredApps.filter { !it.isSystemApp }
        }

        if (filterState.searchQuery.isNotBlank()) {
            val query = filterState.searchQuery.lowercase()
            filteredApps = filteredApps.filter { app ->
                app.appName.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query)
            }
        }

        return when (filterState.sortOrder) {
            SortOrder.NAME_ASC -> filteredApps.sortedBy { it.appName.lowercase() }
            SortOrder.NAME_DESC -> filteredApps.sortedByDescending { it.appName.lowercase() }
            SortOrder.INSTALL_DATE_ASC -> filteredApps.sortedBy { it.installTime }
            SortOrder.INSTALL_DATE_DESC -> filteredApps.sortedByDescending { it.installTime }
            SortOrder.UPDATE_DATE_DESC -> filteredApps.sortedByDescending { it.updateTime }
            SortOrder.SIZE_DESC -> filteredApps.sortedByDescending { it.appSize }
        }
    }
}
