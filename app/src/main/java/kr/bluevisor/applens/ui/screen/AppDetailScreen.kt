package kr.bluevisor.applens.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.model.AppType
import kr.bluevisor.applens.ui.components.AdMobBottomBanner
import kr.bluevisor.applens.viewmodel.AppDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appInfo: AppInfo,
    onBackClick: () -> Unit,
    viewModel: AppDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(appInfo) {
        viewModel.analyzeApp(context, appInfo)
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("App Details") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing app...")
                }
            }
        } else {
            Column {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AppDetailHeader(appInfo = appInfo)
                    }
                    
                    analysis?.let { appAnalysis ->
                        item {
                            AnalysisResultCard(appAnalysis = appAnalysis)
                        }
                        
                        item {
                            FrameworksCard(frameworks = appAnalysis.detectedFrameworks)
                        }
                        
                        if (appAnalysis.nativeLibraries.isNotEmpty()) {
                            item {
                                NativeLibrariesCard(libraries = appAnalysis.nativeLibraries)
                            }
                        }
                        
                        if (appAnalysis.usedLibraries.isNotEmpty()) {
                            item {
                                UsedLibrariesCard(libraries = appAnalysis.usedLibraries, appType = appAnalysis.appInfo.appType)
                            }
                        }
                        
                        item {
                            PermissionsCard(permissions = appAnalysis.permissions)
                        }
                    }
                }
                
                // Bottom Ad Banner
                AdMobBottomBanner()
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon
            if (appInfo.icon != null) {
                Image(
                    bitmap = appInfo.icon.toBitmap(80, 80).asImageBitmap(),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Default App Icon",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = appInfo.versionName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Target SDK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = appInfo.targetSdkVersion.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Updated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFullDate(appInfo.updateTime),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisResultCard(appAnalysis: kr.bluevisor.applens.model.AppAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analysis Result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppTypeChip(appType = appAnalysis.appInfo.appType)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Confidence",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(appAnalysis.analysisConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            appAnalysis.analysisConfidence >= 0.8f -> Color(0xFF4CAF50)
                            appAnalysis.analysisConfidence >= 0.6f -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FrameworksCard(frameworks: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detected Frameworks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (frameworks.isEmpty()) {
                Text(
                    text = "No specific frameworks detected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                frameworks.forEach { framework ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = framework,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NativeLibrariesCard(libraries: List<String>) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Native Libraries (${libraries.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val displayedLibraries = if (isExpanded) libraries else libraries.take(10)
            
            displayedLibraries.forEach { library ->
                Text(
                    text = library,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            
            if (libraries.size > 10) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "... and ${libraries.size - 10} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionsCard(permissions: List<String>) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissions (${permissions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val displayedPermissions = if (isExpanded) permissions else permissions.take(15)
            
            displayedPermissions.forEach { permission ->
                Text(
                    text = permission.substringAfterLast("."),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
            
            if (permissions.size > 15) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "... and ${permissions.size - 15} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun UsedLibrariesCard(libraries: List<String>, appType: AppType) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val cardTitle = when (appType) {
        AppType.FLUTTER -> "Flutter Packages"
        AppType.REACT_NATIVE -> "React Native Libraries"
        AppType.XAMARIN -> "Xamarin Assemblies"
        AppType.CORDOVA -> "Cordova Plugins"
        AppType.UNITY -> "Unity Libraries"
        AppType.KMP -> "KMP Libraries"
        else -> "Android Libraries"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$cardTitle (${libraries.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val displayedLibraries = if (isExpanded) libraries else libraries.take(8)
            
            displayedLibraries.forEach { library ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = library,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (libraries.size > 8) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "... and ${libraries.size - 8} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ComponentsCard(title: String, components: List<String>) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$title (${components.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val displayedComponents = if (isExpanded) components else components.take(10)
            
            displayedComponents.forEach { component ->
                Text(
                    text = component.substringAfterLast("."),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
            
            if (components.size > 10) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "... and ${components.size - 10} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatFullDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}