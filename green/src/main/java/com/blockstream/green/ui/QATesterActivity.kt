package com.blockstream.green.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.blockstream.gdk.data.NetworkEvent
import com.blockstream.gdk.data.Notification
import com.blockstream.green.databinding.QaTesterActivityBinding
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.QTNotificationDelay
import com.blockstream.green.utils.isDevelopmentFlavor
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class QATesterActivity : AppCompatActivity() {

    private lateinit var binding: QaTesterActivityBinding

    @Inject
    lateinit var qaTester: QATester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent opening from adb
        if(!isDevelopmentFlavor()){
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

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonRequireLoginNotification.setOnClickListener {
            qaTester.notificationsEvents.onNext(QTNotificationDelay(Notification(event = "network", network = NetworkEvent(true, loginRequired = true, waiting = 0))))

            showEventSnackbar()
        }

        binding.buttonDisconnectNotification.setOnClickListener {
            qaTester.notificationsEvents.onNext(QTNotificationDelay(Notification(event = "network", network = NetworkEvent(false, waiting = 7, loginRequired = false))))
            qaTester.notificationsEvents.onNext(QTNotificationDelay(Notification(event = "network", network = NetworkEvent(true, loginRequired = false, waiting = 0)), delay = 10))

            showEventSnackbar()
        }
    }

    private fun showEventSnackbar(){
        Snackbar.make(binding.coordinator, "Event will be dispatched in 7 seconds", Snackbar.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}