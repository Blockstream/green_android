package com.blockstream.green.ui.transaction.details

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemTransactionHashBinding
import com.blockstream.green.databinding.ListItemTransactionNoteBinding
import com.blockstream.green.databinding.ListItemTransactionProgressBinding
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.share
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.getConfirmationsMax
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.looks.TransactionLook
import com.blockstream.green.ui.bottomsheets.AssetDetailsBottomSheetFragment
import com.blockstream.green.ui.bottomsheets.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuDataProvider
import com.blockstream.green.ui.bottomsheets.TransactionNoteBottomSheetDialogFragment
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.items.NoteListItem
import com.blockstream.green.ui.items.TransactionFeeListItem
import com.blockstream.green.ui.items.TransactionHashListItem
import com.blockstream.green.ui.items.TransactionProgressListItem
import com.blockstream.green.ui.items.TransactionUtxoListItem
import com.blockstream.green.ui.wallet.AbstractAccountWalletFragment
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.AlphaInAnimator
import com.pandulapeter.beagle.Beagle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsFragment : AbstractAccountWalletFragment<BaseRecyclerViewBinding>(
    layout = R.layout.base_recycler_view,
    menuRes = R.menu.menu_transaction_details
), MenuDataProvider {
    override val isAdjustResize: Boolean = true

    val args: TransactionDetailsFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "TransactionDetails"

    private val amountsAdapter: GenericFastItemAdapter = FastItemAdapter()
    private val detailsAdapter: GenericFastItemAdapter = FastItemAdapter()

    override val subtitle: String
        get() = args.transaction.account.name

    override val toolbarIcon: Int
        get() = args.transaction.network.getNetworkIcon()

    @Inject
    lateinit var beagle: Beagle

    @Inject
    lateinit var viewModelFactory: TransactionDetailsViewModel.AssistedFactory
    val viewModel: TransactionDetailsViewModel by viewModels {
        TransactionDetailsViewModel.provideFactory(
            viewModelFactory,
            args.wallet,
            args.transaction.account,
            args.transaction
        )
    }

    override val title: String
        get() = getString(
            when (args.transaction.txType) {
                Transaction.Type.OUT -> R.string.id_sent
                Transaction.Type.REDEPOSIT -> R.string.id_redeposited
                Transaction.Type.MIXED -> R.string.id_swap
                else -> R.string.id_received
            }
        )


    override fun getAccountWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        if(isDevelopmentOrDebug){
            args.transaction.toJson().also {
                logger.info { it }
                beagle.log(it)
            }
        }

        viewModel.transactionLiveData.observe(viewLifecycleOwner) {
            updateAdapter(it)
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                navigate(
                    TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToSendFragment(
                        wallet = wallet,
                        accountAsset = AccountAsset.fromAccount(account),
                        bumpTransaction = it.data as String
                    )
                )
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        val fastAdapter = FastAdapter.with(listOf(amountsAdapter, detailsAdapter))

        // Update transactions
        session.blockFlow(network).onEach {
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        fastAdapter.addClickListener<ListItemTransactionHashBinding, GenericItem>({ binding -> binding.buttonCopy }) { v, _, _, _ ->
            copyToClipboard(
                label = "Transaction ID",
                content = args.transaction.txHash,
                requireContext(),
                v
            )
            snackbar(R.string.id_copied_to_clipboard)
            countly.shareTransaction(session = session, account = account)
        }

        fastAdapter.addClickListener<ListItemTransactionHashBinding, GenericItem>({ binding -> binding.buttonExplorer }) { _, _, _, _ ->
            if (network.isLiquid) {
                MenuBottomSheetDialogFragment.show(requestCode = 1, title = getString(R.string.id_view_in_explorer), menuItems = listOf(
                    MenuListItem(icon = R.drawable.ic_regular_eye_slash_24, title = StringHolder(R.string.id_confidential)),
                    MenuListItem(icon = R.drawable.ic_regular_eye_24 , title = StringHolder(R.string.id_non_confidential)),
                ), fragmentManager = childFragmentManager)
            } else {
                openBrowser("${network.explorerUrl}${args.transaction.txHash}")
            }
        }

        fastAdapter.addClickListener<ListItemTransactionNoteBinding, GenericItem>({ binding -> binding.buttonEdit }) { _, _, _, _ ->
            TransactionNoteBottomSheetDialogFragment.show(note = viewModel.transactionNote, fragmentManager = childFragmentManager)
        }

        fastAdapter.addClickListener<ListItemTransactionProgressBinding, GenericItem>({ binding -> binding.buttonIncreaseFee }) { _, _, _, _ ->
            viewModel.bumpFee()
        }

        fastAdapter.onClickListener = { _, _, item: GenericItem, _: Int ->
            when (item) {
                is TransactionUtxoListItem -> {
                    item.txOutput?.assetId?.also {
                        AssetDetailsBottomSheetFragment.show(assetId = it, account = account, childFragmentManager)
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

    override fun onPrepareMenu(menu: Menu) {
        // No need to display "Go to Account" menu entry as we come from the Account
        menu.findItem(R.id.account).isVisible = args.account == null
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.share -> {
                if (network.isLiquid) {
                    MenuBottomSheetDialogFragment.show(requestCode = 2, title = getString(R.string.id_share), menuItems = listOf(
                        MenuListItem(icon = R.drawable.ic_regular_eye_slash_24, title = StringHolder(R.string.id_confidential_transaction)),
                        MenuListItem(icon = R.drawable.ic_regular_eye_24 , title = StringHolder(R.string.id_non_confidential_transaction)),
                        MenuListItem(icon = R.drawable.ic_baseline_text_snippet_24 , title = StringHolder(R.string.id_unblinding_data))
                    ), fragmentManager = childFragmentManager)
                } else {
                    share("${network.explorerUrl}${args.transaction.txHash}")
                    countly.shareTransaction(session = session, account = account, isShare = true)
                }
            }
            R.id.account -> {
                navigate(
                    TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToAccountOverviewFragment(
                        wallet = wallet,
                        account = args.transaction.account
                    )
                )
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    private fun updateAdapter(transaction: Transaction) {
        val look = TransactionLook(transaction, session)

        // Amounts
        var list = mutableListOf<GenericItem>()

        for (i in 0 until look.utxoSize) {
            list += TransactionUtxoListItem(index = i, session = session, look = look, hiddenAmount = true)
        }

        list += TransactionFeeListItem(session = session, look = look)

        FastAdapterDiffUtil.set(amountsAdapter.itemAdapter, list, true)

        // Generic elements
        list = mutableListOf()

        list += TransactionProgressListItem(
            transaction,
            transaction.getConfirmationsMax(session),
            transaction.network.confirmationsRequired
        )

        list += TransactionHashListItem(transaction)

        // Watch-only sessions don't provide notes
        if(!session.isWatchOnly) {
            list += NoteListItem(viewModel.transactionNote)
        }

        FastAdapterDiffUtil.set(detailsAdapter.itemAdapter, list, true)
    }

    companion object: KLogging()

    override fun menuItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        if(requestCode == 1) {
            val blinder = args.transaction.getUnblindedString().takeIf { position == 1 }.let {
                if (!it.isNullOrBlank()) "#blinded=$it" else ""
            }
            openBrowser("${network.explorerUrl}${args.transaction.txHash}$blinder")
        } else if(requestCode == 2){
            when (position) {
                0, 1 -> {
                    val blinder = args.transaction.getUnblindedString().takeIf { position == 1 }.let {
                        if (!it.isNullOrBlank()) "#blinded=$it" else ""
                    }

                    share("${network.explorerUrl}${args.transaction.txHash}$blinder")
                    countly.shareTransaction(session = session, account = account, isShare = true)
                }
                else -> {
                    share(args.transaction.getUnblindedData().toString())
                }
            }
        }
    }
}