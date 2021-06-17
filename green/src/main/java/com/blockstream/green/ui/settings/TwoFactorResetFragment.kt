package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.green.R
import com.blockstream.green.data.Countries.COUNTRIES
import com.blockstream.green.data.Country
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.TwofactorEditFragmentBinding
import com.blockstream.green.databinding.TwofactorResetFragmentBinding
import com.blockstream.green.ui.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.FilterableDataProvider
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.utils.localized2faMethod
import com.blockstream.green.utils.openKeyboard
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TwoFactorResetFragment : WalletFragment<TwofactorResetFragmentBinding>(R.layout.twofactor_reset_fragment, 0) {
    val args: TwoFactorResetFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    override val isAdjustResize: Boolean = true

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by navGraphViewModels(R.id.settings_nav_graph) {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonContinue.setOnClickListener {
            viewModel.reset2FA(
                binding.emailEditText.text.toString(),
                false,
                DialogTwoFactorResolver(requireContext())
            )
        }

        viewModel.onError.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                findNavController().popBackStack()

            }
        }
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel
}