import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.compose.GreenApp
import com.blockstream.compose.di.startKoin
import com.blockstream.compose.theme.GreenTheme
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
    GreenTheme {
        GreenApp()
    }
})