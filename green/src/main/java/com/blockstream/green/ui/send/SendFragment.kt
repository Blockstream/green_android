package com.blockstream.green.ui.send

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.AddressInputType
import com.blockstream.common.Urls
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.isEmpty
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.FeeBlockTarget
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.send.SendScreen
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.ListItemTransactionRecipientBinding
import com.blockstream.green.databinding.SendFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.openKeyboard
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.filters.NumberValueFilter
import com.blockstream.green.looks.AssetLook
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.bottomsheets.AccountAssetBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DenominationBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.SelectUtxosBottomSheetDialogFragment
import com.blockstream.green.ui.login.LoginFragmentArgs
import com.blockstream.green.ui.login.LoginFragmentDirections
import com.blockstream.green.ui.wallet.AbstractAssetWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.utils.AmountTextWatcher
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.underlineText
import com.blockstream.green.views.GreenAlertView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Locale

class SendFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    val args: SendFragmentArgs by navArgs()

    val viewModel: com.blockstream.common.models.send.SendViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.accountAsset,
            args.address,
            args.addressType
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val title: String
        get() = getString(R.string.id_send)

    override val useCompose: Boolean = true

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        when (sideEffect) {
            is SideEffects.NavigateToRoot -> {
                findNavController().popBackStack(R.id.walletOverviewFragment, false)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    SendScreen(viewModel = viewModel)
                }
            }
        }

        viewModel.navData.onEach {
            setToolbarVisibility(it.isVisible)
            onBackCallback.isEnabled = !it.isVisible
            (requireActivity() as MainActivity).lockDrawer(!it.isVisible)
        }.launchIn(lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }
}

