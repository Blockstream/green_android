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
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TwoFactorSetupFragment : WalletFragment<TwofactorSetupFragmentBinding>(R.layout.twofactor_setup_fragment, 0),
    FilterableDataProvider {
    val args: TwoFactorSetupFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    override val isAdjustResize: Boolean = true

    @Inject
    lateinit var viewModelFactory: TwoFactorSetupViewModel.AssistedFactory
    val viewModel: TwoFactorSetupViewModel by viewModels {
        TwoFactorSetupViewModel.provideFactory(viewModelFactory, args.wallet, args.method)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val methodLocalized = localized2faMethod(args.method.gdkType)

        setToolbar(title = getString(R.string.id_1s_twofactor_set_up, methodLocalized))

        binding.vm = viewModel
        binding.buttonContinue.setOnClickListener {
            var data = ""
            when(viewModel.method){
                TwoFactorMethod.SMS, TwoFactorMethod.PHONE -> {
                    data = viewModel.getPhoneNumberValue()
                }
                TwoFactorMethod.EMAIL -> {
                    data = viewModel.getEmailValue()
                }
                TwoFactorMethod.AUTHENTICATOR -> {
                    data = viewModel.authenticatorUrl ?: ""
                }
            }
            // setupEmail is used only to setup the email address for recovery transactions legacy option
            viewModel.enable2FA(args.method, data = data, enabled = !args.setupEmail, twoFactorResolver = DialogTwoFactorResolver(requireContext()))
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
                errorDialog(it)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {

                // Hint TwoFactorAuthenticationFragment / WalletSettingsFragment to update TwoFactorConfig
                setNavigationResult(result = true)

                hideKeyboard() // hide keyboard as is no longer required for the backstacked fragments
                findNavController().popBackStack()
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