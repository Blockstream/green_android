package com.blockstream.green.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockstream.green.databinding.UiComponentsBinding
import com.blockstream.green.utils.RiveListener
import com.blockstream.green.utils.isDevelopmentFlavor
import dagger.hilt.android.AndroidEntryPoint
import ly.count.android.sdk.Countly
import mu.KLogging


@AndroidEntryPoint
class UIComponentsActivity : AppCompatActivity() {

    private lateinit var binding: UiComponentsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent opening from adb
        if(!isDevelopmentFlavor){
            finish()
        }

        binding = UiComponentsBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.also {
            // Prevent replacing title from NavController
            it.setDisplayShowTitleEnabled(false)
        }

        binding.toolbar.title = "UI Components"

        binding.rive.registerListener(object : RiveListener() {
            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                logger.info { "$stateMachineName : $stateName" }
            }
        })


        Countly.sharedInstance().also { countly ->
            countly.feedback().getAvailableFeedbackWidgets { countlyFeedbackWidgets, s ->

//                countly.feedback().getFeedbackUrl(countlyFeedbackWidgets.firstOrNull())?.also {
//                    logger.info { it }
//                    binding.webview.loadUrl(it)
//                }

//                countlyFeedbackWidgets.firstOrNull()?.also {
//                    countly.feedback().presentFeedbackWidget(it, this, "Close", null)
//                }

            }
        }

    }

    companion object: KLogging()
}