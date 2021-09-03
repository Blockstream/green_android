package com.blockstream.green.ui.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.green.R
import com.blockstream.green.data.Countries.COUNTRIES
import com.blockstream.green.data.Country
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.TwofactorSetupFragmentBinding
import com.blockstream.green.ui.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.FilterableDataProvider
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.*
import com.greenaddress.Bridge
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

enum class TwoFactorSetupAction {
    SETUP, SETUP_EMAIL, RESET, CANCEL, DISPUTE, UNDO_DISPUTE
}

@AndroidEntryPoint
class TwoFactorSetupFragment : WalletFragment<TwofactorSetupFragmentBinding>(R.layout.twofactor_setup_fragment, 0),
    FilterableDataProvider {
    val args: TwoFactorSetupFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    override val isAdjustResize: Boolean = true

    @Inject
    lateinit var viewModelFactory: TwoFactorSetupViewModel.AssistedFactory
    val viewModel: TwoFactorSetupViewModel by viewModels {
        TwoFactorSetupViewModel.provideFactory(viewModelFactory, args.wallet, args.method, args.action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val methodLocalized = requireContext().localized2faMethod(args.method.gdkType)

        val action = args.action

        binding.vm = viewModel
        
        when(action){
            TwoFactorSetupAction.SETUP -> {
                setToolbar(title = getString(R.string.id_1s_twofactor_setup, methodLocalized))
                binding.message = getString(R.string.id_insert_your_email_to_receive)
                binding.button = getString(R.string.id_continue)
            }
            TwoFactorSetupAction.SETUP_EMAIL -> {
                setToolbar(title = getString(R.string.id_1s_twofactor_setup, methodLocalized))
                binding.message = getString(R.string.id_use_your_email_to_receive)
                binding.button = getString(R.string.id_continue)
            }
            TwoFactorSetupAction.RESET -> {
                setToolbar(title = getString(R.string.id_request_twofactor_reset))
                binding.message = getString(R.string.id_resetting_your_twofactor_takes)
                binding.button = getString(R.string.id_request_twofactor_reset)
            }
            TwoFactorSetupAction.CANCEL -> {
                setToolbar(title = getString(R.string.id_cancel_2fa_reset))

                // Cancel action
                viewModel.cancel2FA(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            }
            TwoFactorSetupAction.DISPUTE -> {
                setToolbar(title = getString(R.string.id_dispute_twofactor_reset))
                binding.message = getString(R.string.id_if_you_did_not_request_the)
                binding.button = getString(R.string.id_dispute_twofactor_reset)
            }
            TwoFactorSetupAction.UNDO_DISPUTE -> {
                setToolbar(title = getString(R.string.id_undo_2fa_dispute))
                binding.message = getString(R.string.id_if_you_initiated_the_2fa_reset)
                binding.button = getString(R.string.id_undo_2fa_dispute)
            }
        }

        binding.buttonContinue.setOnClickListener {
            if(action == TwoFactorSetupAction.SETUP || action == TwoFactorSetupAction.SETUP_EMAIL){
                var data = ""
                when(viewModel.method){
                    TwoFactorMethod.SMS, TwoFactorMethod.PHONE, TwoFactorMethod.TELEGRAM -> {
                        data = viewModel.getPhoneNumberValue()
                    }
                    TwoFactorMethod.EMAIL -> {
                        data = viewModel.getEmailValue()
                    }
                    TwoFactorMethod.AUTHENTICATOR -> {
                        data = viewModel.authenticatorUrl ?: ""
                    }
                    TwoFactorMethod.AUTHENTICATOR -> {
                        data = viewModel.authenticatorUrl ?: ""
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
                }
            }
        }

        binding.authenticatorCode.setOnClickListener {
            copyToClipboard("Address", viewModel.authenticatorCode.value ?: "", requireContext())
            snackbar(R.string.id_copied_to_clipboard)
            binding.authenticatorCode.pulse()
        }

        binding.countryEditText.setOnFocusChangeListener { v, hasFocus ->
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
            event?.getContentIfNotHandledOrReturnNull()?.let {
                // Hint TwoFactorAuthenticationFragment / WalletSettingsFragment to update TwoFactorConfig
                setNavigationResult(result = true)
                popBackStack()
            }
        }
    }

    private fun popBackStack(){
        hideKeyboard() // hide keyboard as is no longer required for the backstacked fragments
        if(Bridge.useGreenModule){
            findNavController().popBackStack()
        }else{
            // Initiated from v4 codebase
            if(args.action == TwoFactorSetupAction.SETUP || args.action == TwoFactorSetupAction.SETUP_EMAIL){
                findNavController().popBackStack()
            }else{
                // Initiated from v3 codebase
                requireActivity().finish()
            }
        }
    }

    private fun openCountryFilter(){
        FilterBottomSheetDialogFragment().also {
            it.show(childFragmentManager, it.toString())
        }
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