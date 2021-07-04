package com.blockstream.green.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.blockstream.green.R
import com.blockstream.green.databinding.BridgeActivityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BridgeActivity : AppCompatActivity(), IActivity {

    private lateinit var navController: NavController
    private lateinit var binding: BridgeActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = BridgeActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.also {
            // Prevent replacing title from NavController
            it.setDisplayShowTitleEnabled(false)
        }


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val inflater = navController.navInflater

        val isSettings = SETTINGS == intent.action
        val isPin = PIN == intent.action
        val isBackupRecovery = BACKUP_RECOVERY == intent.action
        val isAddAccount = ADD_ACCOUNT == intent.action
        val isTwoFactorReset = TWO_FACTOR_RESET == intent.action
        val isReceive = RECEIVE == intent.action

        val graph = inflater.inflate(R.navigation.nav_graph)
        var extras = intent.extras

        graph.startDestination = when {
            isSettings || isPin -> {
                R.id.settings_nav_graph
            }
            isBackupRecovery -> {
                 R.id.recoveryIntroFragment
            }
            isAddAccount -> {
                 R.id.chooseAccountTypeFragment
            }
            isReceive -> {
                 R.id.receiveFragment
            }
            isTwoFactorReset  -> {
                R.id.settings_nav_graph
            }
            else -> graph.startDestination
        }

        navController.setGraph(graph, extras)

        val appBarConfiguration = AppBarConfiguration(
            setOf()
        )

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home ->
                if (navController.previousBackStackEntry == null) {
                    finish()
                }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun isDrawerOpen() = false

    override fun closeDrawer() { }

    override fun lockDrawer(isLocked: Boolean) { }

    override fun setToolbarVisibility(isVisible: Boolean){
        binding.appBarLayout.isVisible = isVisible
    }

    override fun setToolbar(
        title: String?,
        subtitle: String?,
        drawable: Drawable?,
        button: CharSequence?,
        buttonListener: View.OnClickListener?
    ){
        binding.toolbar.set(title, subtitle, drawable, button, buttonListener)
    }

    override fun setTitle(title: CharSequence?) {
        setToolbar(title?.toString() ?: "")
    }

    companion object{
        const val PIN = "PIN"
        const val SETTINGS = "SETTINGS"
        const val ADD_ACCOUNT = "ADD_ACCOUNT"
        const val TWO_FACTOR_RESET = "TWO_FACTOR_RESET"
        const val BACKUP_RECOVERY = "BACKUP_RECOVERY"
        const val RECEIVE = "RECEIVE"
    }
}