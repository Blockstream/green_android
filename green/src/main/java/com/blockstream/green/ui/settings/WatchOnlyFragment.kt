package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.data.Account
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemOutputDescriptorsBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.ui.bottomsheets.QrBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.WatchOnlyBottomSheetDialogFragment
import com.blockstream.green.ui.items.OutputDescriptorListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class WatchOnlyFragment :
    AbstractWalletFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: WatchOnlyFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "WalletSettingsWatchOnly"

    @Inject
    lateinit var viewModelFactory: WatchOnlyViewModel.AssistedFactory
    val viewModel: WatchOnlyViewModel by viewModels {
        WatchOnlyViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    private lateinit var bitcoinMultisigPreference: PreferenceListItem
    private lateinit var liquidMultisigPreference: PreferenceListItem

    private lateinit var fastAdapter: FastAdapter<GenericItem>

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {

        binding.vm = viewModel

        val list = mutableListOf<GenericItem>()

        val extendedPublicKeyAdapter = FastItemAdapter<GenericItem>()
        val outputDescriptorsAdapter = FastItemAdapter<GenericItem>()

        bitcoinMultisigPreference = PreferenceListItem(
            StringHolder("Bitcoin"),
        )

        liquidMultisigPreference = PreferenceListItem(
            StringHolder("Liquid"),
        )

        val extendedPublicKeyModelAdapter = ModelAdapter { account: Account ->
            OutputDescriptorListItem(
                account = account,
                isOutputDescriptor = false
            )
        }

        val outputDescriptorsModelAdapter = ModelAdapter { account: Account ->
            OutputDescriptorListItem(
                account = account
            )
        }

        if (session.activeMultisig.isNotEmpty()) {
            list += TitleListItem(
                StringHolder(R.string.id_multisig),
                iconLeft = ContextCompat.getDrawable(requireContext(), R.drawable.ic_multisig)
            )

            if (session.activeBitcoinMultisig != null) {
                list += bitcoinMultisigPreference

                viewModel.watchOnlyUsernameLiveData(session.activeBitcoinMultisig!!).observe(viewLifecycleOwner) {
                    bitcoinMultisigPreference.subtitle = StringHolder(
                        if (it.isNullOrBlank()) {
                            getString(R.string.id_set_up_watchonly_credentials)
                        } else {
                            getString(R.string.id_enabled_1s, it)
                        }
                    )

                    fastAdapter.getItemById(bitcoinMultisigPreference.identifier)?.let {
                        it.second?.let { it1 -> fastAdapter.notifyAdapterItemChanged(it1) }
                    }
                }
            }

            if (session.activeLiquidMultisig != null) {
                list += liquidMultisigPreference

                viewModel.watchOnlyUsernameLiveData(session.activeLiquidMultisig!!).observe(viewLifecycleOwner) {
                    liquidMultisigPreference.subtitle = StringHolder(
                        if (it.isNullOrBlank()) {
                            getString(R.string.id_set_up_watchonly_credentials)
                        } else {
                            getString(R.string.id_enabled_1s, it)
                        }
                    )

                    fastAdapter.getItemById(liquidMultisigPreference.identifier)?.let {
                        it.second?.let { it1 -> fastAdapter.notifyAdapterItemChanged(it1) }
                    }
                }
            }
        }

        if (session.activeSinglesig.isNotEmpty()) {

            list += TitleListItem(
                StringHolder(R.string.id_singlesig),
                iconLeft = ContextCompat.getDrawable(requireContext(), R.drawable.ic_singlesig)
            )

            viewModel.extendedPublicKeysAccounts.onEach {
                if (it.isNotEmpty()) {
                    extendedPublicKeyAdapter.set(
                        listOf(
                            TextListItem(
                                text = StringHolder(R.string.id_extended_public_keys),
                                textAppearance = R.style.TextAppearance_Green_TitleSmall,
                                paddingBottom = R.dimen.dp0,
                            ),
                            TextListItem(
                                text = StringHolder(R.string.id_tip_you_can_use_the),
                                textColor = R.color.color_on_surface_emphasis_low,
                                textAppearance = R.style.TextAppearance_Green_BodySmall,
                                paddingTop = R.dimen.dp0,
                                paddingBottom = R.dimen.dp8,
                                paddingLeft = R.dimen.dp16,
                                paddingRight = R.dimen.dp16
                            )
                        )
                    )
                }
            }.launchIn(lifecycleScope)

            extendedPublicKeyModelAdapter.observeList(
                lifecycleScope,
                viewModel.extendedPublicKeysAccounts
            )

            viewModel.outputDescriptorsAccounts.onEach {
                if (it.isNotEmpty()) {
                    outputDescriptorsAdapter.set(
                        listOf(
                            TextListItem(
                                text = StringHolder(R.string.id_output_descriptors),
                                textAppearance = R.style.TextAppearance_Green_TitleSmall,
                                paddingBottom = R.dimen.dp8,
                            )
                        )
                    )
                }
            }.launchIn(lifecycleScope)

            outputDescriptorsModelAdapter.observeList(
                lifecycleScope,
                viewModel.outputDescriptorsAccounts
            )

        }

        fastAdapter = FastAdapter.with(
            listOf(
                FastItemAdapter<GenericItem>().also {
                    it.set(list)
                },
                extendedPublicKeyAdapter,
                extendedPublicKeyModelAdapter,
                // Disable Output Descriptor
                // outputDescriptorsAdapter,
                // outputDescriptorsModelAdapter,
            )
        )

        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<GenericItem>, iItem: GenericItem, _: Int ->
                when (iItem) {
                    bitcoinMultisigPreference -> {
                        WatchOnlyBottomSheetDialogFragment.show(
                            session.activeBitcoinMultisig!!,
                            childFragmentManager
                        )
                    }

                    liquidMultisigPreference -> {
                        WatchOnlyBottomSheetDialogFragment.show(
                            session.activeLiquidMultisig!!,
                            childFragmentManager
                        )
                    }
                }

                true
            }

        fastAdapter.addClickListener<ListItemOutputDescriptorsBinding, GenericItem>({ binding -> binding.buttonQR }) { _, _, _, item ->
            if (item is OutputDescriptorListItem) {
                QrBottomSheetDialogFragment.show(
                    title = getString(if (item.isOutputDescriptor) R.string.id_output_descriptors else R.string.id_extended_public_key),
                    subtitle = item.account.name,
                    content = (if (item.isOutputDescriptor) item.account.outputDescriptors else item.account.extendedPubkey)
                        ?: "",
                    fragmentManager = childFragmentManager
                )
            }
        }

        fastAdapter.addClickListener<ListItemOutputDescriptorsBinding, GenericItem>({ binding -> binding.buttonCopy }) { _, _, _, item ->
            if (item is OutputDescriptorListItem) {
                copyToClipboard(
                    label = getString(if (item.isOutputDescriptor) R.string.id_output_descriptors else R.string.id_extended_public_key),
                    content = (if (item.isOutputDescriptor) item.account.outputDescriptors else item.account.extendedPubkey)
                        ?: "",
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
