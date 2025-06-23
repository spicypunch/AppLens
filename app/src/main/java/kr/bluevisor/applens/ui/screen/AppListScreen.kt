package kr.bluevisor.applens.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.AppType
import kr.bluevisor.applens.ui.components.AdMobBottomBanner
import kr.bluevisor.applens.ui.components.AppCountIndicator
import kr.bluevisor.applens.ui.components.SearchAndFilterBar
import kr.bluevisor.applens.viewmodel.AppListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onAppClick: (AppInfo) -> Unit,
    viewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollPosition by viewModel.scrollPosition.collectAsState()
    
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollPosition
    )
    
    LaunchedEffect(context) {
        if (allApps.isEmpty() && !isLoading) {
            viewModel.loadApps(context)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateScrollPosition(listState.firstVisibleItemIndex)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "App Lens",
                    fontWeight = FontWeight.Bold
                )
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column {
                SearchAndFilterBar(
                    filterState = filterState,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSortOrderChange = viewModel::updateSortOrder,
                    onAppFilterChange = viewModel::updateAppFilter
                )
                
                AppCountIndicator(
                    totalCount = allApps.size,
                    filteredCount = apps.size
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps) { app ->
                        AppListItem(
                            app = app,
                            onClick = { onAppClick(app) }
                        )
                    }
                }
                
                // Bottom Ad Banner
                AdMobBottomBanner()
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Default App Icon",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // App Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTypeChip(appType = app.appType)
                    
                    if (app.isSystemApp) {
                        AssistChip(
                            onClick = { },
                            label = { Text("System", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
            
            // Version and Date
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = app.versionName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = formatDate(app.updateTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppTypeChip(appType: AppType) {
    val (text, color) = when (appType) {
        AppType.FLUTTER -> "Flutter" to Color(0xFF0175C2)
        AppType.REACT_NATIVE -> "React Native" to Color(0xFF61DAFB)
        AppType.XAMARIN -> "Xamarin" to Color(0xFF3498DB)
        AppType.CORDOVA -> "Cordova" to Color(0xFFE44D26)
        AppType.IONIC -> "Ionic" to Color(0xFF4C8DFF)
        AppType.NATIVE_ANDROID -> "Native" to Color(0xFF3DDC84)
        AppType.KMP -> "KMP" to Color(0xFF7F52FF)
        AppType.UNITY -> "Unity" to Color(0xFF000000)
        AppType.UNKNOWN -> "Unknown" to Color(0xFF757575)
    }
    
    AssistChip(
        onClick = { },
        label = { 
            Text(
                text = text, 
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            ) 
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color
        )
    )
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}