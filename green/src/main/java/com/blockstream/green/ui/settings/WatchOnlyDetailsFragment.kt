package com.blockstream.green.ui.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemExtendedPublicKeyBinding
import com.blockstream.green.ui.QrBottomSheetDialogFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.ExtendedPublicKeyListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WatchOnlyDetailsFragment :
    WalletFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: WatchOnlyDetailsFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "WatchOnlyDetails"

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {

        binding.vm = viewModel

        val list = mutableListOf<GenericItem>()

        list += TextListItem(
            text = StringHolder(R.string.id_exports_an_accounts_extended_public_key),
            textTypeface = Typeface.BOLD,
            paddingTop = R.dimen.dp16,
            paddingBottom = R.dimen.dp16,
            paddingLeft = R.dimen.dp16,
            paddingRight = R.dimen.dp16
        )

        list += TextListItem(
            text = StringHolder(R.string.id_tip_you_can_use_the_xpub_ypub_zpub),
            textColor = R.color.color_on_surface_emphasis_low,
            paddingTop = R.dimen.dp0,
            paddingBottom = R.dimen.dp16,
            paddingLeft = R.dimen.dp16,
            paddingRight = R.dimen.dp16
        )

        list += TitleListItem(StringHolder(R.string.id_account_extended_public_keys))

        val accountsModelAdapter = ModelAdapter { model: SubAccount ->
            ExtendedPublicKeyListItem(
                name = StringHolder(model.nameOrDefault(getString(R.string.id_main_account))),
                extendedPublicKey = StringHolder("xpub6Bh31iozMXAFqryYyz39QTudkFC4rrhquBsokRCfLAn1YvzfprzdUTxYyPwVc4KTEUy2KPtUWdWwpvEHoz9GhZfW71REQuYwM1WfdQQHBt7")
            )
        }.observeList(viewLifecycleOwner, viewModel.getSubAccountsLiveData())


        val fastAdapter = FastAdapter.with(
            listOf(
                FastItemAdapter<GenericItem>().also {
                    it.set(list)
                },
                accountsModelAdapter,
            )
        )

        fastAdapter.addClickListener<ListItemExtendedPublicKeyBinding, GenericItem>({ binding -> binding.buttonQR }) { _, _, _, item ->
            if(item is ExtendedPublicKeyListItem){
                QrBottomSheetDialogFragment.open(
                    fragment = this,
                    title = getString(R.string.id_extended_public_key),
                    subtitle = item.name.getText(requireContext()),
                    content = item.extendedPublicKey.getText(requireContext()).toString(),
                )
            }
        }

        fastAdapter.addClickListener<ListItemExtendedPublicKeyBinding, GenericItem>({ binding -> binding.buttonCopy }) { _, _, _, item ->
            if(item is ExtendedPublicKeyListItem){
                copyToClipboard(
                    label = "Extended Public Key",
                    content = item.extendedPublicKey.getText(requireContext()).toString(),
                    showCopyNotification = true
                )
            }
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }
}
