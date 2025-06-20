package kr.bluevisor.applens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import kr.bluevisor.applens.model.AppInfo
import kr.bluevisor.applens.ui.screen.AppDetailScreen
import kr.bluevisor.applens.ui.screen.AppListScreen
import kr.bluevisor.applens.ui.theme.AppLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        
        setContent {
            AppLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppLensApp()
                }
            }
        }
    }
}

@Composable
fun AppLensApp() {
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    
    if (selectedApp == null) {
        AppListScreen(
            onAppClick = { app ->
                selectedApp = app
            }
        )
    } else {
        BackHandler {
            selectedApp = null
        }
        AppDetailScreen(
            appInfo = selectedApp!!,
            onBackClick = {
                selectedApp = null
            }
        )
    }
}