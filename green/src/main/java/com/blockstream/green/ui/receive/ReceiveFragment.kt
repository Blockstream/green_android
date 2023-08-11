package com.blockstream.green.ui.receive

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import breez_sdk.LnInvoice
import com.blockstream.common.Urls
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.green.R
import com.blockstream.green.data.AddressType
import com.blockstream.green.data.DenominatedValue
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.data.MediaType
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.databinding.ReceiveFragmentBinding
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.extensions.share
import com.blockstream.green.extensions.shareJPEG
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.extensions.toast
import com.blockstream.green.ui.add.AbstractAddAccountFragment
import com.blockstream.green.ui.bottomsheets.ChooseAssetAccountListener
import com.blockstream.green.ui.bottomsheets.DenominationBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuDataProvider
import com.blockstream.green.ui.bottomsheets.NoteBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RequestAmountLabelBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.VerifyAddressBottomSheetDialogFragment
import com.blockstream.green.ui.dialogs.QrDialogFragment
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.wallet.AbstractAssetWalletFragment
import com.blockstream.green.utils.AmountTextWatcher
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.formatFullWithTime
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.pulse
import com.blockstream.green.utils.rotate
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.views.GreenAlertView
import com.blockstream.lightning.amountSatoshi
import com.blockstream.lightning.expireInAsDate
import com.mikepenz.fastadapter.GenericItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ReceiveFragment : AbstractAssetWalletFragment<ReceiveFragmentBinding>(
    layout = R.layout.receive_fragment,
    menuRes = R.menu.menu_receive
), MenuDataProvider, ChooseAssetAccountListener {
    val args: ReceiveFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "Receive"

    override val showBalance: Boolean = false
    override val showChooseAssetAccount: Boolean = true
    override val showEditIcon: Boolean by lazy {
        !(session.isLightningOnly && session.accounts.size == 1)
    }
    override val isAdjustResize: Boolean
        get() = true

    override val accountAssetLayoutBinding: AccountAssetLayoutBinding
        get() = binding.accountAsset

    override val subtitle: String?
        get() = if (isSessionNetworkInitialized) wallet.name else null

    @Inject
    lateinit var viewModelFactory: ReceiveViewModel.AssistedFactory
    val viewModel: ReceiveViewModel by viewModels {
        ReceiveViewModel.provideFactory(
            viewModelFactory,
            this,
            arguments,
            wallet = args.wallet,
            initAccountAsset = args.accountAsset
        )
    }

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun getAccountWalletViewModel() = viewModel

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
            showWaitingToast()
        }
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedGuarded(view, savedInstanceState)

        getNavigationResult<AccountAsset>(AbstractAddAccountFragment.SET_ACCOUNT)?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.accountAsset = it
                clearNavigationResult(AbstractAddAccountFragment.SET_ACCOUNT)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<GdkEvent.SuccessWithData>()?.let {
                (it.data as? LnInvoice)?.also { paidInvoice ->

                    lifecycleScope.launch {

                        val amount = (paidInvoice.amountSatoshi() ?: 0).toAmountLook(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        )

                        dialog(getString(R.string.id_funds_received), getString(R.string.id_you_have_just_received_s, amount), R.drawable.ic_lightning)
                    }
                }
            }
        }

        binding.vm = viewModel

        AmountTextWatcher.watch(binding.amountEditText)

        binding.address.setOnClickListener {
            copyToClipboard(
                "Address",
                binding.address.text.toString(),
                requireContext(),
                animateView = binding.address
            )
            snackbar(R.string.id_address_copied_to_clipboard)
            countly.receiveAddress(
                addressType = if (viewModel.isAddressUri.value == true) AddressType.URI else AddressType.ADDRESS,
                mediaType = MediaType.TEXT,
                account = account,
                session = session
            )
        }

        binding.buttonShare.setOnClickListener {
            MenuBottomSheetDialogFragment.show(
                requestCode = 1, title = getString(R.string.id_share), menuItems = listOf(
                    MenuListItem(
                        icon = R.drawable.ic_baseline_text_fields_24,
                        title = StringHolder(R.string.id_address)
                    ),
                    MenuListItem(
                        icon = R.drawable.ic_qr_code,
                        title = StringHolder(R.string.id_qr_code)
                    )
                ), fragmentManager = childFragmentManager
            )
        }

        binding.buttonMore.setOnClickListener {
            MenuBottomSheetDialogFragment.show(
                requestCode = 2,
                title = getString(R.string.id_more_options),
                menuItems = buildList {
                    add(MenuListItem(id = 0, title = StringHolder(R.string.id_request_amount)))
                    add(MenuListItem(id = 1, title = StringHolder(R.string.id_list_of_addresses)))
                    if (!network.isLiquid && !network.isElectrum) {
                        add(MenuListItem(id = 2, title = StringHolder(R.string.id_sweep_from_paper_wallet)))
                    }
                },
                fragmentManager = childFragmentManager
            )
        }

        binding.buttonNewAddress.setOnClickListener {
            if (viewModel.onProgress.value == false) {
                viewModel.generateAddress()
                binding.buttonNewAddress.rotate()
            } else {
                showWaitingToast()
            }
        }

        binding.buttonEdit.setOnClickListener {
            RequestAmountLabelBottomSheetDialogFragment.show(childFragmentManager)
        }

        binding.addressQrWrap.setOnClickListener {
            copyToClipboard("Address", binding.address.text.toString(), requireContext())
            snackbar(R.string.id_address_copied_to_clipboard)
            it.pulse()

            countly.receiveAddress(
                addressType = if (viewModel.isAddressUri.value == true) AddressType.URI else AddressType.ADDRESS,
                mediaType = MediaType.TEXT,
                account = account,
                session = session
            )
        }

        binding.addressQrWrap.setOnLongClickListener {
            viewModel.addressQRBitmap.value?.also {
                QrDialogFragment.show(it, childFragmentManager)
            }

            true
        }

        binding.assetWhitelistWarning.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.LEDGER_SUPPORTED_ASSETS)
        }

        binding.buttonVerify.setOnClickListener {
            VerifyAddressBottomSheetDialogFragment.show(address = viewModel.addressAsString , fragmentManager = childFragmentManager)

            if (viewModel.onProgress.value == false) {
                viewModel.validateAddressInDevice()
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it, showReport = true)
            }
        }

        viewModel.addressQRBitmap.observe(viewLifecycleOwner) {
            binding.addressQR.setImageDrawable(BitmapDrawable(resources, it).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        }

        viewModel.onProgress.observe(viewLifecycleOwner) {
            // On HWWallet Block going back until address is generated
            onBackCallback.isEnabled = session.isHardwareWallet && it
            invalidateMenu()
        }

        viewModel.accountLiveData.observe(viewLifecycleOwner){
            invalidateMenu()
        }

        binding.buttonConfirm.setOnClickListener {
            viewModel.createLightningInvoice()
            hideKeyboard()
        }

        listOf(binding.buttonAmountCurrency, binding.amountCurrency).setOnClickListener {
            lifecycleScope.launch {
                UserInput.parseUserInputSafe(
                    session,
                    viewModel.amount.value,
                    assetId = session.lightning?.policyAsset,
                    denomination = viewModel.denomination.value!!

                ).getBalance().also {
                    DenominationBottomSheetDialogFragment.show(
                        denominatedValue = DenominatedValue(
                            balance = it,
                            assetId = session.lightning?.policyAsset,
                            denomination = viewModel.denomination.value!!
                        ),
                        childFragmentManager)
                }
            }
        }

        binding.buttonEditAmount.setOnClickListener {
            viewModel.addressUri.value = ""
            viewModel.lightningInvoice.value = null
        }

        binding.buttonAmountPaste.setOnClickListener {
            viewModel.amount.value = getClipboard(requireContext())
        }

        binding.buttonAmountClear.setOnClickListener {
            viewModel.amount.value = ""
        }

        binding.invoiceExpiration.setOnClickListener {
            viewModel.lightningInvoice.value?.also {
                snackbar(it.expireInAsDate().formatFullWithTime())
            }
        }

        binding.buttonLearnMore.setOnClickListener {
            openBrowser(Urls.HELP_RECEIVE_FEES)
        }

        binding.buttonOnChainToggle.setOnClickListener {
            if(viewModel.showOnchainAddress.boolean()){
                viewModel.showOnchainAddress.value = false
            }else{
                viewModel.createOnchain()
            }

            hideKeyboard()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    private fun showWaitingToast() {
        session.device?.let { device ->
            if (network.isLiquid && device.isLedger) {
                toast(R.string.id_please_wait_until_your_ledger)
            } else {
                toast(R.string.id_please_hold_on_while_your)
            }
        }
    }

    private fun createQRImageAndShare() {
        viewModel.addressQRBitmap.value?.let { qrBitmap ->

            try {
                val extraTextHeight = 80
                val imageSize = 600
                val padding = 16

                val qrCodeScalled = Bitmap.createScaledBitmap(
                    qrBitmap,
                    imageSize - (padding * 2),
                    imageSize - (padding * 2),
                    false
                );

                val bitmap = Bitmap.createBitmap(
                    imageSize,
                    imageSize + extraTextHeight,
                    Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(bitmap)
                canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF) // White
                canvas.drawBitmap(qrCodeScalled, padding.toFloat(), padding.toFloat(), null)


                val textPaint = TextPaint()
                textPaint.isAntiAlias = true
                textPaint.textSize = 16.0f

                val text = viewModel.addressUri.value ?: ""
                val staticLayout = StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length,
                    textPaint,
                    imageSize - (padding * 2)
                )
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .setMaxLines(3)
                    .setEllipsize(TextUtils.TruncateAt.MIDDLE)
                    .build()

                canvas.translate(padding.toFloat(), imageSize.toFloat())
                staticLayout.draw(canvas)

                val file = File(requireContext().cacheDir, "Green_QR_Code.jpg")

                // Delete the previous file as a procaution to share an old version
                file.delete()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG, 60, FileOutputStream(
                        file
                    )
                )

                val fileUri =
                    FileProvider.getUriForFile(
                        requireActivity(),
                        requireContext().packageName.toString() + ".provider",
                        file
                    )

                shareJPEG(fileUri)
            } catch (e: Exception) {
                errorDialog(e)
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.add_description).isVisible = account.isLightning
        menu.findItem(R.id.add_description).isEnabled = !viewModel.onProgress.boolean()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.help -> {
                openBrowser(if (account.isAmp) Urls.HELP_AMP_ASSETS else Urls.HELP_RECEIVE_ASSETS)
            }
            R.id.add_description -> {
                NoteBottomSheetDialogFragment.show(
                    note = viewModel.note.value ?: "",
                    isLightning = true,
                    fragmentManager = childFragmentManager
                )
            }

        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun menuItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        if (requestCode == 1) {
            countly.receiveAddress(
                addressType = if (viewModel.isAddressUri.value == true) AddressType.URI else AddressType.ADDRESS,
                mediaType = if (position == 0) MediaType.TEXT else MediaType.IMAGE,
                isShare = true,
                account = account,
                session = session
            )

            if (position == 0) {
                share(binding.address.text.toString())
            } else {
                createQRImageAndShare()
            }
        } else if (requestCode == 2 && item is MenuListItem) {

            when (item.id) {
                0 -> {
                    RequestAmountLabelBottomSheetDialogFragment.show(childFragmentManager)
                }
                1 -> {
                    navigate(
                        ReceiveFragmentDirections.actionReceiveFragmentToPreviousAddressesFragment(
                            wallet = wallet,
                            account = account
                        )
                    )
                }
                2 -> {
                    navigate(
                        ReceiveFragmentDirections.actionReceiveFragmentToSendFragment(
                            wallet = wallet,
                            accountAsset = viewModel.accountAsset,
                            isSweep = true
                        )
                    )
                }
            }
        }
    }

    override fun createNewAccountClicked(assetId: String) {
        navigate(
            ReceiveFragmentDirections.actionGlobalChooseAccountTypeFragment(
                wallet = wallet,
                assetId = assetId
            )
        )
    }
}
