package com.blockstream.green.ui.recovery

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryPhraseFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.ui.dialogs.QrDialogFragment
import com.blockstream.green.ui.items.RecoveryWordListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.alphaPulse
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.AlphaInAnimator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryPhraseFragment : AppFragment<RecoveryPhraseFragmentBinding>(
    layout = R.layout.recovery_phrase_fragment,
    menuRes = 0
) {
    private val args: RecoveryPhraseFragmentArgs by navArgs()

    override val screenName = "RecoveryPhrase"
    override val segmentation: HashMap<String, Any>? = null

    override val subtitle: String?
        get() = if(args.isLightning) getString(R.string.id_lightning) else null

    val viewModel: RecoveryPhraseViewModel by viewModel {
        parametersOf(args.isLightning, args.credentials, args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        // Note that if a wallet has a bip39 passphrase,
        // we should never show the mnemonic without the passphrase,
        // since adding (or removing) the bip39 passphrase changes the seed, thus the addresses and thus is an entirely different wallet.

        binding.buttonLearnMore.setOnClickListener {
            openBrowser(Urls.HELP_BIP39_PASSPHRASE)
        }

        binding.buttonShowQR.setOnClickListener {
            viewModel.postEvent(RecoveryPhraseViewModel.LocalEvents.ShowQR)
        }

        if (args.isLightning) {
            binding.lightning.alphaPulse(true)
        }

        val itemAdapter = ItemAdapter<RecoveryWordListItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        binding.recycler.apply {
            itemAnimator = AlphaInAnimator()
            adapter = fastAdapter
        }

        viewModel.showQR.onEach {
            if(it){
                binding.materialCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        }.launchIn(lifecycleScope)

        viewModel.mnemonic.onEach {
            createQrBitmap(it)?.also { bitmap ->
                binding.recoveryQR.setImageDrawable(BitmapDrawable(resources, bitmap).also {
                    it.isFilterBitmap = false
                })

                binding.recoveryQR.setOnLongClickListener {
                    QrDialogFragment.show(bitmap, childFragmentManager)
                    true
                }
            }
        }.launchIn(lifecycleScope)

        viewModel.mnemonicWords.onEach {
            val listItems = it.mapIndexed { index, word ->
                RecoveryWordListItem(index = index + 1, StringHolder(word))
            }

            itemAdapter.set(listItems)
        }.launchIn(lifecycleScope)
    }
}