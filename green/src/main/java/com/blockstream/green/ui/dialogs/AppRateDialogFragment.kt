package com.blockstream.green.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import app.rive.runtime.kotlin.core.PlayableInstance
import com.blockstream.base.IAppReview
import com.blockstream.common.Urls
import com.blockstream.green.databinding.AppRateDialogBinding
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.RiveListener
import com.blockstream.green.utils.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class AppRateDialogFragment : AbstractDialogFragment<AppRateDialogBinding>() {

    override fun inflate(layoutInflater: LayoutInflater): AppRateDialogBinding =
        AppRateDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true

    @Inject
    lateinit var appReview: IAppReview

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var lastStateName = ""
        var handled = false

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.rive.registerListener(object : RiveListener() {
            override fun notifyPause(animation: PlayableInstance) {
                val rate = when(lastStateName){
                    "1_star" -> 1
                    "2_stars" -> 2
                    "3_stars" -> 3
                    "4_stars" -> 4
                    "5_stars" -> 5
                    else -> 0
                }

                if(rate > 0 && !handled){
                    handled = true
                    ContextCompat.getMainExecutor(binding.root.context).execute {
                        appReview.showGooglePlayInAppReviewDialog(requireParentFragment()){
                            (requireParentFragment() as AppFragment<*>).openBrowser(Urls.BLOCKSTREAM_GOOGLE_PLAY)
                        }
                        settingsManager.setAskedAboutAppReview()
                    }
                }

            }
            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                lastStateName = stateName
            }
        })
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager) {
            showSingle(AppRateDialogFragment(), fragmentManager)
        }
    }
}
