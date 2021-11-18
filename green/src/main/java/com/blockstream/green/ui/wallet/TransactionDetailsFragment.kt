package com.blockstream.green.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MenuRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemGenericDetailBinding
import com.blockstream.green.databinding.ListItemTransactionAmountBinding
import com.blockstream.green.databinding.ListItemTransactionProgressBinding
import com.blockstream.green.gdk.getConfirmationsMax
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.looks.TransactionDetailsLook
import com.blockstream.green.utils.*
import com.greenaddress.greenbits.ui.send.SendAmountActivity
import com.kennyc.bottomsheet.BottomSheetListener
import com.kennyc.bottomsheet.BottomSheetMenuDialogFragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.AlphaInAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.JsonElement
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

    private val startForResultFeeBump = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            popBackStack()
        }
    }

    @Inject
    lateinit var viewModelFactory: TransactionDetailsViewModel.AssistedFactory
    val viewModel: TransactionDetailsViewModel by viewModels {
        TransactionDetailsViewModel.provideFactory(viewModelFactory, wallet, args.transaction)
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {

        noteListItem = GenericDetailListItem(
            title = StringHolder(R.string.id_my_notes),
            liveContent = viewModel.editableNote,
            buttonText = StringHolder(R.string.id_save),
            enableButton = saveButtonEnabled
        )

        viewModel.getSubAccountLiveData().observe(viewLifecycleOwner) {
            setToolbar(
                title = getString(
                    when (args.transaction.txType) {
                        Transaction.Type.OUT -> R.string.id_sent
                        Transaction.Type.REDEPOSIT -> R.string.id_redeposited
                        else -> R.string.id_received
                    }
                ), subtitle = it.nameOrDefault(getString(R.string.id_main_account))
            )
        }

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

        fastAdapter.addClickListener<ListItemGenericDetailBinding, GenericItem>({ binding -> binding.button }) { view, i: Int, fastAdapter: FastAdapter<GenericItem>, item: GenericItem ->
            if (item == noteListItem) {
                viewModel.saveNote()
                hideKeyboard()
            } else {
                if (session.isLiquid) {
                    showPopupMenu(view, R.menu.menu_transaction_details_explorer_liquid) { item ->
                        when (item.itemId) {
                            R.id.explorer_confidential -> {
                                openBrowser("${session.network.explorerUrl}${args.transaction.txHash}")
                            }
                            R.id.explorer_non_confidential -> {
                                val blinder = args.transaction.getUnblindedString().let {
                                    if (it.isNotBlank()) "#blinded=$it" else ""
                                }

                                openBrowser("${session.network.explorerUrl}${args.transaction.txHash}$blinder")
                            }
                        }
                        true
                    }
                } else {
                    openBrowser("${session.network.explorerUrl}${args.transaction.txHash}")
                }
            }
        }

        fastAdapter.addClickListener<ListItemTransactionAmountBinding, GenericItem>({ binding -> binding.addressTextView }) { view, i: Int, fastAdapter: FastAdapter<GenericItem>, item: GenericItem ->
            copyToClipboard("Address", (view as TextView).text.toString(), animateView = view)
            snackbar(R.string.id_address_copied_to_clipboard)
        }

        fastAdapter.onClickListener = { _, _, item: GenericItem, position: Int ->
            when (item) {
                is TransactionAmountListItem -> {
                    val assetId = item.look.assets[item.index].first
                    if (assetId != session.policyAsset) {
                        navigate(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToAssetBottomSheetFragment(
                                assetId = assetId,
                                wallet = wallet
                            )
                        )
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> {
                if (session.isLiquid) {
                    showMenu(R.menu.menu_transaction_details_share_liquid)
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
            list += TransactionAmountListItem(transaction, i, look)
        }

        list += TransactionFeeListItem(transaction, look)

        FastAdapterDiffUtil.set(amountsAdapter.itemAdapter, list, true)

        // Generic elements
        list = mutableListOf()

        list += TransactionProgressListItem(
            transaction,
            transaction.getConfirmationsMax(session),
            session.network.confirmationsRequired
        )

        list += TitleListItem(title = StringHolder(R.string.id_transaction_details))
        val confirmations = transaction.getConfirmations(session.blockHeight)
        if (confirmations > session.network.confirmationsRequired && transaction.spv.disabledOrVerified()) {
            list += GenericDetailListItem(
                title = StringHolder(R.string.id_confirmations),
                content = StringHolder("$confirmations")
            )
        }
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

    private fun showMenu(@MenuRes menuRes: Int) {
        BottomSheetMenuDialogFragment.Builder(
            context = requireContext(),
            style = R.style.Green_BottomSheetMenuDialog,
            sheet = menuRes,
            listener = object : BottomSheetListener {
                override fun onSheetDismissed(
                    bottomSheet: BottomSheetMenuDialogFragment,
                    `object`: Any?,
                    dismissEvent: Int
                ) {

                }

                override fun onSheetItemSelected(
                    bottomSheet: BottomSheetMenuDialogFragment,
                    item: MenuItem,
                    `object`: Any?
                ) {

                    when (item.itemId) {
                        R.id.non_confidential_transaction -> {
                            val blinder = args.transaction.getUnblindedString().let {
                                if (it.isNotBlank()) "#blinded=$it" else ""
                            }

                            share("${session.network.explorerUrl}${args.transaction.txHash}$blinder")
                        }
                        R.id.confidential_transaction -> {
                            share("${session.network.explorerUrl}${args.transaction.txHash}")
                        }
                        R.id.unblinding_data -> {
                            share(args.transaction.getUnblindedData().toString())
                        }
                    }
                }

                override fun onSheetShown(
                    bottomSheet: BottomSheetMenuDialogFragment,
                    `object`: Any?
                ) {

                }

            },
            title = getString(R.string.id_share),
        ).show(childFragmentManager)
    }

    override fun getWalletViewModel() = viewModel
}