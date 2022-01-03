package com.blockstream.green.ui.send

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.SendConfirmFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.GenericDetailListItem
import com.blockstream.green.ui.items.TransactionAmountListItem
import com.blockstream.green.ui.items.TransactionFeeListItem
import com.blockstream.green.ui.looks.ConfirmTransactionLook
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.utils.*
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SendConfirmFragment : WalletFragment<SendConfirmFragmentBinding>(
    layout = R.layout.send_confirm_fragment,
    menuRes = 0
) {
    lateinit var noteListItem: GenericDetailListItem

    val args: SendConfirmFragmentArgs by navArgs()

    override val wallet by lazy { args.wallet }

    @Inject
    lateinit var viewModelFactory: SendConfirmViewModel.AssistedFactory
    val viewModel: SendConfirmViewModel by viewModels {
        SendConfirmViewModel.provideFactory(viewModelFactory, wallet)
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
                TransactionVerifyAddressBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.toString())
                }
            }
        }

        val fastAdapter = FastAdapter.with(createAdapter(isAddressVerification = false))

        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.buttonSend.progressIndicator = binding.sendIndicator
        binding.buttonSend.setOnClickListener {
            binding.buttonSendHelp.starWarsAndHide(offset = binding.buttonSendHelp.height * 2, duration = 1000)
        }

        binding.buttonSend.setOnLongClickListener {
            viewModel.broadcastTransaction(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            true
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.Navigate>()?.let {
                snackbar(R.string.id_transaction_sent)
                findNavController().popBackStack(R.id.overviewFragment, false)
            }
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
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

    fun createAdapter(isAddressVerification: Boolean): ItemAdapter<GenericItem> {
        val tx = session.pendingTransaction!!.second
        val look = ConfirmTransactionLook(session, tx)

        val itemAdapter = ItemAdapter<GenericItem>()
        val list = mutableListOf<GenericItem>()

        for (i in 0 until look.recipients) {
            list += TransactionAmountListItem(i, look, withStroke = isAddressVerification)
        }

        if(isAddressVerification && look.changeOutput != null && session.device?.isJade == false){
            list += TransactionAmountListItem(look.recipients, look, withStroke = isAddressVerification)
        }

        list += TransactionFeeListItem(look)

        if(!isAddressVerification) {
            if(!tx.isSweep) {
                list += noteListItem
            }
        }

        itemAdapter.add(list)
        return itemAdapter
    }
}