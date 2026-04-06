package kr.bluevisor.applens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.MobileAds
import kr.bluevisor.applens.ui.screen.AppDetailScreen
import kr.bluevisor.applens.ui.screen.AppListScreen
import kr.bluevisor.applens.ui.theme.AppLensTheme
import kr.bluevisor.applens.viewmodel.AppLensViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
fun AppLensApp(viewModel: AppLensViewModel = viewModel()) {
    val selectedApp by viewModel.selectedApp.collectAsState()

    if (selectedApp == null) {
        AppListScreen(
            viewModel = viewModel,
            onAppClick = viewModel::selectApp
        )
    } else {
        BackHandler(onBack = viewModel::clearSelection)
        AppDetailScreen(
            appInfo = selectedApp!!,
            viewModel = viewModel,
            onBackClick = viewModel::clearSelection
        )
    }
}
