package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.WatchOnlyViewModel
import com.blockstream.common.views.wallet.WatchOnlyLook
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemOutputDescriptorsBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.ui.bottomsheets.QrBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.WatchOnlyBottomSheetDialogFragment
import com.blockstream.green.ui.items.OutputDescriptorListItem
import com.blockstream.green.ui.items.PreferenceListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WatchOnlyFragment :
    AppFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: WatchOnlyFragmentArgs by navArgs()

    val viewModel: WatchOnlyViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel


        val multisigTitleAdapter = FastItemAdapter<GenericItem>().also {
            it.set(
                listOf(
                    TitleListItem(
                        StringHolder(R.string.id_multisig),
                        iconLeft = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_multisig
                        )
                    )
                )
            )
        }

        val singlesigTitleAdapter = FastItemAdapter<GenericItem>().also {
            it.set(
                listOf(
                    TitleListItem(
                        StringHolder(R.string.id_singlesig),
                        iconLeft = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_singlesig
                        )
                    )
                )
            )
        }
        val extendedPublicKeyAdapter = FastItemAdapter<GenericItem>()
        val outputDescriptorsAdapter = FastItemAdapter<GenericItem>()


        val multisigModelAdapter = ModelAdapter { look: WatchOnlyLook ->
            PreferenceListItem(
                title = StringHolder(look.network!!.canonicalName),
                subtitle = StringHolder(if (look.username.isNullOrBlank()) {
                    getString(R.string.id_set_up_watchonly_credentials)
                } else {
                    getString(R.string.id_enabled_1s, look.username)
                })
            )
        }

        multisigModelAdapter.observeList(
            lifecycleScope,
            viewModel.multisigWatchOnly
        )

        val extendedPublicKeyModelAdapter = ModelAdapter { look: WatchOnlyLook ->
            OutputDescriptorListItem(
                look = look,
            )
        }

        val outputDescriptorsModelAdapter = ModelAdapter { look: WatchOnlyLook ->
            OutputDescriptorListItem(
                look = look
            )
        }

        val fastAdapter = FastAdapter.with(
            listOf(
                multisigTitleAdapter,
                multisigModelAdapter,
                singlesigTitleAdapter,
                extendedPublicKeyAdapter,
                extendedPublicKeyModelAdapter,
                // Disable Output Descriptor
//                 outputDescriptorsAdapter,
//                 outputDescriptorsModelAdapter,
            )
        )

        viewModel.multisigWatchOnly.onEach {
             multisigTitleAdapter.itemAdapter.active = it.isNotEmpty()
        }.launchIn(lifecycleScope)

        combine(viewModel.extendedPublicKeysAccounts, viewModel.outputDescriptorsAccounts) { a1, a2 ->
            a1.isEmpty() && a2.isEmpty()
        }.onEach {
            singlesigTitleAdapter.itemAdapter.active = !it
        }.launchIn(lifecycleScope)

        fastAdapter.onClickListener =
            { _: View?, adapter: IAdapter<GenericItem>, item: GenericItem, _: Int ->
                if (adapter == multisigModelAdapter) {
                    viewModel.multisigWatchOnly.value.getOrNull(adapter.getAdapterPosition(item))
                        ?.also {
                            WatchOnlyBottomSheetDialogFragment.show(
                                it.network!!,
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

        viewModel.extendedPublicKeysAccounts.onEach {
            (if (it.isNotEmpty()) {
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
            } else {
                listOf()
            }).also {
                extendedPublicKeyAdapter.set(it)
            }
        }.launchIn(lifecycleScope)

        extendedPublicKeyModelAdapter.observeList(
            lifecycleScope,
            viewModel.extendedPublicKeysAccounts
        )

        viewModel.outputDescriptorsAccounts.onEach {
            (if (it.isNotEmpty()) {
                listOf(
                    TextListItem(
                        text = StringHolder(R.string.id_output_descriptors),
                        textAppearance = R.style.TextAppearance_Green_TitleSmall,
                        paddingBottom = R.dimen.dp8,
                    )
                )

            } else {
                listOf()
            }).also {
                outputDescriptorsAdapter.set(it)
            }
        }.launchIn(lifecycleScope)

        outputDescriptorsModelAdapter.observeList(
            lifecycleScope,
            viewModel.outputDescriptorsAccounts
        )
    }
}
