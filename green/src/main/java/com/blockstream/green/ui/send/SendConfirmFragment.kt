package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.SendTransactionSuccess
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.send.SendConfirmViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionNoteBinding
import com.blockstream.green.databinding.ListItemTransactionOutputBinding
import com.blockstream.green.databinding.SendConfirmFragmentBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.looks.ConfirmTransactionLook
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.NoteBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.VerifyTransactionBottomSheetDialogFragment
import com.blockstream.green.ui.items.NoteListItem
import com.blockstream.green.ui.items.TransactionFeeListItem
import com.blockstream.green.ui.items.TransactionUtxoListItem
import com.blockstream.green.ui.overview.WalletOverviewFragment
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SendConfirmFragment : AppFragment<SendConfirmFragmentBinding>(
    layout = R.layout.send_confirm_fragment,
    menuRes = R.menu.send_confirm
) {
    val args: SendConfirmFragmentArgs by navArgs()

    override val segmentation
        get() = countly.accountSegmentation(
            session = viewModel.session,
            account = viewModel.account
        )

    private val beagle: Beagle by inject()

    val viewModel: SendConfirmViewModel by viewModel {
        parametersOf(args.wallet, args.accountAsset, args.transactionSegmentation)
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun getGreenViewModel(): GreenViewModel = viewModel

    private val transactionOrNull get() = viewModel.session.pendingTransaction?.second
    private val transaction get() = transactionOrNull!!

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Success){
            (sideEffect.data as? String)?.also { signedTransaction ->
                dialog("Signed Transaction", signedTransaction, isMessageSelectable = true)
            }
        } else if (sideEffect is SideEffects.NavigateToRoot) {
            findNavController().popBackStack(R.id.walletOverviewFragment, false)
        } else if (sideEffect is SideEffects.Navigate) {
            (sideEffect.data as? SendTransactionSuccess)?.also { sendTransactionSuccess ->
                if (sendTransactionSuccess.hasMessageOrUrl) {
                    val message = sendTransactionSuccess.message ?: ""

                    val isUrl = sendTransactionSuccess.url.isNotBlank()

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.id_success)
                        .setMessage(getString(R.string.id_message_from_recipient_s, message))
                        .setPositiveButton(if (isUrl) R.string.id_open else android.R.string.ok) { _, _ ->
                            if (isUrl) {
                                openBrowser(sendTransactionSuccess.url ?: "")
                            }
                            navigateBack(sendTransactionSuccess.isSendAll)
                        }.apply {
                            if (isUrl) {
                                setNegativeButton(android.R.string.cancel) { _, _ ->
                                    navigateBack(sendTransactionSuccess.isSendAll)
                                }
                            }
                        }
                        .show()
                } else {
                    snackbar(R.string.id_transaction_sent)
                    navigateBack(sendTransactionSuccess.isSendAll)
                }
            }
        } else if(sideEffect is SendConfirmViewModel.LocalSideEffects.DeviceAddressValidation){
            VerifyTransactionBottomSheetDialogFragment.show(childFragmentManager)
        } else if(sideEffect is SideEffects.Dismiss){
            VerifyTransactionBottomSheetDialogFragment.closeAll(childFragmentManager)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel

        if(viewModel.session.pendingTransaction == null){
            popBackStack()
            return
        }

        if(isDevelopmentOrDebug){
            viewModel.session.pendingTransaction!!.second.toJson().also {
                logger.info { it }
                beagle.log(it)
            }
        }

        val noteAdapter = FastItemAdapter<GenericItem>()

        viewModel.note.onEach {
            if(!transaction.isSweep() && it.isNotBlank()) {
                noteAdapter.set(
                    listOf(
                        NoteListItem(note = viewModel.note.value, isEditable = !viewModel.account.isLightning)
                    )
                )
            }else{
                noteAdapter.set(listOf())
            }

            invalidateMenu()
        }.launchIn(lifecycleScope)

        val fastAdapter = FastAdapter.with(listOf(createAdapter(isAddressVerificationOnDevice = false), noteAdapter))

        fastAdapter.addClickListener<ListItemTransactionNoteBinding, GenericItem>({ binding -> binding.buttonEdit }) { _, _, _, _ ->
            NoteBottomSheetDialogFragment.show(note = viewModel.note.value, fragmentManager = childFragmentManager)
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
                viewModel.postEvent(SendConfirmViewModel.LocalEvents.SignTransaction(
                    broadcastTransaction = true, twoFactorResolver = DialogTwoFactorResolver(this@SendConfirmFragment)
                ))
            }
        }

        viewModel.onProgress.onEach {
            onBackCallback.isEnabled = it
            if(viewModel.account.isLightning){
                binding.outputs.alpha = if(it) 0.2f else 1.0f
            }
        }.launchIn(lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.add_note).isVisible = transactionOrNull?.isSweep() == false && viewModel.note.value.isBlank() && !viewModel.account.isLightning
        menu.findItem(R.id.sign_transaction).isVisible = isDevelopmentOrDebug
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_note -> {
                NoteBottomSheetDialogFragment.show(note = viewModel.note.value, fragmentManager = childFragmentManager)
                return true
            }
            R.id.sign_transaction -> {
                viewModel.postEvent(SendConfirmViewModel.LocalEvents.SignTransaction(
                    broadcastTransaction = false, twoFactorResolver = DialogTwoFactorResolver(this)
                ))
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    fun createAdapter(isAddressVerificationOnDevice: Boolean): ItemAdapter<GenericItem> {
        val look = ConfirmTransactionLook(
            network = viewModel.account.network,
            session = viewModel.session,
            transaction = transaction,
            denomination = args.denomination.takeIf { it?.isFiat == false },
            showChangeOutputs = isAddressVerificationOnDevice && viewModel.session.device?.isLedger == true,
            isAddressVerificationOnDevice = isAddressVerificationOnDevice
        )

        val itemAdapter = ItemAdapter<GenericItem>()
        val list = mutableListOf<GenericItem>()

        for (i in 0 until look.utxoSize) {
            list += TransactionUtxoListItem(
                index = i,
                session = viewModel.session,
                look = look,
                withStroke = isAddressVerificationOnDevice
            )
        }

        if(!viewModel.account.isLightning) {
            list += TransactionFeeListItem(session = viewModel.session, look = look, confirmLook = look)
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