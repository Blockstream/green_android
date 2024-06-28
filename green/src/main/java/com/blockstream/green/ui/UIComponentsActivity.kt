package com.blockstream.green.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockstream.green.databinding.UiComponentsBinding
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.common.utils.Loggable


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
    }

    companion object: Loggable()
}