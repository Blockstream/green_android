package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import breez_sdk.InputType
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.LnurlAuthFragmentBinding
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
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

    @Inject
    lateinit var viewModelFactory: LnUrlAuthViewModel.AssistedFactory

    val viewModel: LnUrlAuthViewModel by viewModels {
        LnUrlAuthViewModel.provideFactory(
            viewModelFactory,
            wallet = args.wallet,
            accountAsset = args.accountAsset,
            requestData = requestData
        )
    }

    override fun getAppViewModel(): AppViewModel? {
        return if (requestDataOrNull != null) viewModel else null
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        if (requestDataOrNull == null) {
            popBackStack()
            return
        }

        binding.vm = viewModel

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateBack>()?.let {
                snackbar(R.string.id_authentication_successful)
                popBackStack()
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { throwable ->
                errorDialog(throwable, showCopy = true) {
                    popBackStack()
                }
            }
        }

        binding.buttonAuthenticate.setOnClickListener {
            viewModel.auth()
        }
    }
}