
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.models.MainViewModel
import com.blockstream.compose.GreenApp
import com.blockstream.compose.di.initKoinDesktop
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId
import org.jetbrains.compose.ui.tooling.preview.Preview

fun main() = application {

    val appConfig = AppConfig.default(
        isDebug = true,
        filesDir = "",
        cacheDir = "",
        analyticsFeatureEnabled = false,
        lightningFeatureEnabled = false,
        storeRateEnabled = false
    )

    val appInfo = AppInfo(userAgent = "green_ios", "version", isDebug = true, isDevelopment = true)

    initKoinDesktop(
        appConfig = appConfig,
        appInfo = appInfo,
        doOnStartup = {
            Logger.d { "Start up" }
        }
    )

    val mainViewModel = remember { MainViewModel() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Blockstream Green",
    ) {
        GreenChrome()
        GreenTheme {
            GreenApp(mainViewModel = mainViewModel, modifier = Modifier.compatTestTagsAsResourceId())
        }
    }


}

@Preview
@Composable
fun Preview(){
    Text("OK")
}