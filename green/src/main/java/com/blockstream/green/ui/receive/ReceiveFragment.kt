package com.blockstream.green.ui.receive

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.ReceiveFragmentBinding
import com.blockstream.green.ui.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.MenuDataProvider
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.*
import com.mikepenz.fastadapter.GenericItem
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ReceiveFragment : WalletFragment<ReceiveFragmentBinding>(
    layout = R.layout.receive_fragment,
    menuRes = R.menu.menu_help
) {
    val args: ReceiveFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    @Inject
    lateinit var viewModelFactory: ReceiveViewModel.AssistedFactory
    val viewModel: ReceiveViewModel by viewModels {
        ReceiveViewModel.provideFactory(
            viewModelFactory,
            wallet
        )
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
            showWaitingToast()
        }
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        binding.address.setOnClickListener {
            copyToClipboard("Address", binding.address.text.toString(), requireContext(), animateView = binding.address)
            snackbar(R.string.id_address_copied_to_clipboard)
        }

        binding.buttonShare.setOnClickListener {
            MenuBottomSheetDialogFragment(object : MenuDataProvider {
                override fun getTitle() = getString(R.string.id_share)
                override fun getSubtitle() = null

                override fun getMenuListItems() = listOf(
                    MenuListItem(icon = R.drawable.ic_baseline_text_fields_24, title = StringHolder(R.string.id_address)),
                    MenuListItem(icon = R.drawable.ic_qr_60 , title = StringHolder(R.string.id_qr_code))
                )

                override fun menuItemClicked(item: GenericItem, position: Int) {
                    if(position == 0){
                        share(binding.address.text.toString())
                    }else{
                        createQRImage()
                    }
                }
            }).show(childFragmentManager)
        }

        binding.buttonMore.setOnClickListener {
            MenuBottomSheetDialogFragment(object : MenuDataProvider {
                override fun getTitle() = getString(R.string.id_more_options)
                override fun getSubtitle() = null

                override fun getMenuListItems() = buildList {
                    add(MenuListItem(title = StringHolder(R.string.id_request_amount)))
                    if(!session.isLiquid && !session.isElectrum) {
                        add(MenuListItem(title = StringHolder(R.string.id_sweep_from_paper_wallet)))
                    }
                }

                override fun menuItemClicked(item: GenericItem, position: Int) {
                    if(position == 0){
                        RequestAmountLabelBottomSheetDialogFragment().also {
                            it.show(childFragmentManager, it.toString())
                        }
                    }else{
                        navigate(
                            ReceiveFragmentDirections.actionReceiveFragmentToSendFragment(
                                wallet = wallet,
                                isSweep =  true
                            )
                        )
                    }
                }
            }).show(childFragmentManager)

        }

        binding.buttonNewAddress.setOnClickListener {
            if(viewModel.onProgress.value == false){
                viewModel.generateAddress()
                binding.buttonNewAddress.rotate()
            }else{
                showWaitingToast()
            }
        }

        binding.buttonEdit.setOnClickListener {
            RequestAmountLabelBottomSheetDialogFragment().also {
                it.show(childFragmentManager, it.toString())
            }
        }

        binding.materialCardView.setOnClickListener {
            copyToClipboard("Address", binding.address.text.toString(), requireContext())
            snackbar(R.string.id_address_copied_to_clipboard)
            it.pulse()
        }

        binding.assetWhitelistWarning.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.LEDGER_SUPPORTED_ASSETS)
        }

        binding.buttonVerify.setOnClickListener {
            VerifyAddressBottomSheetDialogFragment().also {
                it.show(childFragmentManager, it.toString())
            }

            if(viewModel.onProgress.value == false) {
                viewModel.validateAddressInDevice()
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.addressQRBitmap.observe(viewLifecycleOwner) {
            binding.addressQR.setImageDrawable(BitmapDrawable(resources, it).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        }

        viewModel.onProgress.observe(viewLifecycleOwner) {
            // On HWWallet Block going back until address is generated
            onBackCallback.isEnabled = session.hasDevice && it
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    private fun showWaitingToast() {
        session.hwWallet?.device?.let { device ->
            if(session.isLiquid && device.isLedger){
                toast(R.string.id_please_wait_until_your_ledger)
            }else{
                toast(R.string.id_please_hold_on_while_your)
            }
        }
    }

    private fun createQRImage() {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.help -> {
                openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_RECEIVE_ASSETS)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}