class SendFragmentOld : AbstractAssetWalletFragment<SendFragmentBinding>(
    layout = R.layout.send_fragment,
    menuRes = 0
) {
    override val isAdjustResize = false

    val args: SendFragmentArgs by navArgs()

    private val isSweep = false
    private val isBump by lazy {
        false
//        !args.bumpTransaction.isNullOrBlank()
    }
    private val bumpTransaction by lazy { if(isBump) Json.parseToJsonElement( "") else null }

    // Store bindings as weak reference to allow them to be GC'ed
    val bindings = mutableListOf<WeakReference<ListItemTransactionRecipientBinding>>()
    
    override val screenName = "Send"

    override val showBalance: Boolean = false

    override val showEditIcon: Boolean
        get() = !(isBump || isSweep)


    val viewModel: SendViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.accountAsset,
            args.address,
            args.addressType,
            bumpTransaction
        )
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun getGreenViewModel(): GreenViewModel = viewModel

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

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Navigate){
            navigate(SendFragmentDirections.actionSendFragmentToSendConfirmFragment(
                wallet = viewModel.greenWallet,
                accountAsset = viewModel.accountAsset.value!!,
                denomination = viewModel.getRecipientStateFlow(0)?.denomination?.value,
                transactionSegmentation = viewModel.createTransactionSegmentation()
            ))
            // Re-enable continue button
            viewModel.onProgress.value = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Clear previous references as we need to re-create everything
        bindings.clear()

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) {
            it?.let { result ->
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.setAddress(viewModel.activeRecipient, result.result, AddressInputType.SCAN)
            }
        }

        // Handle pending URI (BIP-21 or lightning)
        sessionManager.pendingUri.filterNotNull().onEach {
            viewModel.setUri(it)
            sessionManager.pendingUri.value = null
            snackbar(R.string.id_address_was_filled_by_a_payment)
        }.launchIn(lifecycleScope)

        binding.vm = viewModel
        binding.enableMultipleRecipients = false //isDevelopmentFlavor() && session.isTestnet

        viewModel.getRecipientsStateFlow().onEach {
            for (liveData in it.withIndex()) {
                updateRecipient(liveData.index, liveData.value)
            }

            // Remove views
            while (bindings.size > it.size){
                binding.recipientContainer.removeViewAt(bindings.size - 1)
                bindings.removeLastOrNull()
            }
        }.launchIn(lifecycleScope)

        viewModel.transactionError.distinctUntilChanged().observe(viewLifecycleOwner){
            if(it?.startsWith("id_amount_must_be_at_least_s") == true){
                lifecycleScope.launch {
                    bindings.firstOrNull()?.get()?.amountEditText?.requestFocus()
                    openKeyboard()
                }
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
                getExpectedConfirmationTime(requireContext(), FeeBlockTarget[3 - (slider.toInt())])
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun getExpectedConfirmationTime(context: Context, blocks: Int): String {
        val blocksPerHour = viewModel.account.network.blocksPerHour
        val n = if (blocks % blocksPerHour == 0) blocks / blocksPerHour else blocks * (60 / blocksPerHour)
        val s = context.getString(if (blocks % blocksPerHour == 0) if (blocks == blocksPerHour) R.string.id_hour else R.string.id_hours else R.string.id_minutes)
        return String.format(Locale.getDefault(), "~ %d %s", n, s)
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
            recipientBinding.buttonAddressPaste.setOnClickListener {
                viewModel.setAddress(recipientBinding.index ?: 0, getClipboard(requireContext()) ?: "", AddressInputType.PASTE)
            }

            recipientBinding.buttonAddressClear.setOnClickListener {
                viewModel.setAddress(recipientBinding.index ?: 0, "", AddressInputType.PASTE)
            }
        }

        AmountTextWatcher.watch(recipientBinding.amountEditText)

        recipientBinding.buttonAddressScan.setOnClickListener {
            viewModel.activeRecipient = recipientBinding.index ?: 0
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, fragmentManager = childFragmentManager)
        }

        recipientBinding.accountAsset.root.setOnClickListener {
            val liveData = viewModel.getRecipientStateFlow(recipientBinding.index ?: 0)

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

        listOf(recipientBinding.buttonAmountCurrency, recipientBinding.amountCurrency).setOnClickListener {
            lifecycleScope.launch {

                val amountToConvert = viewModel.getAmountToConvert()
                val assetId = viewModel.getRecipientStateFlow(0)?.accountAsset?.value?.assetId
                if(assetId.isPolicyAsset(viewModel.session)) {
                    val denomination = viewModel.getRecipientStateFlow(0)?.denomination?.value

                    UserInput.parseUserInputSafe(
                        session = viewModel.session,
                        input = amountToConvert,
                        assetId = assetId,
                        denomination = denomination
                    ).getBalance().also {
                        DenominationBottomSheetDialogFragment.show(
                            denominatedValue = DenominatedValue(
                                balance = it,
                                assetId = assetId,
                                denomination = denomination!!
                            ), childFragmentManager
                        )
                    }
                }
            }
        }

        recipientBinding.buttonAmountPaste.setOnClickListener {
            viewModel.getRecipientStateFlow(recipientBinding.index ?: 0)?.let {
                it.amount.value = getClipboard(requireContext())
            }
        }

        recipientBinding.buttonAmountClear.setOnClickListener {
            viewModel.getRecipientStateFlow(recipientBinding.index ?: 0)?.let {
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

        viewModel.getRecipientStateFlow(index)?.let { addressParamsLiveData ->
            combine(
                addressParamsLiveData.denomination.asFlow(),
                addressParamsLiveData.accountAsset,
            ) { _,  ->
                addressParamsLiveData
            }.onEach {
                val assetId = addressParamsLiveData.accountAsset.value!!.assetId
                val account = addressParamsLiveData.accountAsset.value!!.account

                val balance = viewModel.session.accountAssets(account).value.balanceOrNull(assetId)
                var look: AssetLook? = null

                if (!assetId.isNullOrBlank()) {
                    look = AssetLook(
                        assetId = assetId,
                        amount = balance ?: 0,
                        session = viewModel.session
                    )
                }

                recipientBinding.addressEditText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or (InputType.TYPE_TEXT_FLAG_MULTI_LINE.takeIf { !account.isLightning } ?: 0)

                recipientBinding.assetName = look?.name
                recipientBinding.assetBalance = look?.balance(denomination = it.denomination.value, withUnit = true) ?: ""
                recipientBinding.assetSatoshi = balance ?: 0
                assetId.isPolicyAsset(viewModel.session).also { isPolicyAsset ->
                    recipientBinding.canConvert = isPolicyAsset

                    (it.denomination.value?.assetTicker(viewModel.session, it.accountAsset.value?.assetId) ?: "").also { amountCurrency ->
                        // Underline only if canConvert
                        recipientBinding.amountCurrency.text = if (isPolicyAsset) underlineText(amountCurrency) else amountCurrency
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
                            val minFeeRateKB: Long = viewModel.feeEstimation?.firstOrNull() ?: viewModel.account.network.defaultFee
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
