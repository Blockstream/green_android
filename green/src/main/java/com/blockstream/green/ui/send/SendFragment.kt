package com.blockstream.green.ui.send

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.base.Urls
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.R
import com.blockstream.green.data.AddressInputType
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.ListItemTransactionRecipientBinding
import com.blockstream.green.databinding.SendFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.filters.NumberValueFilter
import com.blockstream.green.gdk.assetTicker
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.looks.AssetLook
import com.blockstream.green.ui.bottomsheets.AccountAssetBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.SelectUtxosBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.AbstractAssetWalletFragment
import com.blockstream.green.utils.AmountTextWatcher
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.getFiatCurrency
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.GreenAlertView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SendFragment : AbstractAssetWalletFragment<SendFragmentBinding>(
    layout = R.layout.send_fragment,
    menuRes = 0
) {
    override val isAdjustResize = false

    val args: SendFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }
    private val isSweep by lazy { args.isSweep }
    private val isBump by lazy { !args.bumpTransaction.isNullOrBlank() }
    private val bumpTransaction by lazy { if(isBump) Json.parseToJsonElement(args.bumpTransaction ?: "") else null }

    // Store bindings as weak reference to allow them to be GC'ed
    val bindings = mutableListOf<WeakReference<ListItemTransactionRecipientBinding>>()
    
    override val screenName = "Send"

    override val showBalance: Boolean = false

    override val showEditIcon: Boolean
        get() = !(isBump || isSweep)

    @Inject
    lateinit var viewModelFactory: SendViewModel.AssistedFactory
    val viewModel: SendViewModel by viewModels {
        SendViewModel.provideFactory(
            viewModelFactory,
            args.wallet,
            args.accountAsset ?: AccountAsset.fromAccount(session.activeAccount),
            isSweep,
            args.address,
            bumpTransaction
        )
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun getAccountWalletViewModel() = viewModel

    override val title: String
        get() = getString(
            when {
                isBump -> R.string.id_increase_fee
                isSweep -> R.string.id_sweep
                else -> R.string.id_send
            }
        )


    override val accountAssetLayoutBinding: AccountAssetLayoutBinding?
        get() = bindings.getOrNull(0)?.get()?.accountAsset

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        // Clear previous references as we need to re-create everything
        bindings.clear()

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) {
            it?.let { result ->
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.setScannedAddress(viewModel.activeRecipient, result)
            }
        }

        // Handle pending BIP-21 uri
        sessionManager.pendingBip21Uri.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { bip21Uri ->
                viewModel.setBip21Uri(bip21Uri)
                snackbar(R.string.id_address_was_filled_by_a_payment)
            }
        }

        binding.vm = viewModel
        binding.enableMultipleRecipients = false //isDevelopmentFlavor() && session.isTestnet

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.Navigate>()?.let {
                navigate(SendFragmentDirections.actionSendFragmentToSendConfirmFragment(
                    wallet = wallet,
                    account = account,
                    transactionSegmentation = viewModel.createTransactionSegmentation()
                ))
            }

            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateBack>()?.let {
                it.reason?.let {
                    errorDialog(it){
                        popBackStack()
                    }
                } ?: popBackStack()
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.getRecipientsLiveData().observe(viewLifecycleOwner) {
            for (liveData in it.withIndex()) {
                updateRecipient(liveData.index, liveData.value)
            }

            // Remove views
            while (bindings.size > it.size){
                binding.recipientContainer.removeViewAt(bindings.size - 1)
                bindings.removeLastOrNull()
            }
        }

        binding.buttonAddRecipient.setOnClickListener {
            viewModel.addRecipient()
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.confirmTransaction()
        }

        binding.buttonEditFee.setOnClickListener {
            setCustomFeeRate()
        }

        listOf(binding.buttonFeeHelp, binding.feeLabel, binding.feeRate).setOnClickListener {
            openBrowser(Urls.HELP_FEES)
        }

        binding.feeSlider.setLabelFormatter { value: Float ->
            val format = NumberFormat.getCurrencyInstance()
            format.maximumFractionDigits = 0
            format.format(value.toDouble())

            getString(when(value.toInt()){
                1 -> R.string.id_slow
                2 -> R.string.id_medium
                3 -> R.string.id_fast
                else -> R.string.id_custom
            })
        }

        viewModel.feeSlider.distinctUntilChanged().observe(viewLifecycleOwner) { slider ->
            binding.expectedConfirmationTime = if(slider.toInt() == SendViewModel.SliderCustomIndex){
                getString(R.string.id_custom)
            }else{
                getExpectedConfirmationTime(requireContext(), GdkBridge.FeeBlockTarget[3 - (slider.toInt())])
            }
        }

        super.onViewCreatedGuarded(view, savedInstanceState)
    }

    private fun getExpectedConfirmationTime(context: Context, blocks: Int): String {
        val blocksPerHour = account.network.blocksPerHour
        val n = if (blocks % blocksPerHour == 0) blocks / blocksPerHour else blocks * (60 / blocksPerHour)
        val s = context.getString(if (blocks % blocksPerHour == 0) if (blocks == blocksPerHour) R.string.id_hour else R.string.id_hours else R.string.id_minutes)
        return String.format(Locale.getDefault(), " ~ %d %s", n, s)
    }

    private fun updateRecipient(index: Int, value: AddressParamsLiveData) {
        while (index >= bindings.size) {
            val recipientBinding = ListItemTransactionRecipientBinding.inflate(layoutInflater)

            recipientBinding.lifecycleOwner = viewLifecycleOwner
            recipientBinding.vm = viewModel

            bindings.add(WeakReference(recipientBinding))
            binding.recipientContainer.addView(recipientBinding.root)

            initRecipientBinging(recipientBinding)
        }

        bindings.getOrNull(index)?.get()?.let { binding ->
            updateBindingData(recipientBinding = binding, index = index, liveData = value)
        }
    }

    private fun initRecipientBinging(recipientBinding: ListItemTransactionRecipientBinding) {
        if(!isBump){
            recipientBinding.buttonAddressPaste.setOnClickListener{
                viewModel.getRecipientLiveData(recipientBinding.index ?: 0)?.let {
                    it.address.value = getClipboard(requireContext())
                    it.addressInputType = AddressInputType.PASTE
                }
            }

            recipientBinding.buttonAddressClear.setOnClickListener{
                viewModel.getRecipientLiveData(recipientBinding.index ?: 0)?.let {
                    it.address.value = ""
                }
            }
        }

        AmountTextWatcher.watch(recipientBinding.amountEditText)

        recipientBinding.buttonAddressScan.setOnClickListener {
            viewModel.activeRecipient = recipientBinding.index ?: 0
            CameraBottomSheetDialogFragment.showSingle(fragmentManager = childFragmentManager)
        }

        recipientBinding.accountAsset.root.setOnClickListener {
            val liveData = viewModel.getRecipientLiveData(recipientBinding.index ?: 0)

            // Skip if we have a bip21 asset / bump / sweep
            if(liveData?.assetBip21?.value == true || isBump || isSweep){
                return@setOnClickListener
            }

            viewModel.activeRecipient = recipientBinding.index ?: 0

            AccountAssetBottomSheetDialogFragment.show(childFragmentManager, showBalance = true)
        }

        recipientBinding.toggleGroupSendAll.addOnButtonCheckedListener { _, _, isChecked ->
            viewModel.sendAll(index = recipientBinding.index ?: 0, isSendAll = isChecked)
        }

        recipientBinding.buttonAmountCurrency.setOnClickListener {
            viewModel.toggleCurrency(index = recipientBinding.index ?: 0)
        }

        recipientBinding.buttonAmountPaste.setOnClickListener {
            viewModel.getRecipientLiveData(recipientBinding.index ?: 0)?.let {
                it.amount.value = getClipboard(requireContext())
            }
        }

        recipientBinding.buttonAmountClear.setOnClickListener {
            viewModel.getRecipientLiveData(recipientBinding.index ?: 0)?.let {
                it.amount.value = ""
                it.isSendAll.value = false
            }
        }

        recipientBinding.buttonRemove.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_remove)
                .setMessage(R.string.id_are_you_sure_you_want_to_remove)
                .setPositiveButton(R.string.id_remove) { _, _ ->
                    viewModel.removeRecipient(recipientBinding.index ?: 0)
                }
                .setNegativeButton(R.string.id_cancel) { _, _ ->

                }
                .show()
        }

        recipientBinding.buttonCoinControl.setOnClickListener {
            viewModel.activeRecipient = recipientBinding.index ?: 0

            // WIP
            SelectUtxosBottomSheetDialogFragment.show(fragmentManager =  childFragmentManager)
        }
    }


    private fun updateBindingData(
        recipientBinding: ListItemTransactionRecipientBinding,
        index: Int,
        liveData: AddressParamsLiveData
    ) {
        recipientBinding.liveData = liveData
        recipientBinding.index = index

        viewModel.getRecipientLiveData(index)?.let { addressParamsLiveData ->
            combine(
                addressParamsLiveData.isFiat.asFlow(),
                addressParamsLiveData.accountAsset.asFlow(),
            ) { _,  ->
                addressParamsLiveData
            }.onEach {
                val assetId = addressParamsLiveData.accountAsset.value!!.assetId
                val account = addressParamsLiveData.accountAsset.value!!.account

                val balance = session.accountAssets(account).firstNotNullOfOrNull { if (it.key == assetId) it.value else null }
                var look: AssetLook? = null

                if (!assetId.isNullOrBlank()) {
                    look = AssetLook(
                        assetId = assetId,
                        amount = balance ?: 0,
                        session = session
                    )
                }

                recipientBinding.assetName = look?.name
                recipientBinding.assetBalance = look?.balance(isFiat = it.isFiat.value, withUnit = true) ?: ""
                recipientBinding.assetSatoshi = balance ?: 0


                recipientBinding.canConvert = assetId.isPolicyAsset(session)

                recipientBinding.amountCurrency = assetId.let { assetId ->
                    if (it.isFiat.value == true) {
                        getFiatCurrency(account.network, session)
                    } else {
                        assetId.assetTicker(session)
                    }
                }
            }.launchIn(lifecycleScope)

            // When changing asset and send all is enabled, listen for the event resetting the send all flag
            addressParamsLiveData.isSendAll.observe(viewLifecycleOwner) { isSendAll ->
                if (recipientBinding.buttonSendAll.isChecked != isSendAll) {
                    recipientBinding.buttonSendAll.isChecked = isSendAll
                }
            }
        }
    }

    private fun setCustomFeeRate(){
        val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))
        dialogBinding.textInputLayout.endIconCustomMode()

        // TODO add locale
        dialogBinding.textInputLayout.placeholderText = "0.00"
        dialogBinding.editText.keyListener = NumberValueFilter(2)
        dialogBinding.text = (viewModel.getFeeRate().toDouble() / 1000).toString()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_default_custom_fee_rate)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                try {
                    dialogBinding.text.let { input ->
                        if (input.isNullOrBlank()) {
                            viewModel.setCustomFeeRate(null)
                        } else {
                            val minFeeRateKB: Long = viewModel.feeEstimation?.firstOrNull() ?: account.network.defaultFee
                            val enteredFeeRate = dialogBinding.text?.toDouble() ?: 0.0
                            if (enteredFeeRate * 1000 < minFeeRateKB) {
                                snackbar(
                                    getString(
                                        R.string.id_fee_rate_must_be_at_least_s, String.format(
                                            "%.2f",
                                            minFeeRateKB / 1000.0
                                        )
                                    ), Snackbar.LENGTH_SHORT
                                )
                            } else {
                                viewModel.setCustomFeeRate((enteredFeeRate * 1000).toLong())
                            }
                        }
                    }

                } catch (e: Exception) {
                    snackbar(R.string.id_error_setting_fee_rate, Snackbar.LENGTH_SHORT)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}
