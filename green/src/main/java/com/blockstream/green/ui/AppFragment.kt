package com.blockstream.green.ui


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.blockstream.common.ScreenView
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.handleException
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.getStringFromId
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.BannerView
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.errorSnackbar
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.extensions.stringFromIdentifier
import com.blockstream.green.extensions.stringFromIdentifierOrNull
import com.blockstream.green.ui.bottomsheets.AccountAssetListener
import com.blockstream.green.ui.bottomsheets.DenominationBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DeviceInteractionRequestBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.PassphraseBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.PinMatrixBottomSheetDialogFragment
import com.blockstream.green.ui.drawer.DrawerFragment
import com.blockstream.green.utils.BannersHelper
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.GreenAlertView
import com.blockstream.green.views.GreenToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


/**
 * AppFragment
 *
 * This class is a useful abstract base class. Extend all other Fragments if possible.
 * Some of the features can be turned on/off in the constructor.
 *
 * @property layout the layout id of the fragment
 * is called when the fragment is not actually visible
 *
 */

abstract class AppFragment<T : ViewDataBinding>(
    @LayoutRes val layout: Int,
    @MenuRes val menuRes: Int = 0
) : Fragment(), MenuProvider, ScreenView, BannerView, AccountAssetListener {
    open val isAdjustResize = false

    internal lateinit var binding: T

    val countly: CountlyAndroid by inject()

    protected val gdk: Gdk by inject()

    protected val wally: Wally by inject()

    protected val sessionManager: SessionManager by inject()

    val zendeskSdk: ZendeskSdk by inject()

    val settingsManager: SettingsManager by inject()
    open fun getGreenViewModel(): GreenViewModel? = null

    open val title : String?
        get() = getGreenViewModel()?.navData?.value?.title

    open val subtitle : String?
        get() = getGreenViewModel()?.navData?.value?.subtitle

    open val toolbarIcon: Int? = null

    override val screenName: String? = null
    override var screenIsRecorded = false
    override val segmentation: HashMap<String, Any>? = null

    protected val toolbar: GreenToolbar
        get() = (requireActivity() as AppActivity).toolbar

    open val useCompose : Boolean = false

    open fun updateToolbar() {
        val navData: NavData? = getGreenViewModel()?.navData?.value
        (navData?.title ?: title)?.let {
            toolbar.title = it
        }
        toolbar.subtitle = (navData?.subtitle ?: subtitle)
        toolbar.logo = null
        // Only show toolbar icon if it's overridden eg. add account flow
        toolbarIcon?.let { toolbar.setLogo(it) }
        toolbar.setBubble(null)
        toolbar.setButton(null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, layout, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add MenuProvider if required
        if (menuRes > 0) {
            (requireActivity() as? MenuHost)?.addMenuProvider(
                this,
                viewLifecycleOwner,
                Lifecycle.State.RESUMED
            )
        }

        getGreenViewModel()?.let { viewModel ->

            viewModel.banner.onEach {
                BannersHelper.setupBanner(this, it)
            }.launchIn(lifecycleScope)

            if(useCompose){
                viewModel.navData.onEach {
                    updateToolbar()
                    invalidateMenu()
                }.launchIn(lifecycleScope)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    if(!useCompose){
                        viewModel.sideEffect.onEach {
                            // Consume side effects
                        }.launchIn(this)
                    }
                    viewModel.sideEffectAppFragment.onEach {
                        handleSideEffect(it)
                    }.launchIn(this)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Prevent DrawerFragment from corrupting the main fragment
        if (this !is DrawerFragment) {
            @Suppress("DEPRECATION")
            requireActivity().window.setSoftInputMode(if (isAdjustResize) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            updateToolbar()
            countly.screenView(this)
        }

        BannersHelper.handle(this, getGreenViewModel()?.sessionOrNull)
    }

    override fun onPrepareMenu(menu: Menu) {
        // Handle for example visibility of menu items
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        menuInflater.inflate(menuRes, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        // Validate and handle the selected menu item
        return true
    }

    protected fun invalidateMenu(){
        (requireActivity() as? MenuHost)?.invalidateMenu()
    }

    protected fun closeDrawer() {
        (requireActivity() as AppActivity).closeDrawer()
    }

    protected fun isDrawerOpen() = (requireActivity() as AppActivity).isDrawerOpen()

    fun setToolbarVisibility(isVisible: Boolean){
        (requireActivity() as AppActivity).setToolbarVisibility(isVisible)
    }

    fun navigate(directions: NavDirections, navOptionsBuilder: NavOptions.Builder? = null) {
        navigate(directions.actionId, directions.arguments, false, navOptionsBuilder)
    }

    fun navigate(@IdRes resId: Int, navOptionsBuilder: NavOptions.Builder? = null) {
        navigate(resId, null, false, navOptionsBuilder)
    }

    @SuppressLint("RestrictedApi")
    fun navigate(@IdRes resId: Int, args: Bundle?, isLogout: Boolean = false, optionsBuilder: NavOptions.Builder? = null) {
        com.blockstream.green.extensions.navigate(
            findNavController(),
            resId,
            args,
            isLogout,
            optionsBuilder
        )
    }

    internal open fun popBackStack(){
        findNavController().popBackStack()
    }

    internal open fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean){
        findNavController().popBackStack(destinationId, inclusive)
    }

    open suspend fun handleSideEffect(sideEffect: SideEffect){
        when (sideEffect){
            is SideEffects.OpenBrowser -> {
                if(!useCompose) {
                    openBrowser(sideEffect.url)
                }
            }
            is SideEffects.Snackbar -> {
                // Snackbar is implemented in compose but LocalSnackbar is only available in GreenApp

                lifecycleScope.launch {
                    view?.also {
                        Snackbar.make(
                            it,
                            sideEffect.text.getString(),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            is SideEffects.Dialog -> {
                if (!useCompose) {
                    MaterialAlertDialogBuilder(requireContext()).setTitle(
                            requireContext().stringFromIdentifierOrNull(
                                sideEffect.title?.getString()
                            )
                        ).setMessage(requireContext().stringFromIdentifier(sideEffect.message.getString()))
                        .setPositiveButton(android.R.string.ok, null).show()
                }
            }
            is SideEffects.ErrorSnackbar -> {
                if(!useCompose) {
                    errorSnackbar(
                        throwable = sideEffect.error,
                        errorReport = sideEffect.errorReport,
                        duration = Snackbar.LENGTH_LONG
                    )
                }
            }
            is SideEffects.ErrorDialog -> {
                if(!useCompose) {
                    errorDialog(
                        throwable = sideEffect.error,
                        errorReport = sideEffect.errorReport,
                    )
                }
            }
            is SideEffects.DeviceRequestPin -> {
                PinMatrixBottomSheetDialogFragment.show(childFragmentManager)
            }
            is SideEffects.DeviceRequestPassphrase -> {
                PassphraseBottomSheetDialogFragment.show(childFragmentManager)
            }
            is SideEffects.DeviceInteraction -> {

                DeviceInteractionRequestBottomSheetDialogFragment.showSingle(
                    device = sideEffect.device,
                    message = sideEffect.message,
                    delay = (3000L).takeIf { sideEffect.completable == null } ?: 0L,
                    childFragmentManager
                )

                sideEffect.completable?.also { completable ->
                    lifecycleScope.launch(context = handleException()) {
                        completable.await()
                        withResumed {
                            DeviceInteractionRequestBottomSheetDialogFragment.closeAll(
                                childFragmentManager
                            )
                        }
                    }
                }
            }
            is SideEffects.OpenDenomination -> {
                if(!useCompose) {
                    DenominationBottomSheetDialogFragment.show(
                        denominatedValue = sideEffect.denominatedValue,
                        childFragmentManager
                    )
                }
            }

            is SideEffects.NavigateToRoot -> {
                if (sideEffect.popToReceive) {
                    findNavController().popBackStack(R.id.receiveFragment, false)
                } else {
                    findNavController().popBackStack(R.id.walletOverviewFragment, false)
                }
            }

            is SideEffects.NavigateBack -> {
                if (sideEffect.error != null) {
                    errorDialog(sideEffect.error!!, errorReport = sideEffect.errorReport) {
                        popBackStack()
                    }
                } else if (sideEffect.message != null) {
                    dialog(
                        title = sideEffect.title?.getString() ?: "",
                        message = sideEffect.message?.getString() ?: ""
                    ) {
                        popBackStack()
                    }
                } else {
                    popBackStack()
                }
            }
            is SideEffects.CopyToClipboard -> {
                if(!useCompose) {
                    copyToClipboard(sideEffect.label ?: "Green", sideEffect.value, requireContext())

                    sideEffect.message?.also {
                        snackbar(requireContext().stringFromIdentifier(it))
                    }
                }
            }
            is SideEffects.Logout -> {
                logger.d { "Logout sideeffect: $sideEffect" }
                getGreenViewModel()?.greenWalletOrNull?.let {
                    // If is ephemeral wallet, prefer going to intro
                    if (it.isEphemeral || it.isHardware || sideEffect.reason == LogoutReason.USER_ACTION) {
                        NavGraphDirections.actionGlobalHomeFragment()
                    } else {
                        NavGraphDirections.actionGlobalLoginFragment(it)
                    }.let { directions ->
                        navigate(directions.actionId, directions.arguments, isLogout = true)
                    }

                    true
                } ?: run {
                    NavGraphDirections.actionGlobalHomeFragment().also {
                        navigate(it.actionId, it.arguments, isLogout = true)
                    }
                }
            }

            is SideEffects.NavigateTo -> {
                (sideEffect.destination as? NavigateDestinations.AddWallet)?.also {
                    navigate(NavGraphDirections.actionGlobalAddWalletFragment())
                }

                (sideEffect.destination as? NavigateDestinations.About)?.also {
                    navigate(NavGraphDirections.actionGlobalAboutFragment())
                }

                (sideEffect.destination as? NavigateDestinations.UseHardwareDevice)?.also {
                    navigate(NavGraphDirections.actionGlobalUseHardwareDeviceFragment())
                }

                (sideEffect.destination as? NavigateDestinations.WatchOnlyPolicy)?.also {
                    navigate(NavGraphDirections.actionGlobalWatchOnlyPolicyFragment())
                }

                (sideEffect.destination as? NavigateDestinations.WatchOnlyNetwork)?.also {
                    navigate(NavGraphDirections.actionGlobalWatchOnlyNetworkFragment(setupArgs = it.setupArgs))
                }

                (sideEffect.destination as? NavigateDestinations.WatchOnlyCredentials)?.also {
                    navigate(NavGraphDirections.actionGlobalWatchOnlyCredentialsFragment(setupArgs = it.setupArgs))
                }

                (sideEffect.destination as? NavigateDestinations.AppSettings)?.also {
                    navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
                }

                (sideEffect.destination as? NavigateDestinations.Receive)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalReceiveFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            accountAsset = it.accountAsset
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.Sweep)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalSweepFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            accountAsset = it.accountAsset,
                            privateKey = it.privateKey
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.Send)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalSendFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            address = it.address,
                            addressType = it.addressType
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.AccountExchange)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalAccountExchangeFragment(
                            wallet = getGreenViewModel()!!.greenWallet
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.Bump)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalBumpFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            accountAsset = it.accountAsset,
                            transaction = it.transaction
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.SendConfirm)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalSendConfirmFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            accountAsset = it.accountAsset,
                            denomination = it.denomination
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.TwoFactorSetup)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalTwoFactorSetupFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            method = it.method,
                            action = it.action,
                            network = it.network,
                            isSmsBackup = it.isSmsBackup
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.LnUrlAuth)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalLnUrlAuthFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            lnUrlAuthRequest = it.lnUrlAuthRequest,
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.LnUrlWithdraw)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalLnUrlWithdrawFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            lnUrlWithdrawRequest = it.lnUrlWithdrawRequest,
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ArchivedAccounts)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalArchivedAccountsFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            navigateToOverview = it.navigateToRoot
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.RecoveryIntro)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalRecoveryIntroFragment(
                            setupArgs = it.setupArgs
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ReEnable2FA)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalReEnable2faFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.Redeposit)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalRedepositFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            accountAsset = it.accountAsset,
                            isRedeposit2FA = it.isRedeposit2FA
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.WalletAssets)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalAssetsFragment(
                            wallet = getGreenViewModel()!!.greenWallet
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.AccountOverview)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalAccountOverviewFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            accountAsset = it.accountAsset
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.ChooseAccountType)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalChooseAccountTypeFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            isReceive = it.isReceive,
                            asset = it.assetBalance?.asset
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.Transaction)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalTransactionDetailsFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            transaction = it.transaction
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.RecoverFunds)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalRecoverFundsFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            amount = it.amount,
                            address = it.address,
                            isSendAll = it.isSendAll
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.TwoFactorAuthentication)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalTwoFractorAuthenticationFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            network = it.network
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.JadeQR)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalJadeQrFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            psbt = it.psbt,
                            isLightningMnemonicExport = it.isLightningMnemonicExport
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.ReviewAddAccount)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalReviewAddAccountFragment(
                            setupArgs = it.setupArgs,
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.AddAccount2of3)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalAccount2of3Fragment(
                            setupArgs = it.setupArgs,
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.Xpub)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalXpubFragment(
                            setupArgs = it.setupArgs
                        )
                    )
                }
                (sideEffect.destination as? NavigateDestinations.EnterRecoveryPhrase)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalEnterRecoveryPhraseFragment(
                            setupArgs = it.setupArgs
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.RecoveryWords)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalRecoveryWordsFragment(
                            args = it.setupArgs
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.RecoveryCheck)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalRecoveryCheckFragment(
                            args = it.setupArgs,
                        ), navOptionsBuilder = NavOptions.Builder().also {
                            it.setPopUpTo(R.id.recoveryIntroFragment, false)
                        }
                    )
                }

                (sideEffect.destination as? NavigateDestinations.WalletSettings)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalWalletSettingsFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            showRecoveryTransactions = it.section == WalletSettingsSection.RecoveryTransactions,
                            network = it.network
                        )
                    )
                }

                (sideEffect.destination as? NavigateDestinations.Addresses)?.also {
                    navigate(
                        NavGraphDirections.actionGlobalPreviousAddressesFragment(
                            wallet = getGreenViewModel()!!.greenWallet,
                            account = it.accountAsset.account,
                        )
                    )
                }
            }
        }
    }

    override fun getBannerAlertView(): GreenAlertView? = null

    override fun accountAssetClicked(accountAsset: AccountAsset) {
        getGreenViewModel()?.also {
            it.accountAsset.value = accountAsset
        }
    }

    companion object: Loggable()
}