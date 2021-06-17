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
class TwoFactorEditFragment : WalletFragment<TwofactorEditFragmentBinding>(R.layout.twofactor_edit_fragment, 0),
    FilterableDataProvider {
    val args: TwoFactorEditFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }


    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by navGraphViewModels(R.id.settings_nav_graph) {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val method = args.method
        val methodLocalized = localized2faMethod(method.gdkType)

        setToolbar(title = getString(R.string.id_1s_twofactor_set_up, methodLocalized))

        binding.vm = viewModel
        binding.method = method
        binding.title = getString(R.string.id_please_provide_your_1s, method)

        binding.buttonContinue.setOnClickListener {
            var data = ""
            when(method){
                TwoFactorMethod.SMS, TwoFactorMethod.PHONE -> {
                    data = binding.countryEditText.text.toString() + binding.phoneNumberEditText.text.toString()
                }
                TwoFactorMethod.EMAIL -> {
                    data = binding.emailEditText.text.toString()
                }
                TwoFactorMethod.AUTHENTICATOR -> TODO()
            }
            viewModel.enable2FA(args.method, data, DialogTwoFactorResolver(requireContext()))
        }

        binding.countryEditText.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                openCountryFilter()
            }
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