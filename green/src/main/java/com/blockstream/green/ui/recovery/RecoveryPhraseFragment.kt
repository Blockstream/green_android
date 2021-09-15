package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.Countries
import com.blockstream.green.data.Country
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.databinding.RecoveryPhraseFragmentBinding
import com.blockstream.green.databinding.RecoverySetupWordsFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.items.RecoveryWordListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.AlphaInAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryPhraseFragment : WalletFragment<RecoveryPhraseFragmentBinding>(
    layout = R.layout.recovery_phrase_fragment,
    menuRes = 0
) {
    private val args: RecoveryPhraseFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet!! }

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, wallet)
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val words = session.getMnemonicPassphrase().split(" ")

        val listItems = words.mapIndexed { index, word ->
            RecoveryWordListItem(index = index + 1, StringHolder(word))
        }

        val itemAdapter = ItemAdapter<RecoveryWordListItem>()
            .add(listItems)

        val fastAdapter = FastAdapter.with(itemAdapter)

        binding.recycler.apply {
            itemAnimator = AlphaInAnimator()
            adapter = fastAdapter
        }
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel
}