package com.blockstream.green.ui.recovery

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.base.Urls
import com.blockstream.gdk.data.Credentials
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryPhraseFragmentBinding
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.items.RecoveryWordListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.AlphaInAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryPhraseFragment : AbstractWalletFragment<RecoveryPhraseFragmentBinding>(
    layout = R.layout.recovery_phrase_fragment,
    menuRes = 0
) {
    private val args: RecoveryPhraseFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "RecoveryPhrase"
    override val segmentation: HashMap<String, Any>? = null

    val credentials: Credentials
        get() = args.credentials ?: session.getCredentials()

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, args.wallet!!)
    }

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionAndWalletRequired(): Boolean = walletOrNull != null

    override fun isLoggedInRequired(): Boolean = isSessionAndWalletRequired()

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getAppViewModel(): AppViewModel? = if(walletOrNull != null) super.getAppViewModel() else null

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        // Note that if a wallet has a bip39 passphrase,
        // we should never show the mnemonic without the passphrase,
        // since adding (or removing) the bip39 passphrase changes the seed, thus the addresses and thus is an entirely different wallet.
        binding.passphrase = credentials.bip39Passphrase

        val mnemonic = credentials.mnemonic
        val words = mnemonic.split(" ")

        binding.buttonLearnMore.setOnClickListener {
            openBrowser(Urls.HELP_BIP39_PASSPHRASE)
        }

        binding.recoveryQR.setImageDrawable(BitmapDrawable(resources, createQrBitmap(mnemonic)).also { bitmap ->
            bitmap.isFilterBitmap = false
        })

        binding.buttonShowQR.setOnClickListener {
            binding.showQR = true
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
}