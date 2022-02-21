package com.blockstream.green

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.blockstream.gdk.AssetManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.managers.NotificationManager
import com.blockstream.green.settings.Migrator
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.QATesterActivity
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.isDevelopmentFlavor
import com.pandulapeter.beagle.Beagle
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias ApplicationScope = kotlinx.coroutines.CoroutineScope

@HiltAndroidApp
class GreenApplication : Application(){

    @Inject
    lateinit var migrator: Migrator

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var assetManager: AssetManager

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var qaTester: QATester

    @Inject
    lateinit var greenWallet: GreenWallet

    @Inject
    lateinit var beagle: Beagle

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            migrator.migrate()
        }

        if (isDevelopmentFlavor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initShortcuts()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun initShortcuts(){
        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val shortcut = ShortcutInfo.Builder(this, "QATester")
            .setShortLabel("QA Tester")
            .setLongLabel("QA Tester")
            .setIcon(Icon.createWithResource(this, R.drawable.blockstream_jade_device))
            .setIntent(Intent(Intent.ACTION_VIEW, null, this, QATesterActivity::class.java))
            .build()
        shortcutManager!!.dynamicShortcuts = listOf(shortcut)
    }
}