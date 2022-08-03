package com.blockstream.green.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.NetworkEvent
import com.blockstream.gdk.data.Notification
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.Countly
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.QaTesterActivityBinding
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.bottomsheets.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.FilterableDataProvider
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.QTNotificationDelay
import com.blockstream.green.utils.isDevelopmentFlavor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import mu.KLogging
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class QATesterActivity : AppCompatActivity(), FilterableDataProvider {

    private lateinit var binding: QaTesterActivityBinding

    @Inject
    lateinit var qaTester: QATester

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var gdkBridge: GdkBridge

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var countly: Countly

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent opening from adb
        if(!isDevelopmentFlavor){
            finish()
        }

        binding = QaTesterActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.also {
            // Prevent replacing title from NavController
            it.setDisplayShowTitleEnabled(false)
        }

        binding.toolbar.title = "QA Tester"

        binding.qaTester = qaTester

        binding.buttonContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.buttonKill.setOnClickListener {
            exitProcess(0)
        }

        binding.buttonClearLocalSettings.setOnClickListener {
            settingsManager.clearAll()
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonDisconnectAll.setOnClickListener {
            for(session in sessionManager.getConnectedSessions()){
                session.disconnectAsync()
            }

            Snackbar.make(binding.coordinator, "All sessions was disconnected", Snackbar.LENGTH_SHORT).show()
        }

        binding.buttonDisconnectNotification.setOnClickListener {
            qaTester.notificationsEvents.tryEmit(QTNotificationDelay(Notification(event = "network", network = NetworkEvent.DisconnectedEvent)))
            qaTester.notificationsEvents.tryEmit(QTNotificationDelay(Notification(event = "network", network = NetworkEvent.ConnectedEvent), delay = 10))
        }

        binding.buttonCreateCustomNetwork.setOnClickListener {
            FilterBottomSheetDialogFragment.show(fragmentManager = supportFragmentManager)
        }

        binding.buttonClearGdk.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                logger.info { "Deleting ${applicationContext.filesDir.absolutePath}" }
                File(applicationContext.filesDir.absolutePath).deleteRecursively()

                Snackbar.make(binding.root, "Restarting application", Snackbar.LENGTH_SHORT).show()
                delay(1500)
                exitProcess(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.errorCounter.text = "Exception Counter: ${countly.exceptionCounter}"
    }

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {
        val adapter = ModelAdapter<Network, NetworkListItem>() {
            NetworkListItem(it.id, it.name, "")
        }.set(gdkBridge.networks.networks.values.toList())

        adapter.itemFilter.filterPredicate = { item: NetworkListItem, constraint: CharSequence? ->
            item.networkName.lowercase().contains(
                constraint.toString().lowercase()
            )
        }

        return adapter
    }
    override fun getFilterHeaderAdapter(requestCode: Int): GenericFastItemAdapter? = null
    override fun getFilterFooterAdapter(requestCode: Int): GenericFastItemAdapter? = null

    override fun filteredItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        val network = (item as NetworkListItem).network

        val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(this))
        dialogBinding.hint = "Hostname"
        dialogBinding.textInputLayout.helperText = "host:port"

        MaterialAlertDialogBuilder(this)
            .setTitle("Network hostname")
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                gdkBridge.registerCustomNetwork(network, dialogBinding.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object: KLogging()
}