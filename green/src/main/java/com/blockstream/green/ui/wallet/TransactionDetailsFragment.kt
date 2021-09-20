package com.blockstream.green.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemGenericDetailBinding
import com.blockstream.green.databinding.ListItemTransactionProgressBinding
import com.blockstream.green.gdk.getConfirmationsMax
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.looks.TransactionDetailsLook
import com.blockstream.green.utils.*
import com.greenaddress.greenbits.ui.send.SendAmountActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import com.mikepenz.itemanimators.AlphaInAnimator
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransactionDetailsFragment : WalletFragment<BaseRecyclerViewBinding>(
    layout = R.layout.base_recycler_view,
    menuRes = 0
) {
    override val isAdjustResize: Boolean = true

    val args: TransactionDetailsFragmentArgs by navArgs()

    override val wallet by lazy { args.wallet }

    private val amountsAdapter: GenericFastItemAdapter = FastItemAdapter()
    private val detailsAdapter: GenericFastItemAdapter = FastItemAdapter()

    private val saveButtonEnabled = MutableLiveData(false)

    lateinit var noteListItem : GenericDetailListItem

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

        viewModel.editableNote.observe(viewLifecycleOwner){
            saveButtonEnabled.postValue(it != viewModel.originalNote.value ?: "")
        }

        viewModel.transaction.observe(viewLifecycleOwner) {
            updateAdapter(it)
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledOrReturnNull()?.let {
                val intent = Intent(requireContext(), SendAmountActivity::class.java)
                startActivity(intent)
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

        fastAdapter.addClickListener<ListItemGenericDetailBinding, GenericItem>({ binding -> binding.button }) { _, i: Int, fastAdapter: FastAdapter<GenericItem>, item: GenericItem ->
            if(item == noteListItem){
                viewModel.saveNote()
                hideKeyboard()
            }else{
                openBrowser("${session.network.explorerUrl}${args.transaction.txHash}")
            }
        }

        fastAdapter.onClickListener = { _, _, item: GenericItem, position: Int ->
            when(item){
                is TransactionAmountListItem -> {
                    val assetId = item.look.assets[item.index].first
                    if(assetId != session.policyAsset){
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

    private fun updateAdapter(
        transaction: Transaction,
    ) {
        val look = TransactionDetailsLook(session, transaction)

        // Amounts
        var list = mutableListOf<GenericItem>()

        for (i in 0 until look.assetSize) {
            list += TransactionAmountListItem(transaction, i, look)
        }

        if (transaction.isOut || transaction.isRedeposit) {
            list += TransactionFeeListItem(transaction, look)
        }

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
        if(confirmations > session.network.confirmationsRequired && transaction.spv.disabledOrVerified()) {
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

        list += noteListItem

        FastAdapterDiffUtil.set(detailsAdapter.itemAdapter, list, true)
    }

    override fun getWalletViewModel() = viewModel
}