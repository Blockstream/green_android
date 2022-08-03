package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.ListItemTransactionNoteBinding
import com.blockstream.green.databinding.ListItemTransactionOutputBinding
import com.blockstream.green.databinding.SendConfirmFragmentBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.errorSnackbar
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.looks.ConfirmTransactionLook
import com.blockstream.green.ui.bottomsheets.TransactionNoteBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.VerifyTransactionBottomSheetDialogFragment
import com.blockstream.green.ui.items.NoteListItem
import com.blockstream.green.ui.items.TransactionFeeListItem
import com.blockstream.green.ui.items.TransactionUtxoListItem
import com.blockstream.green.ui.overview.WalletOverviewFragment
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractAccountWalletFragment
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.views.GreenAlertView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.ncorti.slidetoact.SlideToActView
import com.pandulapeter.beagle.Beagle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SendConfirmFragment : AbstractAccountWalletFragment<SendConfirmFragmentBinding>(
    layout = R.layout.send_confirm_fragment,
    menuRes = R.menu.send_confirm
) {
    val args: SendConfirmFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "SendConfirm"
    override val segmentation
        get() = if (isSessionAndWalletRequired() && isSessionNetworkInitialized) countly.accountSegmentation(
            session = session,
            account = viewModel.account
        ) else null

    @Inject
    lateinit var beagle: Beagle

    @Inject
    lateinit var viewModelFactory: SendConfirmViewModel.AssistedFactory
    val viewModel: SendConfirmViewModel by viewModels {
        SendConfirmViewModel.provideFactory(viewModelFactory, args.wallet, args.account, args.transactionSegmentation)
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun getAccountWalletViewModel() = viewModel

    private val transactionOrNull get() = session.pendingTransaction?.second
    private val transaction get() = transactionOrNull!!

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        if(session.pendingTransaction == null){
            popBackStack()
            return
        }

        if(isDevelopmentOrDebug){
            session.pendingTransaction!!.second.toJson().also {
                logger.info { it }
                beagle.log(it)
            }
        }

        viewModel.deviceAddressValidationEvent.observe(viewLifecycleOwner){
            if(it.peekContent() == null){ // open if null
                VerifyTransactionBottomSheetDialogFragment.show(childFragmentManager)
            }
        }
        val noteAdapter = FastItemAdapter<GenericItem>()

        viewModel.transactionNoteLiveData.observe(viewLifecycleOwner){
            if(!transaction.isSweep && !it.isNullOrBlank()) {
                noteAdapter.set(
                    listOf(
                        NoteListItem(viewModel.transactionNote)
                    )
                )
            }else{
                noteAdapter.set(listOf())
            }

            invalidateMenu()
        }

        val fastAdapter = FastAdapter.with(listOf(createAdapter(isAddressVerificationOnDevice = false), noteAdapter))

        fastAdapter.addClickListener<ListItemTransactionNoteBinding, GenericItem>({ binding -> binding.buttonEdit }) { _, _, _, _ ->
            TransactionNoteBottomSheetDialogFragment.show(note = viewModel.transactionNote, fragmentManager = childFragmentManager)
        }

        fastAdapter.addClickListener<ListItemTransactionOutputBinding, GenericItem>({ binding -> binding.addressTextView }) { v, _: Int, _: FastAdapter<GenericItem>, _: GenericItem ->
            copyToClipboard("Address", (v as TextView).text.toString(), animateView = v)
            snackbar(R.string.id_address_copied_to_clipboard)
        }

        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.buttonSend.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener{
            override fun onSlideComplete(view: SlideToActView) {
                viewModel.broadcastTransaction(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                (it.data as? Boolean)?.let { isSendAll ->
                    setNavigationResult(result = isSendAll, key = WalletOverviewFragment.BROADCASTED_TRANSACTION, destinationId = R.id.walletOverviewFragment)
                }
                snackbar(R.string.id_transaction_sent)
                findNavController().popBackStack(R.id.walletOverviewFragment, false)
            }
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                // Reset send slider
                binding.buttonSend.resetSlider()

                when {
                    // If the error is the Anti-Exfil validation violation we show that prominently.
                    // Otherwise show a toast of the error text.
                    throwable.message == "id_signature_validation_failed_if" -> {
                        errorDialog(throwable)
                    }
                    throwable.message == "id_transaction_already_confirmed" -> {
                        snackbar(R.string.id_transaction_already_confirmed)
                        findNavController().popBackStack(R.id.walletOverviewFragment, false)
                    }
                    throwable.message != "id_action_canceled" -> {
                        errorSnackbar(throwable)
                    }
                }
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.add_note).isVisible = transactionOrNull?.isSweep == false && viewModel.transactionNote.isBlank()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_note -> {
                TransactionNoteBottomSheetDialogFragment.show(note = viewModel.transactionNote, fragmentManager = childFragmentManager)
                return true
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    fun createAdapter(isAddressVerificationOnDevice: Boolean): ItemAdapter<GenericItem> {
        val look = ConfirmTransactionLook(
            network = network,
            session = session,
            transaction = transaction,
            showChangeOutputs = isAddressVerificationOnDevice && (session.device?.isLedger == true || (session.device?.isTrezor == true && session.hwWallet?.model != "Trezor One" && network.isMultisig)),
            isAddressVerificationOnDevice = isAddressVerificationOnDevice
        )


        val itemAdapter = ItemAdapter<GenericItem>()
        val list = mutableListOf<GenericItem>()

        for (i in 0 until look.utxoSize) {
            list += TransactionUtxoListItem(
                index = i,
                session = session,
                look = look,
                withStroke = isAddressVerificationOnDevice
            )
        }

        list += TransactionFeeListItem(session = session, look = look, confirmLook = look)

        itemAdapter.add(list)
        return itemAdapter
    }
}