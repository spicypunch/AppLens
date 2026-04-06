package kr.bluevisor.applens.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kr.bluevisor.applens.model.AnalysisStatus
import kr.bluevisor.applens.model.AppAnalysisState
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.FrameworkType
import kr.bluevisor.applens.model.analysisCacheKey
import kr.bluevisor.applens.ui.presentation.displayLabel
import kr.bluevisor.applens.ui.presentation.listChipLabelFor
import kr.bluevisor.applens.ui.components.AdMobBottomBanner
import kr.bluevisor.applens.ui.components.AppCountIndicator
import kr.bluevisor.applens.ui.components.SearchAndFilterBar
import kr.bluevisor.applens.viewmodel.AppLensViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppLensViewModel,
    onAppClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollPosition by viewModel.scrollPosition.collectAsState()
    val analysisStates by viewModel.analysisStates.collectAsState()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollPosition
    )

    LaunchedEffect(context) {
        viewModel.loadApps()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateScrollPosition(listState.firstVisibleItemIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            Column(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isPackageVisibilityLimited) {
                    PackageVisibilityNotice(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

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

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No visible apps found for this build.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = apps,
                            key = { it.analysisCacheKey() }
                        ) { app ->
                            AppListItem(
                                app = app,
                                analysisState = analysisStates[app.analysisCacheKey()],
                                onClick = { onAppClick(app) }
                            )
                        }
                    }
                }

                AdMobBottomBanner()
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    analysisState: AppAnalysisState?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = "App icon",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Default app icon",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
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
                    val chipLabel = listChipLabelFor(analysisState)
                    if (chipLabel != null) {
                        if (analysisState?.status == AnalysisStatus.READY) {
                            FrameworkChip(framework = analysisState.analysis!!.framework)
                        } else {
                            NeutralStatusChip(label = chipLabel)
                        }
                    }

                    if (app.isSystemApp) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "System",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
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
fun FrameworkChip(framework: FrameworkType) {
    val (color, textColor) = when (framework) {
        FrameworkType.FLUTTER -> Color(0xFF0175C2) to Color.White
        FrameworkType.REACT_NATIVE -> Color(0xFF61DAFB) to Color.Black
        FrameworkType.XAMARIN -> Color(0xFF3498DB) to Color.White
        FrameworkType.CORDOVA -> Color(0xFFE44D26) to Color.White
        FrameworkType.UNITY -> Color(0xFF111111) to Color.White
        FrameworkType.UNCLASSIFIED -> Color(0xFF3DDC84) to Color.Black
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = framework.displayLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color
        )
    )
}

@Composable
fun NeutralStatusChip(label: String) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
fun PackageVisibilityNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = "This build can analyze only the apps visible under Android package visibility policies.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
