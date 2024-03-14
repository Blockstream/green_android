package com.blockstream.green.ui.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.Countries.Countries
import com.blockstream.common.data.Country
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.TwoFactorSetupViewModel
import com.blockstream.compose.utils.stringResourceId
import com.blockstream.green.R
import com.blockstream.green.databinding.TwofactorSetupFragmentBinding
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.openKeyboard
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.FilterableDataProvider
import com.blockstream.green.ui.dialogs.QrDialogFragment
import com.blockstream.green.ui.items.CountryListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.utils.linkedText
import com.blockstream.green.utils.pulse
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class TwoFactorSetupFragment : AppFragment<TwofactorSetupFragmentBinding>(R.layout.twofactor_setup_fragment, 0),
    FilterableDataProvider {
    val args: TwoFactorSetupFragmentArgs by navArgs()

    val network: Network by lazy { args.network }

    override val subtitle: String
        get() = getString(R.string.id_multisig)

    override val toolbarIcon: Int
        get() = network.getNetworkIcon()


    val viewModel: TwoFactorSetupViewModel by viewModel {
        parametersOf(args.wallet, args.network, args.method, args.action, args.isSmsBackup)
    }

    override val title: String
        get() = stringResourceId(requireContext(), viewModel.navData.value.title ?: "")


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val action = args.action

        binding.vm = viewModel

        binding.text1.movementMethod = LinkMovementMethod.getInstance()
        binding.text1.linksClickable = true
        binding.text1.isClickable = true
        binding.text1.text = requireContext().linkedText(
            text = R.string.id_by_continuing_you_agree_to_blockstream_s,
            color = R.color.color_on_surface_emphasis_medium,
            links = listOf(
                R.string.id_terms_of_service to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.ClickTermsOfService())
                    }
                },
                R.string.id_privacy_policy to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.ClickPrivacyPolicy())
                    }
                }
            )
        )

        binding.text3.movementMethod = LinkMovementMethod.getInstance()
        binding.text3.linksClickable = true
        binding.text3.isClickable = true
        binding.text3.text = requireContext().linkedText(
            text = R.string.id_for_help_visit_help_blockstream_com,
            color = R.color.color_on_surface_emphasis_medium,
            links = listOf(
                R.string.id_help_blockstream_com to object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.ClickHelp())
                    }
                }
            )
        )
        
        when(action){
            TwoFactorSetupAction.SETUP -> {
                binding.message =  if(args.method == TwoFactorMethod.EMAIL){
                    getString(R.string.id_insert_your_email_to_receive)
                }else{
                    getString(R.string.id_insert_your_phone_number_to)
                }
                binding.button = getString(R.string.id_continue)
            }
            TwoFactorSetupAction.SETUP_EMAIL -> {
                binding.message = getString(R.string.id_use_your_email_to_receive)
                binding.button = getString(R.string.id_continue)
            }
            TwoFactorSetupAction.RESET -> {
                binding.message = getString(R.string.id_resetting_your_twofactor_takes)
                binding.button = getString(R.string.id_request_twofactor_reset)
            }
            TwoFactorSetupAction.CANCEL -> {
                // Cancel action
                viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.Cancel2FA(DialogTwoFactorResolver(this)))
            }
            TwoFactorSetupAction.DISPUTE -> {
                binding.message = getString(R.string.id_if_you_did_not_request_the)
                binding.button = getString(R.string.id_dispute_twofactor_reset)
            }
            TwoFactorSetupAction.UNDO_DISPUTE -> {
                binding.message = getString(R.string.id_if_you_initiated_the_2fa_reset)
                binding.button = getString(R.string.id_undo_2fa_dispute)
            }
        }

        binding.buttonContinue.setOnClickListener {
            hideKeyboard()
            when(action){
                TwoFactorSetupAction.RESET -> {
                    viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.Reset2FA(DialogTwoFactorResolver(this)))
                }
                TwoFactorSetupAction.DISPUTE -> {
                    viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.Reset2FA(DialogTwoFactorResolver(this)))
                }
                TwoFactorSetupAction.UNDO_DISPUTE -> {
                    viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.UndoReset2FA(DialogTwoFactorResolver(this)))
                }
                TwoFactorSetupAction.SETUP,TwoFactorSetupAction.SETUP_EMAIL  -> {
                    viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.Enable2FA(DialogTwoFactorResolver(this)))
                }
                TwoFactorSetupAction.CANCEL -> {
                    // Cancel is triggered immediately
                }
            }
        }

        binding.authenticatorCode.setOnClickListener {
            copyToClipboard("Address", viewModel.authenticatorCode.value ?: "", requireContext())
            snackbar(R.string.id_copied_to_clipboard)
            binding.authenticatorCode.pulse()
        }

        binding.countryEditText.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                openCountryFilter()
            }
        }

        viewModel.qr.filterNotNull().onEach {
            binding.authenticatorQR.setImageDrawable(BitmapDrawable(resources, createQrBitmap(it)).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        }.launchIn(lifecycleScope)

        binding.authenticatorQR.setOnLongClickListener {
            viewModel.qr.value?.let { createQrBitmap(it) }?.also {
                QrDialogFragment.show(it, childFragmentManager)
            }
            true
        }
    }

    private fun openCountryFilter(){
        FilterBottomSheetDialogFragment.show(fragmentManager = childFragmentManager)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {
        val adapter = ModelAdapter<Country, CountryListItem>() {
            CountryListItem(it)
        }.set(Countries)

        adapter.itemFilter.filterPredicate = { item: CountryListItem, constraint: CharSequence? ->
            item.country.name.lowercase().contains(
                constraint.toString().lowercase()
            )
        }

        return adapter
    }

    override fun filteredItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        binding.countryEditText.setText((item as CountryListItem).country.dialCodeString)
        binding.phoneNumberEditText.requestFocus()
        openKeyboard()
    }

    override fun getFilterHeaderAdapter(requestCode: Int): GenericFastItemAdapter? = null
    override fun getFilterFooterAdapter(requestCode: Int): GenericFastItemAdapter? = null
}