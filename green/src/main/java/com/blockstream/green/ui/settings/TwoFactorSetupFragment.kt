package com.blockstream.green.ui.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.Countries.COUNTRIES
import com.blockstream.green.data.Country
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.TwofactorSetupFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.bottomsheets.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.FilterableDataProvider
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.*
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class TwoFactorSetupAction {
    SETUP, SETUP_EMAIL, RESET, CANCEL, DISPUTE, UNDO_DISPUTE
}

@AndroidEntryPoint
class TwoFactorSetupFragment : WalletFragment<TwofactorSetupFragmentBinding>(R.layout.twofactor_setup_fragment, 0),
    FilterableDataProvider {
    val args: TwoFactorSetupFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName by lazy{
        when (args.action) {
            TwoFactorSetupAction.SETUP, TwoFactorSetupAction.SETUP_EMAIL -> {
                "Setup"
            }
            TwoFactorSetupAction.RESET -> {
                "Reset"
            }
            TwoFactorSetupAction.CANCEL -> {
                "CancelDispute"
            }
            TwoFactorSetupAction.DISPUTE -> {
                "Dispute"
            }
            TwoFactorSetupAction.UNDO_DISPUTE -> {
                "UndoDispute"
            }
        }.let {
            "WalletSettings2FA$it"
        }
    }
    override val segmentation by lazy { if(isSessionAndWalletRequired() && isSessionNetworkInitialized) countly.twoFactorSegmentation(session, viewModel.getSubAccountLiveData().value, args.method) else null }

    override val isAdjustResize: Boolean = true

    @Inject
    lateinit var viewModelFactory: TwoFactorSetupViewModel.AssistedFactory
    val viewModel: TwoFactorSetupViewModel by viewModels {
        TwoFactorSetupViewModel.provideFactory(viewModelFactory, args.wallet, args.method, args.action)
    }

    override val title: String
        get() {
            val methodLocalized = requireContext().localized2faMethod(args.method.gdkType)

            return when (args.action) {
                TwoFactorSetupAction.SETUP, TwoFactorSetupAction.SETUP_EMAIL -> {
                    getString(R.string.id_1s_twofactor_setup, methodLocalized)
                }
                TwoFactorSetupAction.RESET -> {
                    getString(R.string.id_request_twofactor_reset)
                }
                TwoFactorSetupAction.CANCEL -> {
                    getString(R.string.id_cancel_2fa_reset)
                }
                TwoFactorSetupAction.DISPUTE -> {
                    getString(R.string.id_dispute_twofactor_reset)
                }
                TwoFactorSetupAction.UNDO_DISPUTE -> {
                    getString(R.string.id_undo_2fa_dispute)
                }
            }
        }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val action = args.action

        binding.vm = viewModel

        if(session.isLiquid && action == TwoFactorSetupAction.RESET){
            snackbar("2FA Reset is not possible in a Liquid Wallet")
            popBackStack()
            return
        }
        
        when(action){
            TwoFactorSetupAction.SETUP -> {
                binding.message = getString(R.string.id_insert_your_email_to_receive)
                binding.button = getString(R.string.id_continue)
            }
            TwoFactorSetupAction.SETUP_EMAIL -> {
                binding.message = getString(R.string.id_use_your_email_to_receive)
                binding.button = getString(R.string.id_continue)
            }
            TwoFactorSetupAction.RESET -> {
                binding.message = getString(R.string.id_resetting_your_twofactor_takes)
                binding.button = getString(R.string.id_request_twofactor_reset)
            }
            TwoFactorSetupAction.CANCEL -> {
                // Cancel action
                viewModel.cancel2FA(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            }
            TwoFactorSetupAction.DISPUTE -> {
                binding.message = getString(R.string.id_if_you_did_not_request_the)
                binding.button = getString(R.string.id_dispute_twofactor_reset)
            }
            TwoFactorSetupAction.UNDO_DISPUTE -> {
                binding.message = getString(R.string.id_if_you_initiated_the_2fa_reset)
                binding.button = getString(R.string.id_undo_2fa_dispute)
            }
        }

        binding.buttonContinue.setOnClickListener {
            hideKeyboard()

            if(action == TwoFactorSetupAction.SETUP || action == TwoFactorSetupAction.SETUP_EMAIL){
                var data = when(viewModel.method){
                    TwoFactorMethod.SMS, TwoFactorMethod.PHONE, TwoFactorMethod.TELEGRAM -> {
                        viewModel.getPhoneNumberValue()
                    }
                    TwoFactorMethod.EMAIL -> {
                        viewModel.getEmailValue()
                    }
                    TwoFactorMethod.AUTHENTICATOR -> {
                        viewModel.authenticatorUrl ?: ""
                    }
                }
                // setupEmail is used only to setup the email address for recovery transactions legacy option
                viewModel.enable2FA(args.method, data = data, enabled = args.action != TwoFactorSetupAction.SETUP_EMAIL, twoFactorResolver = DialogTwoFactorResolver(this))
            }else{
                val email = binding.emailEditText.text.toString()
                when(action){
                    TwoFactorSetupAction.RESET -> {
                        viewModel.reset2FA(
                            email = email,
                            isDispute = false,
                            twoFactorResolver = DialogTwoFactorResolver(requireContext())
                        )
                    }
                    TwoFactorSetupAction.DISPUTE -> {
                        viewModel.reset2FA(
                            email = email,
                            isDispute = true,
                            twoFactorResolver = DialogTwoFactorResolver(requireContext())
                        )
                    }
                    TwoFactorSetupAction.UNDO_DISPUTE -> {
                        viewModel.undoReset2FA(
                            email = email,
                            twoFactorResolver = DialogTwoFactorResolver(requireContext())
                        )
                    }
                    TwoFactorSetupAction.SETUP,TwoFactorSetupAction.SETUP_EMAIL, TwoFactorSetupAction.CANCEL -> {

                    }
                }
            }
        }

        binding.authenticatorCode.setOnClickListener {
            copyToClipboard("Address", viewModel.authenticatorCode.value ?: "", requireContext())
            snackbar(R.string.id_copied_to_clipboard)
            binding.authenticatorCode.pulse()
        }

        binding.countryEditText.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                openCountryFilter()
            }
        }

        viewModel.authenticatorQRBitmap.observe(viewLifecycleOwner) {
            binding.authenticatorQR.setImageDrawable(BitmapDrawable(resources, it).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        }

        viewModel.onError.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it) {
                    if(action == TwoFactorSetupAction.CANCEL){
                        popBackStack()
                    }
                }
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledForType<GdkEvent.Success>()?.let {
                // Hint TwoFactorAuthenticationFragment / WalletSettingsFragment to update TwoFactorConfig
                setNavigationResult(result = true)
                popBackStack()
            }
        }
    }

    private fun openCountryFilter(){
        FilterBottomSheetDialogFragment.show(childFragmentManager)
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    override fun getModelAdapter(): ModelAdapter<*, *> {
        val adapter = ModelAdapter<Country, CountryListItem>() {
            CountryListItem(it)
        }.set(COUNTRIES)

        adapter.itemFilter.filterPredicate = { item: CountryListItem, constraint: CharSequence? ->
            item.country.name.lowercase().contains(
                constraint.toString().lowercase()
            )
        }

        return adapter
    }

    override fun filteredItemClicked(item: GenericItem, position: Int) {
        binding.countryEditText.setText((item as CountryListItem).country.dialCodeString)
        binding.phoneNumberEditText.requestFocus()
        openKeyboard()
    }
}