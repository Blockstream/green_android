package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.extensions.toggle
import com.blockstream.common.models.add.ChooseAccountTypeViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseAccountTypeFragmentBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.ui.bottomsheets.EnrichedAssetsBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.EnrichedAssetsListener
import com.blockstream.green.ui.items.AccountTypeListItem
import com.blockstream.green.ui.jade.JadeQRFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChooseAccountTypeFragment : AbstractAddAccountFragment<ChooseAccountTypeFragmentBinding>(
    R.layout.choose_account_type_fragment, 0
), EnrichedAssetsListener {
    val args: ChooseAccountTypeFragmentArgs by navArgs()

    override val assetId: String?
        get() = args.asset?.assetId

    override val viewModel: ChooseAccountTypeViewModel by viewModel {
        parametersOf(args.wallet, args.asset)
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is SideEffects.NavigateTo -> {
                (sideEffect.destination as? NavigateDestinations.ExportLightningKey)?.also {
                    navigate(
                        ChooseAccountTypeFragmentDirections.actionGlobalJadeQrFragment(
                            wallet = args.wallet,
                            isLightningMnemonicExport = true
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.AddAccount2of3)?.also {
                    navigate(
                        ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToAccount2of3Fragment(
                            setupArgs = it.setupArgs,
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ReviewAddAccount)?.also {
                    navigate(
                        ChooseAccountTypeFragmentDirections.actionGlobalReviewAddAccountFragment(
                            setupArgs = it.setupArgs,
                        )
                    )
                }
            }

            is ChooseAccountTypeViewModel.LocalSideEffects.ExperimentalFeaturesDialog -> {
                dialog(
                    title = R.string.id_experimental_feature,
                    message = R.string.id_experimental_features_might,
                    icon = R.drawable.ic_fill_flask_24,
                    listener = {
                        viewModel.postEvent(sideEffect.event)
                    }
                )

            }

            is ChooseAccountTypeViewModel.LocalSideEffects.ArchivedAccountDialog -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_archived_account)
                    .setMessage(R.string.id_there_is_already_an_archived)
                    .setPositiveButton(R.string.id_continue) { _, _ ->
                        viewModel.postEvent(sideEffect.event)
                    }
                    .setNeutralButton(R.string.id_archived_accounts) { _, _ ->
                        navigate(NavGraphDirections.actionGlobalArchivedAccountsFragment(wallet = viewModel.greenWallet, navigateToOverview = true))
                    }
                    .show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        getNavigationResult<String>(JadeQRFragment.MNEMONIC_RESULT)?.observe(viewLifecycleOwner) { mnemonic ->
            if (mnemonic != null) {
                clearNavigationResult(JadeQRFragment.MNEMONIC_RESULT)
                viewModel.postEvent(
                    ChooseAccountTypeViewModel.LocalEvents.CreateLightningAccount(
                        mnemonic
                    )
                )
            }
        }

        viewModel.asset.onEach {
            binding.asset.bind(scope = lifecycleScope, asset = it, session = viewModel.session, showEditIcon = true)
        }.launchIn(lifecycleScope)

        binding.assetMaterialCardView.setOnClickListener {
            EnrichedAssetsBottomSheetDialogFragment.show(fragmentManager = childFragmentManager)
        }

        binding.buttonAdvanced.setOnClickListener {
            viewModel.isShowingAdvancedOptions.toggle()
        }

        val fastAdapter = FastItemAdapter<GenericItem>()

        fastAdapter.onClickListener = { _, _, item: GenericItem, _: Int ->
            if (item is AccountTypeListItem) {
                viewModel.postEvent(ChooseAccountTypeViewModel.LocalEvents.ChooseAccountType(item.accountTypeLook.accountType))
            }
            false
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        viewModel.accountTypes.onEach {
            FastAdapterDiffUtil.set(adapter = fastAdapter.itemAdapter, items = it.map { AccountTypeListItem(it) }, detectMoves = true)
        }.launchIn(lifecycleScope)

        viewModel.onProgress.onEach {
            binding.mainContainer.alpha = if(it) 0.15f else 1.0f
        }.launchIn(lifecycleScope)
    }

    override fun assetClicked(asset: EnrichedAsset) {
        viewModel.asset.value = asset
    }
}
