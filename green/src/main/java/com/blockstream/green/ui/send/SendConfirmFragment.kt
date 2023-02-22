package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import breez_sdk.SuccessActionProcessed
import com.blockstream.gdk.data.SendTransactionSuccess
import com.blockstream.green.R
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.ListItemTransactionNoteBinding
import com.blockstream.green.databinding.ListItemTransactionOutputBinding
import com.blockstream.green.databinding.SendConfirmFragmentBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.looks.ConfirmTransactionLook
import com.blockstream.green.ui.bottomsheets.NoteBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.VerifyTransactionBottomSheetDialogFragment
import com.blockstream.green.ui.items.NoteListItem
import com.blockstream.green.ui.items.TransactionFeeListItem
import com.blockstream.green.ui.items.TransactionUtxoListItem
import com.blockstream.green.ui.overview.WalletOverviewFragment
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractAccountWalletFragment
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.GreenAlertView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

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
                        NoteListItem(note = viewModel.transactionNote, isEditable = !account.isLightning)
                    )
                )
            }else{
                noteAdapter.set(listOf())
            }

            invalidateMenu()
        }

        val fastAdapter = FastAdapter.with(listOf(createAdapter(isAddressVerificationOnDevice = false), noteAdapter))

        fastAdapter.addClickListener<ListItemTransactionNoteBinding, GenericItem>({ binding -> binding.buttonEdit }) { _, _, _, _ ->
            NoteBottomSheetDialogFragment.show(note = viewModel.transactionNote, fragmentManager = childFragmentManager)
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
                viewModel.signTransaction(broadcast = true, twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                (it.data as? SendTransactionSuccess)?.also { sendTransactionSuccess ->
                    val successAction = sendTransactionSuccess.successAction
                    if(successAction != null){
                        val message = when(successAction){
                            is SuccessActionProcessed.Aes -> {
                                "${successAction.data.description}\n\n${successAction.data.plaintext}"
                            }
                            is SuccessActionProcessed.Message -> {
                                successAction.data.message
                            }
                            is SuccessActionProcessed.Url -> {
                                successAction.data.description
                            }
                        }

                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.id_success)
                            .setMessage(message)
                            .setPositiveButton(if(successAction is SuccessActionProcessed.Url) R.string.id_open else android.R.string.ok) { _, _ ->
                                if(successAction is SuccessActionProcessed.Url){
                                    openBrowser(successAction.data.url)
                                }
                                navigateBack(sendTransactionSuccess.isSendAll)
                            }.apply {
                                if(successAction is SuccessActionProcessed.Url){
                                    setNegativeButton(android.R.string.cancel) { _, _ ->
                                        navigateBack(sendTransactionSuccess.isSendAll)
                                    }
                                }
                            }
                            .show()
                    }else{
                        snackbar(R.string.id_transaction_sent)
                        navigateBack(sendTransactionSuccess.isSendAll)
                    }
                }
            }

            consumableEvent?.getContentIfNotHandledForType<GdkEvent.SuccessWithData>()?.let {
                (it.data as? String)?.also { signedTransaction ->
                    dialog("Signed Transaction", signedTransaction, isMessageSelectable = true)
                }
            }
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                // Reset send slider
                binding.buttonSend.setCompleted(completed = false, withAnimation = true)

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
                        errorDialog(throwable, true)
                    }
                }
            }
        }

        viewModel.onProgress.observe(viewLifecycleOwner){
            onBackCallback.isEnabled = it
            if(account.isLightning){
                binding.outputs.alpha = if(it) 0.2f else 1.0f
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.add_note).isVisible = transactionOrNull?.isSweep == false && viewModel.transactionNote.isBlank() && !account.isLightning
        menu.findItem(R.id.sign_transaction).isVisible = isDevelopmentOrDebug
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_note -> {
                NoteBottomSheetDialogFragment.show(note = viewModel.transactionNote, fragmentManager = childFragmentManager)
                return true
            }
            R.id.sign_transaction -> {
                viewModel.signTransaction(broadcast = false, twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    fun createAdapter(isAddressVerificationOnDevice: Boolean): ItemAdapter<GenericItem> {
        val look = ConfirmTransactionLook(
            network = network,
            session = session,
            transaction = transaction,
            denomination = args.denomination.takeIf { it?.isFiat == false },
            showChangeOutputs = isAddressVerificationOnDevice && session.device?.isLedger == true,
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

        if(!account.isLightning) {
            list += TransactionFeeListItem(session = session, look = look, confirmLook = look)
        }

        itemAdapter.add(list)
        return itemAdapter
    }

    private fun navigateBack(isSendAll: Boolean){
        setNavigationResult(
            result = isSendAll,
            key = WalletOverviewFragment.BROADCASTED_TRANSACTION,
            destinationId = R.id.walletOverviewFragment
        )
        findNavController().popBackStack(R.id.walletOverviewFragment, false)
    }
}