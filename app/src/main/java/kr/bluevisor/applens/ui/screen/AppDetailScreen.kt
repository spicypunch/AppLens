package kr.bluevisor.applens.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kr.bluevisor.applens.model.AnalysisStatus
import kr.bluevisor.applens.model.AppAnalysis
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.ObservedArtifact
import kr.bluevisor.applens.model.PermissionDetails
import kr.bluevisor.applens.model.VisibilityScope
import kr.bluevisor.applens.model.analysisCacheKey
import kr.bluevisor.applens.ui.components.AdMobBottomBanner
import kr.bluevisor.applens.viewmodel.AppLensViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appInfo: AppInfo,
    viewModel: AppLensViewModel,
    onBackClick: () -> Unit
) {
    val analysisStates by viewModel.analysisStates.collectAsState()
    val analysisState = analysisStates[appInfo.analysisCacheKey()]

    LaunchedEffect(appInfo.analysisCacheKey()) {
        viewModel.ensureAnalysis(appInfo)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("App Details") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        when (analysisState?.status) {
            null, AnalysisStatus.NOT_ANALYZED, AnalysisStatus.ANALYZING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Inspecting packaged artifacts...")
                    }
                }
            }

            AnalysisStatus.FAILED -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (viewModel.isPackageVisibilityLimited) {
                        PackageVisibilityNotice(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { AppDetailHeader(appInfo) }
                        item {
                            AnalysisErrorCard(
                                message = analysisState.errorMessage ?: "Analysis failed"
                            )
                        }
                    }

                    AdMobBottomBanner()
                }
            }

            AnalysisStatus.READY -> {
                val analysis = analysisState.analysis!!

                Column(modifier = Modifier.fillMaxSize()) {
                    if (viewModel.isPackageVisibilityLimited) {
                        PackageVisibilityNotice(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { AppDetailHeader(appInfo) }
                        item { AnalysisSummaryCard(analysis) }

                        if (analysis.frameworkEvidence.isNotEmpty()) {
                            item {
                                ObservedArtifactsCard(
                                    title = "Framework Evidence",
                                    artifacts = analysis.frameworkEvidence
                                )
                            }
                        }

                        if (analysis.detectedLibraries.isNotEmpty()) {
                            item {
                                ObservedArtifactsCard(
                                    title = "Observed Packaged Libraries",
                                    artifacts = analysis.detectedLibraries
                                )
                            }
                        }

                        if (analysis.nativeLibraries.isNotEmpty()) {
                            item {
                                ObservedArtifactsCard(
                                    title = "Native Shared Objects",
                                    artifacts = analysis.nativeLibraries
                                )
                            }
                        }

                        item {
                            PermissionsCard(permissions = analysis.permissions)
                        }
                    }

                    AdMobBottomBanner()
                }
            }
        }
    }
}

@Composable
fun AppDetailHeader(appInfo: AppInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (appInfo.icon != null) {
                Image(
                    bitmap = appInfo.icon.toBitmap(80, 80).asImageBitmap(),
                    contentDescription = "App icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Default app icon",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = appInfo.appName,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = appInfo.packageName,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = "Version",
                    value = appInfo.versionName
                )
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = "Target SDK",
                    value = appInfo.targetSdkVersion.toString()
                )
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = "Updated",
                    value = formatFullDate(appInfo.updateTime)
                )
            }
        }
    }
}

@Composable
fun AnalysisSummaryCard(appAnalysis: AppAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Analysis Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Detected stack",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FrameworkChip(framework = appAnalysis.framework)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryStat(
                    label = "Visibility",
                    value = visibilityScopeLabel(appAnalysis.visibilityScope)
                )
                SummaryStat(
                    label = "APKs analyzed",
                    value = appAnalysis.analyzedApkCount.toString()
                )
            }
        }
    }
}

@Composable
fun SummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AnalysisErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Analysis failed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun ObservedArtifactsCard(
    title: String,
    artifacts: List<ObservedArtifact>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayedArtifacts = if (isExpanded) artifacts else artifacts.take(8)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$title (${artifacts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            displayedArtifacts.forEach { artifact ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = artifactLabel(artifact),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = artifact.matchedPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Source: ${artifact.sourceApkOrSplit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (artifacts.size > 8) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "... and ${artifacts.size - 8} more"
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionsCard(permissions: List<PermissionDetails>) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayedPermissions = if (isExpanded) permissions else permissions.take(10)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Manifest Permissions (${permissions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (permissions.isEmpty()) {
                Text(
                    text = "No manifest permissions requested.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                displayedPermissions.forEach { permission ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = permission.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PermissionFlagChip(
                                    label = if (permission.granted) "Granted" else "Not granted"
                                )
                                if (permission.implicit) {
                                    PermissionFlagChip(label = "Implicit")
                                }
                                if (permission.neverForLocation) {
                                    PermissionFlagChip(label = "Never for location")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Protection: ${permission.protectionLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (permissions.size > 10) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "... and ${permissions.size - 10} more"
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionFlagChip(label: String) {
    FilterChip(
        selected = true,
        onClick = {},
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

private fun artifactLabel(artifact: ObservedArtifact): String {
    return artifact.abi?.let { "${artifact.name} ($it)" } ?: artifact.name
}

private fun visibilityScopeLabel(scope: VisibilityScope): String {
    return when (scope) {
        VisibilityScope.FULL_DEVICE -> "Full"
        VisibilityScope.FILTERED -> "Filtered"
    }
}

private fun formatFullDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
