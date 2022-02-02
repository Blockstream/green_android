package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemGenericDetailBinding
import com.blockstream.green.databinding.ListItemTransactionProgressBinding
import com.blockstream.green.gdk.getConfirmationsMax
import com.blockstream.green.ui.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.MenuDataProvider
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.looks.TransactionDetailsLook
import com.blockstream.green.ui.overview.AssetBottomSheetFragment
import com.blockstream.green.utils.*
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.AlphaInAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsFragment : WalletFragment<BaseRecyclerViewBinding>(
    layout = R.layout.base_recycler_view,
    menuRes = R.menu.menu_transaction_details
) {
    override val isAdjustResize: Boolean = true

    val args: TransactionDetailsFragmentArgs by navArgs()

    override val wallet by lazy { args.wallet }

    private val amountsAdapter: GenericFastItemAdapter = FastItemAdapter()
    private val detailsAdapter: GenericFastItemAdapter = FastItemAdapter()

    private val saveButtonEnabled = MutableLiveData(false)

    lateinit var noteListItem: GenericDetailListItem

    @Inject
    lateinit var viewModelFactory: TransactionDetailsViewModel.AssistedFactory
    val viewModel: TransactionDetailsViewModel by viewModels {
        TransactionDetailsViewModel.provideFactory(viewModelFactory, wallet, args.transaction)
    }

    override val title: String
        get() = getString(
            when (args.transaction.txType) {
                Transaction.Type.OUT -> R.string.id_sent
                Transaction.Type.REDEPOSIT -> R.string.id_redeposited
                else -> R.string.id_received
            }
        )

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        noteListItem = GenericDetailListItem(
            title = StringHolder(R.string.id_my_notes),
            liveContent = viewModel.editableNote,
            buttonText = StringHolder(R.string.id_save),
            enableButton = saveButtonEnabled
        )

        viewModel.editableNote.observe(viewLifecycleOwner) {
            saveButtonEnabled.postValue(it != viewModel.originalNote.value ?: "")
        }

        viewModel.transaction.observe(viewLifecycleOwner) {
            updateAdapter(it)
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                navigate(TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToSendFragment(
                    wallet = wallet,
                    bumpTransaction = it.data as String
                ))
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        val fastAdapter = FastAdapter.with(listOf(amountsAdapter, detailsAdapter))

        fastAdapter.addClickListener<ListItemTransactionProgressBinding, GenericItem>({ binding -> binding.buttonIncreaseFee }) { _, _, _, item ->
            viewModel.bumpFee()
        }

        fastAdapter.addClickListener<ListItemGenericDetailBinding, GenericItem>({ binding -> binding.button }) { _, i: Int, _: FastAdapter<GenericItem>, item: GenericItem ->
            if (item == noteListItem) {
                viewModel.saveNote()
                hideKeyboard()
            } else {
                if (session.isLiquid) {

                    MenuBottomSheetDialogFragment(object : MenuDataProvider {
                        override fun getTitle() = getString(R.string.id_view_in_explorer)
                        override fun getSubtitle() = null

                        override fun getMenuListItems() = listOf(
                            MenuListItem(icon = R.drawable.ic_baseline_visibility_off_24, title = StringHolder(R.string.id_confidential)),
                            MenuListItem(icon = R.drawable.ic_baseline_visibility_24 , title = StringHolder(R.string.id_non_confidential)),
                        )

                        override fun menuItemClicked(item: GenericItem, position: Int) {
                            val blinder = args.transaction.getUnblindedString().takeIf { position == 1 }.let {
                                if (!it.isNullOrBlank()) "#blinded=$it" else ""
                            }

                            share("${session.network.explorerUrl}${args.transaction.txHash}$blinder")
                        }
                    }).show(childFragmentManager)
                } else {
                    openBrowser("${session.network.explorerUrl}${args.transaction.txHash}")
                }
            }
        }

        fastAdapter.onClickListener = { _, _, item: GenericItem, position: Int ->
            when (item) {
                is TransactionAmountListItem -> {
                    AssetBottomSheetFragment.newInstance(item.assetId).also {
                        it.show(childFragmentManager, it.toString())
                    }
                }
            }

            true
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = AlphaInAnimator()
            adapter = fastAdapter
        }
    }

    override fun onPause() {
        super.onPause()
        // Hide keyboard if entered note is not saved
        hideKeyboard()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> {
                if (session.isLiquid) {
                    MenuBottomSheetDialogFragment(object : MenuDataProvider {
                        override fun getTitle() = getString(R.string.id_share)
                        override fun getSubtitle() = null

                        override fun getMenuListItems() = listOf(
                            MenuListItem(icon = R.drawable.ic_baseline_visibility_off_24, title = StringHolder(R.string.id_confidential_transaction)),
                            MenuListItem(icon = R.drawable.ic_baseline_visibility_24 , title = StringHolder(R.string.id_non_confidential_transaction)),
                            MenuListItem(icon = R.drawable.ic_baseline_text_snippet_24 , title = StringHolder(R.string.id_unblinding_data))
                        )

                        override fun menuItemClicked(item: GenericItem, position: Int) {
                            when (position) {
                                0, 1 -> {
                                    val blinder = args.transaction.getUnblindedString().takeIf { position == 1 }.let {
                                        if (!it.isNullOrBlank()) "#blinded=$it" else ""
                                    }

                                    share("${session.network.explorerUrl}${args.transaction.txHash}$blinder")
                                }
                                else -> {
                                    share(args.transaction.getUnblindedData().toString())
                                }
                            }
                        }
                    }).show(childFragmentManager)

                } else {
                    share("${session.network.explorerUrl}${args.transaction.txHash}")
                }

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateAdapter(
        transaction: Transaction,
    ) {
        val look = TransactionDetailsLook(session, transaction)

        // Amounts
        var list = mutableListOf<GenericItem>()

        for (i in 0 until look.assetSize) {
            list += TransactionAmountListItem(i, look)
        }

        list += TransactionFeeListItem(look)

        FastAdapterDiffUtil.set(amountsAdapter.itemAdapter, list, true)

        // Generic elements
        list = mutableListOf()

        list += TransactionProgressListItem(
            transaction,
            transaction.getConfirmationsMax(session),
            session.network.confirmationsRequired
        )

        list += TitleListItem(title = StringHolder(R.string.id_transaction_details))

        list += GenericDetailListItem(
            title = StringHolder(R.string.id_transaction_id),
            content = StringHolder(transaction.txHash),
            copyOnClick = true,
            buttonText = StringHolder(R.string.id_view_in_explorer)
        )

        // Watch-only sessions don't provide notes
        if(!session.isWatchOnly) {
            list += noteListItem
        }

        FastAdapterDiffUtil.set(detailsAdapter.itemAdapter, list, true)
    }

    override fun getWalletViewModel() = viewModel
}