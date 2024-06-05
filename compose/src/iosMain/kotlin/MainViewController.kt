import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import com.blockstream.compose.GreenApp
import com.blockstream.compose.di.startKoin
import com.blockstream.compose.theme.GreenTheme
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.compose.resources.ExperimentalResourceApi


@OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class)
fun MainViewController() = ComposeUIViewController(configure = {

    startKoin()

    delegate = object : ComposeUIViewControllerDelegate {
        override fun viewWillAppear(animated: Boolean) {
            super.viewWillAppear(animated)
        }

        override fun viewDidAppear(animated: Boolean) {
            super.viewDidAppear(animated)
        }

        override fun viewWillDisappear(animated: Boolean) {
            super.viewWillDisappear(animated)
        }

        override fun viewDidDisappear(animated: Boolean) {
            super.viewDidDisappear(animated)
        }
    }

}, content = {
    GreenTheme {
        GreenApp()
    }
})