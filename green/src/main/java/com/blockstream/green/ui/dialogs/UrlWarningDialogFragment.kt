package com.blockstream.green.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.hostname
import com.blockstream.green.R
import com.blockstream.green.databinding.UrlWarningDialogBinding
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.utils.openNewTicket
import mu.KLogging
import org.koin.android.ext.android.inject

class UrlWarningDialogFragment : AbstractDialogFragment<UrlWarningDialogBinding>() {

    private val settingsManager: SettingsManager by inject()
    override fun inflate(layoutInflater: LayoutInflater): UrlWarningDialogBinding =
        UrlWarningDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true

    val urls
        get() = arguments?.getStringArrayList(URLS)?.toList() ?: listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        binding.connectionAttemptHost = getString(R.string.id_connection_attempt_to_s, urls.map { it.hostname() }.joinToString(", "))

        binding.buttonClose.setOnClickListener {
            handle(false)
        }

        binding.buttonContactSupport.setOnClickListener {
            openNewTicket(
                settingsManager = settingsManager,
                subject = "Non-default PIN server",
                isJade = true
            )
            handle(false)
        }

        binding.buttonAdvanced.setOnClickListener {
            binding.showAdvanced = true
        }

        binding.buttonAllow.setOnClickListener {
            handle(true)
        }
    }

    fun handle(allow: Boolean) {
        if(binding.remember.isChecked) {
            settingsManager.setAllowCustomPinServer(urls)
        }

        (requireActivity() as? MainActivity)
            ?.activityViewModel
            ?.unsafeUrlWarningEmitter
            ?.complete(allow)

        dismiss()
    }

    companion object : KLogging() {
        private const val URLS = "URLS"
        fun show(urls: List<String>, fragmentManager: FragmentManager) {
            showSingle(UrlWarningDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putStringArrayList(URLS, ArrayList(urls))
                }
            }, fragmentManager)
        }
    }
}
