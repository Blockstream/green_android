package com.blockstream.green

import android.app.Application
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.fcm.Firebase
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.green.di.initKoinAndroid
import com.blockstream.green.lifecycle.ActivityLifecycle
import com.blockstream.green.settings.AndroidMigrator
import com.blockstream.green.utils.Loggable
import com.blockstream.green.utils.isDevelopmentFlavor
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class GreenApplication : Application() {
    private val androidMigrator: AndroidMigrator by inject()

    private val activityLifecycle: ActivityLifecycle by inject()

    private val firebase: Firebase by inject()

    private val applicationScope: ApplicationScope by inject()

    override fun onCreate() {
        super.onCreate()

        initKoinAndroid(this)

        val lifecycleManager: LifecycleManager = get()

        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
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
            applicationScope.launch {
                initShortcuts()
            }
        }

        firebase.initialize()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    suspend fun initShortcuts() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val hideAmountsShortcut: ShortcutInfo? = null
//            ShortcutInfo.Builder(this, GreenActivity.HIDE_AMOUNTS)
//            .setShortLabel(org.jetbrains.compose.resources.getString(Res.string.id_hide_amounts))
//            .setLongLabel(org.jetbrains.compose.resources.getString(Res.string.id_hide_amounts))
//            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_eye_close))
//            .setIntent(Intent(GreenActivity.HIDE_AMOUNTS, null, this, GreenActivity::class.java)).build()

        shortcutManager!!.dynamicShortcuts = listOfNotNull(hideAmountsShortcut)
    }

    companion object : Loggable()
}