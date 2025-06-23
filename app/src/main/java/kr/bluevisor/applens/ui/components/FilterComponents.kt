package kr.bluevisor.applens.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.bluevisor.applens.model.AppFilter
import kr.bluevisor.applens.model.FilterState
import kr.bluevisor.applens.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterBar(
    filterState: FilterState,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onAppFilterChange: (AppFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search apps...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter and Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Button
            FilterChip(
                onClick = { showSortMenu = true },
                label = { Text("Sort: ${getSortOrderLabel(filterState.sortOrder)}") },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Sort Menu
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOrder.entries.forEach { sortOrder ->
                    DropdownMenuItem(
                        text = { Text(getSortOrderLabel(sortOrder)) },
                        onClick = {
                            onSortOrderChange(sortOrder)
                            showSortMenu = false
                        }
                    )
                }
            }

        }

        // Quick Filter Chips
        Spacer(modifier = Modifier.height(8.dp))
        QuickFilterChips(
            currentFilter = filterState.appFilter,
            onFilterChange = onAppFilterChange
        )
    }
}

@Composable
fun QuickFilterChips(
    currentFilter: AppFilter,
    onFilterChange: (AppFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickFilters = listOf(
        AppFilter.ALL,
        AppFilter.USER_APPS_ONLY
    )

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickFilters) { filter ->
            FilterChip(
                onClick = { onFilterChange(filter) },
                label = { Text(getAppFilterLabel(filter)) },
                selected = currentFilter == filter
            )
        }
    }
}

@Composable
fun AppCountIndicator(
    totalCount: Int,
    filteredCount: Int,
    modifier: Modifier = Modifier
) {
    if (totalCount != filteredCount) {
        Card(
            modifier = modifier.padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Showing $filteredCount of $totalCount apps",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getSortOrderLabel(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.NAME_ASC -> "Name A-Z"
        SortOrder.NAME_DESC -> "Name Z-A"
        SortOrder.INSTALL_DATE_ASC -> "Oldest First"
        SortOrder.INSTALL_DATE_DESC -> "Newest First"
        SortOrder.UPDATE_DATE_DESC -> "Recently Updated"
        SortOrder.SIZE_DESC -> "Size (Large)"
    }
}

private fun getAppFilterLabel(appFilter: AppFilter): String {
    return when (appFilter) {
        AppFilter.ALL -> "All Apps"
        AppFilter.USER_APPS_ONLY -> "User Apps"
    }
}