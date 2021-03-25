package com.blockstream.green.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.blockstream.green.databinding.MainActivityBinding
import com.blockstream.green.devices.DeviceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), IActivity {

    @Inject
    lateinit var deviceManager: DeviceManager

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.also {
            // Prevent replacing title from NavController
            it.setDisplayShowTitleEnabled(false)
        }

        binding.toolbar.title = "Green"

        deviceManager.handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { deviceManager.handleIntent(it) }
    }

    override fun isDrawerOpen(): Boolean = binding.drawerLayout.isDrawerOpen(GravityCompat.START)

    override fun lockDrawer(isLocked: Boolean) {
        binding.drawerLayout.setDrawerLockMode(if (isLocked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun closeDrawer() {
        binding.drawerLayout.closeDrawers()
    }

    override fun setToolbar(
        title: String?, subtitle: String?, drawable: Drawable?, button: CharSequence?,
        buttonListener: View.OnClickListener?
    ){
        binding.toolbar.set(title, subtitle, drawable, button, buttonListener)
    }

    override fun setToolbarVisibility(isVisible: Boolean){
//        binding.appBarLayout = isVisible
    }

    override fun onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}

interface IActivity{
    fun isDrawerOpen(): Boolean
    fun closeDrawer()
    fun lockDrawer(isLocked: Boolean)
    fun setToolbar(
        title: String?,
        subtitle: String? = null,
        drawable: Drawable? = null,
        button: CharSequence? = null,
        buttonListener: View.OnClickListener? = null
    )
    fun setToolbarVisibility(isVisible: Boolean)
}

