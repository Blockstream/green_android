package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.gdk.data.Settings
import com.blockstream.green.R
import com.blockstream.green.data.Countries.COUNTRIES
import com.blockstream.green.data.Country
import com.blockstream.green.databinding.TwofactorEditFragmentBinding
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.FilterableDataProvider
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.utils.hideKeyboard
import com.blockstream.green.utils.openKeyboard
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
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

        binding.vm = viewModel

        binding.buttonAction.setOnClickListener {
            viewModel.enable2FA(args.method, binding.countryEditText.text.toString() + binding.phoneNumberEditText.text.toString(), DialogTwoFactorResolver(requireContext()))
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
            item.country.name.toLowerCase(Locale.getDefault()).contains(
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