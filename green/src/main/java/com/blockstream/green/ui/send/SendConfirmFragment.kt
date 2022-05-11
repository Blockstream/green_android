package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.ListItemTransactionAmountBinding
import com.blockstream.green.databinding.SendConfirmFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.bottomsheets.VerifyTransactionBottomSheetDialogFragment
import com.blockstream.green.ui.items.GenericDetailListItem
import com.blockstream.green.ui.items.TransactionAmountListItem
import com.blockstream.green.ui.items.TransactionFeeListItem
import com.blockstream.green.ui.looks.ConfirmTransactionLook
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.utils.*
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.ncorti.slidetoact.SlideToActView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SendConfirmFragment : WalletFragment<SendConfirmFragmentBinding>(
    layout = R.layout.send_confirm_fragment,
    menuRes = 0
) {
    lateinit var noteListItem: GenericDetailListItem

    val args: SendConfirmFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "SendConfirm"
    override val segmentation by lazy { if(isSessionAndWalletRequired() && isSessionNetworkInitialized) countly.subAccountSegmentation(session, subAccount = viewModel.getSubAccountLiveData().value) else null }

    @Inject
    lateinit var viewModelFactory: SendConfirmViewModel.AssistedFactory
    val viewModel: SendConfirmViewModel by viewModels {
        SendConfirmViewModel.provideFactory(viewModelFactory, wallet, args.transactionSegmentation)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel


        if(session.pendingTransaction == null){
            popBackStack()
            return
        }

        noteListItem = GenericDetailListItem(
            title = StringHolder(R.string.id_my_notes),
            liveContent = viewModel.editableNote,
        )

        viewModel.deviceAddressValidationEvent.observe(viewLifecycleOwner){
            if(it.peekContent() == null){ // open if null
                VerifyTransactionBottomSheetDialogFragment.show(childFragmentManager)
            }
        }

        val fastAdapter = FastAdapter.with(createAdapter(isAddressVerificationOnDevice = false))

        fastAdapter.addClickListener<ListItemTransactionAmountBinding, GenericItem>({ binding -> binding.addressTextView }) { view, _: Int, _: FastAdapter<GenericItem>, _: GenericItem ->
            copyToClipboard("Address", (view as TextView).text.toString(), animateView = view)
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
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.Navigate>()?.let {
                snackbar(R.string.id_transaction_sent)
                findNavController().popBackStack(R.id.overviewFragment, false)
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
                        findNavController().popBackStack(R.id.overviewFragment, false)
                    }
                    throwable.message != "id_action_canceled" -> {
                        errorSnackbar(throwable)
                    }
                }
            }
        }
    }

    fun createAdapter(isAddressVerificationOnDevice: Boolean): ItemAdapter<GenericItem> {
        val tx = session.pendingTransaction!!.second
        val look = ConfirmTransactionLook(session = session, tx = tx, overrideDenomination = isAddressVerificationOnDevice)

        val itemAdapter = ItemAdapter<GenericItem>()
        val list = mutableListOf<GenericItem>()

        for (i in 0 until look.recipients) {
            list += TransactionAmountListItem(i, look, withStroke = isAddressVerificationOnDevice)
        }
        
        if(isAddressVerificationOnDevice && session.device?.isJade == false){
            for(i in 0 until look.changeOutputs.size){
                list += TransactionAmountListItem(look.recipients + i, look, withStroke = isAddressVerificationOnDevice)
            }
        }

        list += TransactionFeeListItem(look)

        if(!isAddressVerificationOnDevice) {
            if(!tx.isSweep) {
                list += noteListItem
            }
        }

        itemAdapter.add(list)
        return itemAdapter
    }
}