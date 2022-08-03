package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.base.IAppReview
import com.blockstream.base.Urls
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.ApplicationScope
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemWalletBalanceBinding
import com.blockstream.green.databinding.WalletOverviewFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.logException
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.add.AbstractAddAccountFragment.Companion.SET_ACCOUNT
import com.blockstream.green.ui.bottomsheets.Call2ActionBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.ConsentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuDataProvider
import com.blockstream.green.ui.bottomsheets.RenameAccountBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.SystemMessageBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.TwoFactorResetBottomSheetDialogFragment
import com.blockstream.green.ui.items.AccountListItem
import com.blockstream.green.ui.items.AccountsListItem
import com.blockstream.green.ui.items.AlertListItem
import com.blockstream.green.ui.items.AlertType
import com.blockstream.green.ui.items.AppReviewListItem
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.items.TransactionListItem
import com.blockstream.green.ui.items.WalletBalanceListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.AppReviewHelper
import com.blockstream.green.utils.BannersHelper
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.AccordionListener
import com.blockstream.green.views.NpaLinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates


@AndroidEntryPoint
class WalletOverviewFragment : AbstractWalletFragment<WalletOverviewFragmentBinding>(
    layout = R.layout.wallet_overview_fragment,
    menuRes = R.menu.wallet_overview
), MenuDataProvider{

    val args: WalletOverviewFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var appReview: IAppReview

    @Inject
    lateinit var viewModelFactory: WalletOverviewViewModel.AssistedFactory
    val viewModel: WalletOverviewViewModel by viewModels {
        WalletOverviewViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    var fastAdapter: FastAdapter<GenericItem>? = null

    // Prevent ViewModel initialization if session is not initialized
    override val title: String
        get() = if (isSessionNetworkInitialized) viewModel.wallet.name else ""


    var showAccountInToolbar: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateToolbar()
        }
    }

    override val screenName = "WalletOverview"

    companion object {
        const val BROADCASTED_TRANSACTION = "BROADCASTED_TRANSACTION"
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        getNavigationResult<AccountAsset>(SET_ACCOUNT)?.observe(viewLifecycleOwner) { accountAssetOrNull ->
            accountAssetOrNull?.let { accountAsset ->
                viewModel.setActiveAccount(account = accountAsset.account)
                clearNavigationResult(SET_ACCOUNT)
                //viewModel.expandedAccount.postValue(accountAsset.account)
            }
        }

        getNavigationResult<Boolean>(BROADCASTED_TRANSACTION)?.observe(viewLifecycleOwner) { isSendAll ->
            // Avoid requesting for review on send all transactions
            if (isSendAll == false) {
                if (AppReviewHelper.shouldAskForReview(settingsManager, countly)) {
                    applicationScope.launch(context = logException(countly)) {
                        delay(1000)
                        viewModel.setAppReview(true)
                    }
                }
                clearNavigationResult(BROADCASTED_TRANSACTION)
            }
        }

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) {
            it?.let { result ->
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                openProposal(result)
            }
        }

        // Handle pending BIP-21 uri
        sessionManager.pendingBip21Uri.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { bip21Uri ->
                navigate(
                    WalletOverviewFragmentDirections.actionWalletOverviewFragmentToSendFragment(
                        wallet = wallet, address = bip21Uri
                    )
                )
                snackbar(R.string.id_address_was_filled_by_a_payment)
            }
        }

        binding.vm = viewModel

        binding.bottomNav.isWatchOnly = wallet.isWatchOnly

        viewModel.accountsFlow.onEach {
            (if(it.isEmpty()) 0.2f else 1.0f).also { alpha ->
                binding.recycler.alpha = alpha
                binding.bottomNav.root.alpha = alpha
            }
            setToolbarVisibility(it.isNotEmpty())
        }.launchIn(lifecycleScope)

        viewModel.archivedAccountsLiveData.observe(viewLifecycleOwner){
            invalidateMenu()
        }

        val fastAdapter = setupAdapters()

        binding.buttonCreateAccount.setOnClickListener {
            navigate(
                WalletOverviewFragmentDirections.actionGlobalChooseAccountTypeFragment(
                    wallet
                )
            )
            countly.firstAccount(session)
        }

        binding.bottomNav.buttonReceive.setOnClickListener {
            navigate(
                WalletOverviewFragmentDirections.actionWalletOverviewFragmentToReceiveFragment(
                    wallet = viewModel.wallet
                )
            )
        }

        binding.bottomNav.buttonSwap.setOnClickListener {
            MenuBottomSheetDialogFragment.show(title = getString(R.string.id_choose_a_swap_option), menuItems = ArrayList(listOf(
                MenuListItem(icon = R.drawable.ic_swap, title = StringHolder(R.string.id_create_a_new_proposal)),
                MenuListItem(icon = R.drawable.ic_clipboard , title = StringHolder(R.string.id_paste_an_existing_proposal)),
                MenuListItem(icon = R.drawable.ic_qr_code , title = StringHolder(R.string.id_scan_a_proposal)),
            )), fragmentManager = childFragmentManager)
        }

        binding.bottomNav.buttonSend.setOnClickListener {
            when {
                session.isWatchOnly -> {
                    navigate(
                        WalletOverviewFragmentDirections.actionWalletOverviewFragmentToSendFragment(
                            wallet = wallet,
                            isSweep = true
                        )
                    )
                }
//                ((viewModel.walletAssets[session.bitcoin] ?: 0L) == 0L && (viewModel.walletAssets[session.liquid] ?: 0L) == 0L) -> {
//                    MaterialAlertDialogBuilder(requireContext())
//                        .setTitle(R.string.id_warning)
//                        .setMessage(if (network.isLiquid) R.string.id_insufficient_lbtc_to_send_a else R.string.id_you_have_no_coins_to_send)
//                        .also {
//                            if (network.isLiquid) {
//                                it.setPositiveButton(R.string.id_learn_more) { _: DialogInterface, _: Int ->
//                                    openBrowser(Urls.HELP_GET_LIQUID)
//                                }
//                            } else {
//                                it.setPositiveButton(R.string.id_receive) { _: DialogInterface, _: Int ->
//                                    navigate(
//                                        WalletOverviewFragmentDirections.actionWalletOverviewFragmentToSendFragment(
//                                            wallet = viewModel.wallet
//                                        )
//                                    )
//                                }
//                            }
//                        }
//                        .setNegativeButton(R.string.id_cancel, null)
//                        .show()
//                }
                else -> {
                    navigate(
                        WalletOverviewFragmentDirections.actionWalletOverviewFragmentToSendFragment(
                            wallet = wallet
                        )
                    )
                }
            }
        }

        fastAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.recycler.apply {
            layoutManager = NpaLinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.refresh()
        }

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                showAccountInToolbar =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 0
            }
        })

        if (ConsentBottomSheetDialogFragment.shouldShowConsentDialog(countly, settingsManager)) {
            applicationScope.launch(context = logException(countly)) {
                delay(1500)
                ConsentBottomSheetDialogFragment.show(childFragmentManager)
            }
        }

        (requireActivity() as MainActivity).askForNotificationPermissionIfNeeded()
    }

    override fun onPrepareMenu(menu: Menu) {
        // Prevent from archiving all your acocunts
        menu.findItem(R.id.create_account).isVisible = !session.isWatchOnly
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.settings -> {
                navigate(
                    WalletOverviewFragmentDirections.actionWalletOverviewFragmentToWalletSettingsFragment(
                        wallet
                    )
                )
                return true
            }
            R.id.create_account -> {
                navigate(
                    WalletOverviewFragmentDirections.actionGlobalChooseAccountTypeFragment(
                        wallet
                    )
                )
                countly.accountNew(session)
            }
            R.id.logout -> {
                viewModel.logout(AbstractWalletViewModel.LogoutReason.USER_ACTION)
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    override fun onResume() {
        super.onResume()

        requireActivity().window.navigationBarColor =
            ContextCompat.getColor(requireContext(), R.color.brand_surface_variant)
    }

    override fun onPause() {
        super.onPause()

        requireActivity().window.navigationBarColor =
            ContextCompat.getColor(requireContext(), R.color.brand_background)
    }

    private fun setupAdapters(): GenericFastAdapter {
        val totalBalanceAdapter: GenericFastItemAdapter = FastItemAdapter()

        // Wallet Balance
        WalletBalanceListItem(session = session, countly = countly).also {
            totalBalanceAdapter.set(listOf(it))
        }

        val accountsAdapter: GenericFastItemAdapter = FastItemAdapter()

        session.accountsFlow.onEach {
            AccountsListItem(
                session = session,
                accounts = session.accounts,
                showArrow = true,
                show2faActivation = true,
                showCopy = false,
                expandedAccount = session.activeAccountFlow,
                listener = object : AccordionListener {
                    override fun expandListener(view: View, position: Int) {
                        viewModel.accounts.getOrNull(position)?.also {
                            viewModel.setActiveAccount(it)
                        }
                    }

                    override fun arrowClickListener(view: View, position: Int) {
                        navigate(
                            WalletOverviewFragmentDirections.actionWalletOverviewFragmentToAccountOverviewFragment(
                                wallet = wallet,
                                account = viewModel.accounts[position]
                            )
                        )
                    }

                    override fun copyClickListener(view: View, position: Int) {}

                    override fun warningClickListener(view: View, position: Int) {
                        Call2ActionBottomSheetDialogFragment.showEnable2FA(
                            viewModel.accounts[position],
                            childFragmentManager
                        )
                    }

                    override fun longClickListener(view: View, position: Int) {
                        val menu =
                            if (viewModel.accounts.size == 1) R.menu.menu_account else R.menu.menu_account_archive
                        val account = viewModel.accounts[position]
                        showPopupMenu(view, menu) { menuItem ->
                            when (menuItem.itemId) {
                                R.id.rename -> {
                                    RenameAccountBottomSheetDialogFragment.show(
                                        account,
                                        childFragmentManager
                                    )
                                }
                                R.id.archive -> {
                                    viewModel.archiveAccount(account)
                                    snackbar(R.string.id_account_has_been_archived)
                                }
                            }
                            true
                        }
                    }

                }
            ).also {
                accountsAdapter.set(listOf(it))
            }
        }.launchIn(lifecycleScope)

        // Alert cards
        val alertCardsAdapter = ModelAdapter<AlertType, GenericItem> {
            if (it is AlertType.AppReview) {
                AppReviewListItem { rate ->
                    if (rate > 0) {
                        appReview.showGooglePlayInAppReviewDialog(this){
                            openBrowser(Urls.BLOCKSTREAM_GOOGLE_PLAY)
                        }
                    }
                    settingsManager.setAskedAboutAppReview()
                    viewModel.setAppReview(false)
                }
            } else {
                AlertListItem(it).also { alertListItem ->
                    alertListItem.action = { isClose ->
                        when (alertListItem.alertType) {
                            is AlertType.Reset2FA -> {
                                TwoFactorResetBottomSheetDialogFragment.show(
                                    alertListItem.alertType.network,
                                    alertListItem.alertType.twoFactorReset,
                                    childFragmentManager
                                )
                            }
                            is AlertType.Dispute2FA -> {
                                TwoFactorResetBottomSheetDialogFragment.show(
                                    alertListItem.alertType.network,
                                    alertListItem.alertType.twoFactorReset,
                                    childFragmentManager
                                )
                            }
                            is AlertType.SystemMessage -> {
                                if (isClose) {
                                    viewModel.dismissSystemMessage()
                                } else {
                                    SystemMessageBottomSheetDialogFragment.show(
                                        alertListItem.alertType.network,
                                        alertListItem.alertType.message,
                                        childFragmentManager
                                    )
                                }
                            }
                            is AlertType.Banner -> {
                                BannersHelper.dismiss(this, alertListItem.alertType.banner)
                            }
                            is AlertType.FailedNetworkLogin -> {
                                viewModel.tryFailedNetworks()
                            }
                            AlertType.EphemeralBip39, AlertType.TestnetWarning, AlertType.AppReview -> {}
                        }
                    }
                }
            }
        }.observeList(viewLifecycleOwner, viewModel.alertsLiveData)

        val transactionsTitleAdapter = FastItemAdapter<GenericItem>()

        combine(viewModel.walletTransactionsFlow, viewModel.accountsFlow){ transactions: List<Transaction>, accounts: List<Account> ->

            transactionsTitleAdapter.set((if (accounts.isEmpty()){
                listOf()
            } else {
                listOfNotNull(
                    TitleListItem(StringHolder(R.string.id_latest_transactions)),
                    if(transactions.isEmpty()) TextListItem(
                        text = StringHolder(R.string.id_your_transactions_will_be_shown),
                        textColor = R.color.color_on_surface_emphasis_low
                    ) else null
                )
            }))

        }.launchIn(lifecycleScope)

        val walletTransactionAdapter = ModelAdapter<Transaction, TransactionListItem> {
            // use getConfirmationsMax to avoid animations after a tx is confirmed
            TransactionListItem(it, session, showAccount = true)
        }.observeList(lifecycleScope, viewModel.walletTransactionsFlow)

        val adapters = listOf(
            totalBalanceAdapter,
            alertCardsAdapter,
            accountsAdapter,
            transactionsTitleAdapter,
            walletTransactionAdapter
        )

        val fastAdapter = FastAdapter.with(adapters).also {
            this.fastAdapter = it
        }

        merge(viewModel.walletTotalBalanceFlow, viewModel.walletAssetsFlow).onEach {
            fastAdapter.notifyItemChanged(0)
        }.launchIn(lifecycleScope)


        // Notify when 1) account & balance are update and when 2) assets are updated
        merge(session.accountsAndBalanceUpdatedFlow, session.networkAssetManager.assetsUpdateFlow).onEach {
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        // Update on amount visibility change
        settingsManager.getApplicationSettingsLiveData().observe(viewLifecycleOwner){
            fastAdapter.notifyAdapterDataSetChanged()
        }

        val navigateToAssets = {
            navigate(
                WalletOverviewFragmentDirections.actionWalletOverviewFragmentToAssetsFragment(
                    wallet = wallet
                )
            )
        }

        fastAdapter.addClickListener<ListItemWalletBalanceBinding, GenericItem>({ binding -> binding.eye }) { _, _, _, _ ->
            settingsManager.saveApplicationSettings(
                settingsManager.getApplicationSettings().let {
                    it.copy(
                        hideAmounts = !it.hideAmounts
                    )
                }
            )
        }

        fastAdapter.addClickListener<ListItemWalletBalanceBinding, GenericItem>({ binding -> binding.assetsIcons }) { _, _, _, _ ->
            navigateToAssets.invoke()
        }

        fastAdapter.addClickListener<ListItemWalletBalanceBinding, GenericItem>({ binding -> binding.assetsTextView }) { _, _, _, _ ->
            navigateToAssets.invoke()
        }

        fastAdapter.onClickListener = { _, _, item, _ ->
            when (item) {
                is AccountListItem -> {
                    navigate(
                        WalletOverviewFragmentDirections.actionWalletOverviewFragmentToAccountOverviewFragment(
                            wallet = wallet,
                            account = item.account
                        )
                    )
                }
                is TransactionListItem -> {
                    navigate(
                        WalletOverviewFragmentDirections.actionWalletOverviewFragmentToTransactionDetailsFragment(
                            wallet = wallet,
                            transaction = item.tx
                        )
                    )
                }
                is AlertListItem -> {
                    if (item.alertType is AlertType.Banner) {
                        BannersHelper.handleClick(this, item.alertType.banner)
                    }
                }
            }

            true
        }

        fastAdapter.onLongClickListener = { view: View, _, item: GenericItem, _ ->
                if (item is AccountListItem) {
                    val menu = if (viewModel.accounts.size == 1) R.menu.menu_account else R.menu.menu_account_archive

                    showPopupMenu(view, menu) { menuItem ->
                        when (menuItem.itemId) {
                            R.id.rename -> {
                                RenameAccountBottomSheetDialogFragment.show(
                                    item.account,
                                    childFragmentManager
                                )
                            }
                            R.id.archive -> {
                                viewModel.archiveAccount(item.account)
                                snackbar(R.string.id_account_has_been_archived)
                            }
                        }
                        true
                    }

                }

                true
            }


        return fastAdapter
    }

    override fun menuItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        when(position){
            0 -> {
                navigate(WalletOverviewFragmentDirections.actionGlobalSwapFragment(
                    wallet = viewModel.wallet,
                    proposal = null
                ))
            }
            1 -> {
                context?.let {
                    getClipboard(it)
                }?.let { link ->
                    openProposal(link)
                }
            }
            else -> {
                CameraBottomSheetDialogFragment.showSingle(decodeContinuous = false, childFragmentManager)
            }
        }
    }

    private fun openProposal(link: String) {
        if (!link.startsWith("https://")) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_warning)
                .setMessage(R.string.id_invalid_swap_proposal)
                .show()
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                viewModel.downloadProposal(link)?.let {
                    navigate(
                        WalletOverviewFragmentDirections.actionGlobalSwapFragment(
                            wallet = viewModel.wallet,
                            proposal = it
                        )
                    )
                }
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_warning)
                    .setMessage(R.string.id_invalid_swap_proposal)
                    .show()
            }
        }
    }
}
