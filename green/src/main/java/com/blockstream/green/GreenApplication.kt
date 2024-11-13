package com.blockstream.green

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import co.touchlab.kermit.Logger
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.fcm.Firebase
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.utils.Loggable
import com.blockstream.green.di.initKoinAndroid
import com.blockstream.green.lifecycle.ActivityLifecycle
import com.blockstream.green.settings.AndroidMigrator
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.QATesterActivity
import com.blockstream.green.utils.isDevelopmentFlavor
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class GreenApplication : Application() {
    private val androidMigrator: AndroidMigrator by inject()

    private val activityLifecycle: ActivityLifecycle by inject()

    private val zendeskSdk: ZendeskSdk by inject()

    private val firebase: Firebase by inject()

    override fun onCreate() {
        super.onCreate()

        initKoinAndroid(this)

        val lifecycleManager: LifecycleManager = get()

        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(object: DefaultLifecycleObserver{
            override fun onResume(owner: LifecycleOwner) {
                lifecycleManager.updateState(true)
            }

            override fun onPause(owner: LifecycleOwner) {
                lifecycleManager.updateState(false)
            }
        })

        registerActivityLifecycleCallbacks(activityLifecycle)

        androidMigrator.migrate()

        if (isDevelopmentFlavor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initShortcuts()
        }

        zendeskSdk.appVersion = BuildConfig.VERSION_NAME

        firebase.initialize()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun initShortcuts(){
        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val hideAmountsShortcut : ShortcutInfo = ShortcutInfo.Builder(this, MainActivity.HIDE_AMOUNTS)
            .setShortLabel(getString(R.string.id_hide_amounts))
            .setLongLabel(getString(R.string.id_hide_amounts))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_eye_close))
            .setIntent(Intent(MainActivity.HIDE_AMOUNTS, null, this, MainActivity::class.java)).build()

        val qaTesterShortcut : ShortcutInfo? =
            if (isDevelopmentFlavor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutInfo.Builder(this, "QATester")
                    .setShortLabel("QA Tester")
                    .setLongLabel("QA Tester")
                    .setIcon(Icon.createWithResource(this,  R.drawable.blockstream_jade_device))
                    .setIntent(Intent(Intent.ACTION_VIEW, null, this, QATesterActivity::class.java))
                    .build()
            } else {
                null
            }


        shortcutManager!!.dynamicShortcuts = listOfNotNull(hideAmountsShortcut, qaTesterShortcut)
    }

    companion object: Loggable()
}