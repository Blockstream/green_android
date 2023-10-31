package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.lightning.LnUrlAuthViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.LnurlAuthFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class LnUrlAuthFragment : AppFragment<LnurlAuthFragmentBinding>(R.layout.lnurl_auth_fragment, menuRes = 0) {

    val args: LnUrlAuthFragmentArgs by navArgs()

    private val requestData by lazy { args.lnUrlAuthRequest.requestData }

    override val subtitle: String
        get() = args.wallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning


    val viewModel: LnUrlAuthViewModel by viewModel {
        parametersOf(
            args.wallet,
            requestData
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonAuthenticate.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }
    }
}
