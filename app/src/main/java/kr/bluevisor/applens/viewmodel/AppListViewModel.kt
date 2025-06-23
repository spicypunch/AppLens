package kr.bluevisor.applens.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.bluevisor.applens.analyzer.AppAnalyzer
import kr.bluevisor.applens.model.AppFilter
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.AppType
import kr.bluevisor.applens.model.FilterState
import kr.bluevisor.applens.model.SortOrder

class AppListViewModel : ViewModel() {
    
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _filterState = MutableStateFlow(FilterState())
    
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
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
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()
    
    fun loadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            
            try {
                val analyzer = AppAnalyzer(context)
                val installedApps = analyzer.getInstalledApps()
                _allApps.value = installedApps
            } catch (e: Exception) {
                _allApps.value = emptyList()
            } finally {
                _isLoading.value = false
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
        
        // Apply app type filter
        filteredApps = when (filterState.appFilter) {
            AppFilter.ALL -> filteredApps
            AppFilter.USER_APPS_ONLY -> filteredApps.filter { !it.isSystemApp }
        }
        
        // Apply search filter
        if (filterState.searchQuery.isNotBlank()) {
            val query = filterState.searchQuery.lowercase()
            filteredApps = filteredApps.filter { app ->
                app.appName.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
            }
        }
        
        // Apply sorting
        filteredApps = when (filterState.sortOrder) {
            SortOrder.NAME_ASC -> filteredApps.sortedBy { it.appName.lowercase() }
            SortOrder.NAME_DESC -> filteredApps.sortedByDescending { it.appName.lowercase() }
            SortOrder.INSTALL_DATE_ASC -> filteredApps.sortedBy { it.installTime }
            SortOrder.INSTALL_DATE_DESC -> filteredApps.sortedByDescending { it.installTime }
            SortOrder.UPDATE_DATE_DESC -> filteredApps.sortedByDescending { it.updateTime }
            SortOrder.SIZE_DESC -> filteredApps.sortedByDescending { it.appSize }
        }
        
        return filteredApps
    }
}