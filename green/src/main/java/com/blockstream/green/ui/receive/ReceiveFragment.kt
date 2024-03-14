package com.blockstream.green.ui.receive

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import breez_sdk.LnInvoice
import com.blockstream.common.Urls
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.lightning.amountSatoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.green.R
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.databinding.ReceiveFragmentBinding
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
import com.blockstream.green.ui.bottomsheets.DeviceInteractionRequestBottomSheetDialogFragment
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
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.utils.formatFullWithTime
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.pulse
import com.blockstream.green.utils.rotate
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.views.GreenAlertView
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class ReceiveFragment : AbstractAssetWalletFragment<ReceiveFragmentBinding>(
    layout = R.layout.receive_fragment,
    menuRes = R.menu.menu_receive
), MenuDataProvider, ChooseAssetAccountListener {
    val args: ReceiveFragmentArgs by navArgs()

    override val showBalance: Boolean = false
    override val showChooseAssetAccount: Boolean = true
    override val showEditIcon: Boolean by lazy {
        !(viewModel.session.isLightningShortcut && viewModel.session.accounts.value.size == 1)
    }
    override val isAdjustResize: Boolean
        get() = true

    override val accountAssetLayoutBinding: AccountAssetLayoutBinding
        get() = binding.accountAsset

    override val subtitle: String
        get() = viewModel.greenWallet.name

    val viewModel: ReceiveViewModel by viewModel {
        parametersOf(args.accountAsset, args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
            showWaitingToast()
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        when (sideEffect) {
            is ReceiveViewModel.LocalSideEffects.VerifiedOnDevice -> {
                DeviceInteractionRequestBottomSheetDialogFragment.closeAll(childFragmentManager)
            }

            is ReceiveViewModel.LocalSideEffects.VerifyOnDevice -> {
                VerifyAddressBottomSheetDialogFragment.show(address = sideEffect.address , fragmentManager = childFragmentManager)
            }

            is ReceiveViewModel.LocalSideEffects.ShareAddress -> {
               share(sideEffect.address)
            }

            is ReceiveViewModel.LocalSideEffects.ShareQR -> {
                createQRImageAndShare(sideEffect.address)
            }

            is SideEffects.Success -> {
                (sideEffect.data as? LnInvoice)?.also { paidInvoice ->
                    lifecycleScope.launch {
                        val amount = (paidInvoice.amountSatoshi() ?: 0).toAmountLook(
                            session = viewModel.session,
                            withUnit = true,
                            withGrouping = true
                        )

                        hideKeyboard()

                        // Move it as a side effect in VM
                        dialog(getString(R.string.id_funds_received), getString(R.string.id_you_have_just_received_s, amount), R.drawable.ic_lightning_fill)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<AccountAsset>(AbstractAddAccountFragment.SET_ACCOUNT)?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.postEvent(Events.SetAccountAsset(it))
                clearNavigationResult(AbstractAddAccountFragment.SET_ACCOUNT)
            }
        }

        binding.vm = viewModel

        AmountTextWatcher.watch(binding.amountEditText)

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
                    if (!viewModel.account.network.isLiquid) {
                        add(MenuListItem(id = 2, title = StringHolder(R.string.id_sweep_from_paper_wallet)))
                    }
                },
                fragmentManager = childFragmentManager
            )
        }

        binding.buttonNewAddress.setOnClickListener {
            if (!viewModel.onProgress.value) {
                binding.buttonNewAddress.rotate()
                viewModel.postEvent(ReceiveViewModel.LocalEvents.GenerateNewAddress)
            } else {
                showWaitingToast()
            }
        }

        binding.buttonEdit.setOnClickListener {
            RequestAmountLabelBottomSheetDialogFragment.show(childFragmentManager)
        }

        binding.address.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
            it.pulse()
        }

        binding.addressQrWrap.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
            it.pulse()
        }

        binding.addressQrWrap.setOnLongClickListener {
            viewModel.receiveAddress.value?.let {
                lifecycleScope.launch {
                    withContext(context = Dispatchers.IO) {
                        createQrBitmap(it)
                    }?.also {
                        QrDialogFragment.show(it, childFragmentManager)
                    }
                }
            }

            true
        }

        binding.assetWhitelistWarning.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.LEDGER_SUPPORTED_ASSETS)
        }

        binding.buttonVerify.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.VerifyOnDevice)
        }

        viewModel.receiveAddress.onEach {
            if (it != null) {
                binding.addressQR.setImageDrawable(
                    BitmapDrawable(
                        resources,
                        withContext(context = Dispatchers.IO) { createQrBitmap(it) }).also { bitmap ->
                        bitmap.isFilterBitmap = false
                    })
            } else {
                binding.addressQR.setImageDrawable(null)
            }
        }.launchIn(lifecycleScope)

        viewModel.onProgress.onEach {
            // On HWWallet Block going back until address is generated
            onBackCallback.isEnabled = viewModel.session.isHardwareWallet && it
            invalidateMenu()
        }.launchIn(lifecycleScope)

        viewModel.accountAsset.onEach {
            invalidateMenu()
        }.launchIn(lifecycleScope)

        binding.buttonConfirm.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.CreateInvoice)
            hideKeyboard()
        }

        listOf(binding.buttonAmountCurrency, binding.amountCurrency).setOnClickListener {
            lifecycleScope.launch {
                UserInput.parseUserInputSafe(
                    viewModel.session,
                    viewModel.amount.value,
                    assetId = viewModel.session.lightning?.policyAsset,
                    denomination = viewModel.denomination.value

                ).getBalance().also {
                    DenominationBottomSheetDialogFragment.show(
                        denominatedValue = DenominatedValue(
                            balance = it,
                            assetId = viewModel.session.lightning?.policyAsset,
                            denomination = viewModel.denomination.value
                        ),
                        childFragmentManager)
                }
            }
        }

        binding.buttonEditAmount.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.ClearLightningInvoice)
        }

        binding.buttonAmountPaste.setOnClickListener {
            viewModel.amount.value = getClipboard(requireContext()) ?: ""
        }

        binding.buttonAmountClear.setOnClickListener {
            viewModel.amount.value = ""
        }

        binding.invoiceExpiration.setOnClickListener {
            viewModel.invoiceExpirationTimestamp.value?.also {
                 snackbar(Date(it).formatFullWithTime())
            }
        }

        viewModel.invoiceExpirationTimestamp.filterNotNull().onEach {
            binding.invoiceExpiration.text = DateUtils.getRelativeTimeSpanString(
                it,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()
        }.launchIn(lifecycleScope)

        binding.buttonLearnMore.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.ClickFundingFeesLearnMore)
        }

        binding.buttonOnChainToggle.setOnClickListener {
            viewModel.postEvent(ReceiveViewModel.LocalEvents.ToggleLightning)
            hideKeyboard()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    private fun showWaitingToast() {
        viewModel.session.device?.let { device ->
            if (viewModel.account.network.isLiquid && device.isLedger) {
                toast(R.string.id_please_wait_until_your_ledger)
            } else {
                toast(R.string.id_please_hold_on_while_your)
            }
        }
    }

    private fun createQRImageAndShare(address: String) {
        createQrBitmap(address)?.also { qrBitmap ->
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

                val staticLayout = StaticLayout.Builder.obtain(
                    address,
                    0,
                    address.length,
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
        menu.findItem(R.id.add_description).isVisible = viewModel.account.isLightning
        menu.findItem(R.id.add_description).isEnabled = !viewModel.onProgress.value
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.help -> {
                openBrowser(if (viewModel.account.isAmp) Urls.HELP_AMP_ASSETS else Urls.HELP_RECEIVE_ASSETS)
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
            viewModel.postEvent(if (position == 0) ReceiveViewModel.LocalEvents.ShareAddress else ReceiveViewModel.LocalEvents.ShareQR) //
        } else if (requestCode == 2 && item is MenuListItem) {

            when (item.id) {
                0 -> {
                    RequestAmountLabelBottomSheetDialogFragment.show(childFragmentManager)
                }
                1 -> {
                    navigate(
                        ReceiveFragmentDirections.actionReceiveFragmentToPreviousAddressesFragment(
                            wallet = viewModel.greenWallet,
                            account = viewModel.account
                        )
                    )
                }
                2 -> {
                    navigate(
                        ReceiveFragmentDirections.actionReceiveFragmentToSendFragment(
                            wallet = viewModel.greenWallet,
                            accountAsset = viewModel.accountAsset.value!!,
                            isSweep = true
                        )
                    )
                }
            }
        }
    }

    override fun createNewAccountClicked(asset: EnrichedAsset) {
        navigate(
            ReceiveFragmentDirections.actionGlobalChooseAccountTypeFragment(
                wallet = viewModel.greenWallet,
                asset = asset
            )
        )
    }
}
