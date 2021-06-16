package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.gdk.data.Settings
import com.blockstream.green.R
import com.blockstream.green.databinding.WalletSettingsFragmentBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.errorDialog
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TwoFactorExpirationFragment : WalletFragment<WalletSettingsFragmentBinding>(R.layout.wallet_settings_fragment, 0) {
    val args: WalletSettingsFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    private val itemAdapter = ItemAdapter<GenericItem>()

    private val radioPreferences by lazy {
        val titles = resources.getStringArray(R.array.csv_titles)
        val subtitles = resources.getStringArray(R.array.csv_subtitles)

        titles.mapIndexed { index, title ->
            PreferenceListItem(StringHolder(title), StringHolder(subtitles[index]), withRadio = true)
        }
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by navGraphViewModels(R.id.settings_nav_graph) {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.withSaveButton = true

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, item: GenericItem, _: Int ->
                viewModel.twoFactorConfigLiveData.value?.let {
                    if(radioPreferences.contains(item)){
                        radioPreferences.forEach { it.radioChecked = false }
                        (item as PreferenceListItem).radioChecked = true
                        notifyDataSetChanged()
                    }
                }
                true
            }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        binding.buttonSave.setOnClickListener {
            viewModel.settingsLiveData.value?.let { settings ->
                val selectedIndex = radioPreferences.indexOfFirst { it.radioChecked }
                if(selectedIndex > -1) {
                    val csvTime = session.network.csvBuckets[selectedIndex]
                    viewModel.setCsvTime(csvTime, DialogTwoFactorResolver(requireContext()))
                }
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

        viewModel.settingsLiveData.observe(viewLifecycleOwner) {
            updateAdapter(it)
        }
    }

    private fun updateAdapter(settings: Settings) {
        val list = mutableListOf<GenericItem>()

        list += HelpListItem(
            StringHolder(R.string.id_customize_2fa_expiration_of),
        )

        list += radioPreferences

        val selectedIndex = session.network.csvBuckets.indexOf(settings.csvTime)

        radioPreferences.forEachIndexed { index, radioPreference ->
            radioPreference.radioChecked = index == selectedIndex
        }

        FastAdapterDiffUtil.set(itemAdapter, list, false)
        notifyDataSetChanged()
    }

    override fun getWalletViewModel(): AbstractWalletViewModel? = viewModel

    private fun notifyDataSetChanged() {
        binding.recycler.adapter?.notifyDataSetChanged()
    }
}