package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import breez_sdk.InputType
import com.blockstream.common.data.ErrorReport
import com.blockstream.green.R
import com.blockstream.green.databinding.LnurlAuthFragmentBinding
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LnUrlAuthFragment :
    AbstractWalletFragment<LnurlAuthFragmentBinding>(R.layout.lnurl_auth_fragment, menuRes = 0) {
    override val screenName = "LNURLAuth"

    val args: LnUrlAuthFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    private val requestDataOrNull: InputType.LnUrlAuth? by lazy {
        session.parseInput(
            args.auth
        )?.second as? InputType.LnUrlAuth
    }

    private val requestData by lazy { requestDataOrNull!!.data }

    override val subtitle: String
        get() = wallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning


    val viewModel: LnUrlAuthViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.accountAsset,
            requestData
        )
    }

    override fun getAppViewModel(): AppViewModelAndroid? {
        return if (requestDataOrNull != null) viewModel else null
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        if (requestDataOrNull == null) {
            popBackStack()
            return
        }

        binding.vm = viewModel

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { throwable ->
                errorDialog(throwable = throwable, errorReport = ErrorReport.create(throwable = throwable, network = session.lightning, session = session)) {
                    popBackStack()
                }
            }
        }

        binding.buttonAuthenticate.setOnClickListener {
            viewModel.auth()
        }
    }
}