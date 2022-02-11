package com.blockstream.green.ui.recovery

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryPhraseFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.RecoveryWordListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.createQrBitmap
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
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

    override val screenName = "RecoveryPhrase"
    override val segmentation: HashMap<String, Any>? = null

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, wallet)
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val mnemonic = session.getMnemonicPassphrase()
        val words = mnemonic.split(" ")

        binding.recoveryQR.setImageDrawable(BitmapDrawable(resources, createQrBitmap(mnemonic)).also { bitmap ->
            bitmap.isFilterBitmap = false
        })

        binding.buttonShowQR.setOnClickListener {
            binding.buttonShowQR.isInvisible = true
            binding.recoveryQR.isVisible = true
            binding.materialCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

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