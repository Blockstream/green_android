import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import com.blockstream.data.managers.LifecycleManager
import com.blockstream.compose.models.MainViewModel
import com.blockstream.compose.GreenApp
import com.blockstream.compose.di.startKoin
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId
import org.koin.mp.KoinPlatformTools

fun MainViewController() = ComposeUIViewController(configure = {

    startKoin()

    val lifecycleManager = KoinPlatformTools.defaultContext().get().get<LifecycleManager>()

    delegate = object : ComposeUIViewControllerDelegate {
        override fun viewWillAppear(animated: Boolean) {
            super.viewWillAppear(animated)
            lifecycleManager.updateState(isOnForeground = true)
        }

        override fun viewDidDisappear(animated: Boolean) {
            super.viewDidDisappear(animated)
            lifecycleManager.updateState(isOnForeground = false)
        }
    }
}, content = {
    val mainViewModel = remember { MainViewModel() }

    GreenChrome()
    GreenTheme {
        GreenApp(mainViewModel = mainViewModel, modifier = Modifier.compatTestTagsAsResourceId())
    }
})