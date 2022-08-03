package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.blockstream.base.Urls
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.AddWalletFragmentBinding
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.bottomsheets.AbstractBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.ConsentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DismissBottomSheetDialogListener
import com.blockstream.green.ui.bottomsheets.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuDataProvider
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.GenericItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddWalletFragment :
    AbstractOnboardingFragment<AddWalletFragmentBinding>(R.layout.add_wallet_fragment, menuRes = 0),
    DismissBottomSheetDialogListener, MenuDataProvider {

    private var pendingNavigation: NavDirections? = null
    val args: AddWalletFragmentArgs by navArgs()

    override val screenName = "OnBoardIntro"

    @Inject
    lateinit var assistedFactory: AddWalletViewModel.AssistedFactory

    private val viewModel: AddWalletViewModel by viewModels {
        AddWalletViewModel.provideFactory(assistedFactory, args.deviceId)
    }

    override fun getAppViewModel(): AppViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.termsChecked.value = isChecked
        }

        binding.buttonContinueHardware.setOnClickListener {
            settingsManager.setDeviceTermsAccepted()
            setNavigationResult(result = args.network)
            popBackStack()
        }

        binding.buttonNewWallet.setOnClickListener {
            if (settingsManager.getApplicationSettings().testnet){
                askForNetworkEnviroment(0)
            }else{
                askForAnalyticsConsentAndNavigate(
                    AddWalletFragmentDirections.actionAddWalletFragmentToRecoveryIntroFragment(onboardingOptions = OnboardingOptions(isRestoreFlow = false))
                )
            }

            countly.newWallet()
        }

        binding.buttonRestoreWallet.setOnClickListener {

            if (settingsManager.getApplicationSettings().testnet){
                askForNetworkEnviroment(1)
            }else{
                askForAnalyticsConsentAndNavigate(
                    AddWalletFragmentDirections.actionAddWalletFragmentToEnterRecoveryPhraseFragment(OnboardingOptions(isRestoreFlow = true))
                )
            }

            countly.restoreWallet()
        }

        binding.buttonWatchOnly.setOnClickListener {
            askForAnalyticsConsentAndNavigate(AddWalletFragmentDirections.actionAddWalletFragmentToChooseWatchOnlyFragment())
            countly.watchOnlyWallet()
        }

        binding.termsLink.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.TERMS_OF_SERVICE)
        }
    }

    private fun askForNetworkEnviroment(requestCode: Int){
        MenuBottomSheetDialogFragment.show(requestCode = requestCode, title = getString(R.string.id_select_network), menuItems = listOf(
                MenuListItem(
                    icon = R.drawable.ic_regular_currency_btc_24,
                    title = StringHolder("Mainnet")
                ),
                MenuListItem(
                    icon = R.drawable.ic_regular_flask_24,
                    title = StringHolder("Testnet")
                )
            ), fragmentManager = childFragmentManager
        )
    }

    override fun menuItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        val onboardingOptions = OnboardingOptions(isRestoreFlow = requestCode == 1, isTestnet = position == 1)

        askForAnalyticsConsentAndNavigate(
            if(onboardingOptions.isRestoreFlow){
                AddWalletFragmentDirections.actionAddWalletFragmentToEnterRecoveryPhraseFragment(onboardingOptions = onboardingOptions)
            }else{
                AddWalletFragmentDirections.actionAddWalletFragmentToRecoveryIntroFragment(onboardingOptions = onboardingOptions)
            }
        )
    }

    private fun askForAnalyticsConsentAndNavigate(directions: NavDirections){
        if(ConsentBottomSheetDialogFragment.shouldShowConsentDialog(countly, settingsManager)){
            pendingNavigation = directions
            ConsentBottomSheetDialogFragment.show(childFragmentManager)
        }else{
            navigate(directions)
        }
    }

    override fun dialogDismissed(dialog: AbstractBottomSheetDialogFragment<*>) {
        pendingNavigation?.let { navigate(it) }
    }
}