package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.common.Urls
import com.blockstream.common.data.Denomination
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.lightning.isLoading
import com.blockstream.common.events.Events
import com.blockstream.common.navigation.LogoutReason
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemLightningInfoBinding
import com.blockstream.green.databinding.ListItemWalletBalanceBinding
import com.blockstream.green.databinding.WalletOverviewFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.setOnClickListener
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
import com.blockstream.green.ui.dialogs.AppRateDialogFragment
import com.blockstream.green.ui.dialogs.ArchiveAccountDialogFragment
import com.blockstream.green.ui.items.AccountListItem
import com.blockstream.green.ui.items.AccountsListItem
import com.blockstream.green.ui.items.AlertListItem
import com.blockstream.green.ui.items.AlertType
import com.blockstream.green.ui.items.LightningInfoListItem
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.items.TitleListItem
import com.blockstream.green.ui.items.TransactionListItem
import com.blockstream.green.ui.items.WalletBalanceListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.AppReviewHelper
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates


class WalletOverviewFragment : AbstractWalletFragment<WalletOverviewFragmentBinding>(
    layout = R.layout.wallet_overview_fragment,
    menuRes = R.menu.wallet_overview
), OverviewInterface, MenuDataProvider{

    val args: WalletOverviewFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val appFragment: WalletOverviewFragment
        get() = this

    private val applicationScope: ApplicationScope by inject()

    val viewModel: WalletOverviewViewModel by viewModel {
        parametersOf(args.wallet)
    }

    var fastAdapter: FastAdapter<GenericItem>? = null

    // Prevent ViewModel initialization if session is not initialized
    override val title: String
        get() = if (isSessionNetworkInitialized) viewModel.wallet.name else ""

    override val subtitle: String?
        get() = if(session.isLightningShortcut) getString(R.string.id_lightning_account) else null

    var showAccountInToolbar: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateToolbar()
        }
    }

    override val screenName = "WalletOverview"

    companion object {
        const val BROADCASTED_TRANSACTION = "BROADCASTED_TRANSACTION"
        const val ACCOUNT_ARCHIVED = "ACCOUNT_ARCHIVED"
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        overviewSetup()

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
                    AppRateDialogFragment.show(childFragmentManager)
                }
                clearNavigationResult(BROADCASTED_TRANSACTION)
            }
        }

        getNavigationResult<Boolean>(ACCOUNT_ARCHIVED)?.observe(viewLifecycleOwner) {
            it?.let {
                clearNavigationResult(ACCOUNT_ARCHIVED)
                ArchiveAccountDialogFragment.show(fragmentManager = childFragmentManager)
            }
        }

        // Handle pending URI (BIP-21 or lightning)
        sessionManager.pendingUri.consumeAsFlow().filterNotNull().onEach {
            handleUserInput(it, false)
        }.launchIn(lifecycleScope)

        binding.vm = viewModel

        binding.bottomNav.isWatchOnly = wallet.isWatchOnly
        binding.bottomNav.sweepEnabled = session.defaultNetwork.isBitcoin && session.defaultNetwork.isMultisig

        viewModel.zeroAccounts.onEach {
            (if(it) 0.2f else 1.0f).also { alpha ->
                binding.recycler.alpha = alpha
                binding.bottomNav.root.alpha = alpha
            }
            setToolbarVisibility(!it)

            if(it){
                binding.rive.play()
            }else{
                binding.rive.stop()
            }
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
                    wallet = viewModel.wallet, accountAsset = AccountAsset.fromAccount(session.activeAccount.value!!)
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

        binding.bottomNav.buttonCamera.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, fragmentManager = childFragmentManager)
        }

        binding.bottomNav.buttonSend.setOnClickListener {
            when {
                session.isWatchOnly -> {
                    navigate(
                        WalletOverviewFragmentDirections.actionWalletOverviewFragmentToSendFragment(
                            wallet = wallet,
                            isSweep = true,
                            accountAsset = AccountAsset.fromAccount(session.activeAccount.value!!)
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
                            wallet = wallet, accountAsset = AccountAsset.fromAccount(session.activeAccount.value!!)
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

        if (ConsentBottomSheetDialogFragment.shouldShowConsentDialog(settingsManager)) {
            applicationScope.launch(context = logException(countly)) {
                delay(1500)
                ConsentBottomSheetDialogFragment.show(childFragmentManager)
            }
        }

        (requireActivity() as MainActivity).askForNotificationPermissionIfNeeded()
    }

    override fun onPrepareMenu(menu: Menu) {
        // Prevent from archiving all your acocunts
        menu.findItem(R.id.create_account).isVisible = !session.isWatchOnly && !wallet.isLightning
        menu.findItem(R.id.settings).isVisible = !wallet.isLightning
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
                viewModel.logout(LogoutReason.USER_ACTION)
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
        val walletBalanceListItem = WalletBalanceListItem(session = session, countly = countly).also {
            it.denomination = viewModel.balanceDenomination.value
            if(!wallet.isLightning) {
                totalBalanceAdapter.set(listOf(it))
            }
        }

        val accountsAdapter: GenericFastItemAdapter = FastItemAdapter()

        session.accounts.onEach {
            AccountsListItem(
                session = session,
                accounts = session.accounts.value,
                showArrow = !session.isLightningShortcut,
                show2faActivation = true,
                showCopy = false,
                expandedAccount = session.activeAccount,
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
                        if(session.isLightningShortcut) return
                        val account = viewModel.accounts[position]

                        val menu = if(account.isLightning) R.menu.menu_account_remove else if (viewModel.accounts.size == 1) R.menu.menu_account else R.menu.menu_account_archive
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
                                    ArchiveAccountDialogFragment.show(fragmentManager = childFragmentManager)
                                }

                                R.id.remove -> {
                                    viewModel.removeAccount(account)
                                    snackbar(R.string.id_account_has_been_removed)
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
                            viewModel.postEvent(Events.BannerDismiss)
                        }
                        is AlertType.FailedNetworkLogin -> {
                            viewModel.tryFailedNetworks()
                        }
                        AlertType.EphemeralBip39, AlertType.TestnetWarning -> {}
                    }
                }
            }
        }.observeList(viewLifecycleOwner, viewModel.alertsLiveData)

        val lightningInboundAdapter: GenericFastItemAdapter = FastItemAdapter()

        if(session.isLightningShortcut){
            session.lightningNodeInfoStateFlow.filter { !it.isLoading() }.onEach {
                lightningInboundAdapter.set(
                    listOf(
                        LightningInfoListItem(session = session, nodeState = it)
                    )
                )
            }.launchIn(lifecycleScope)
        }

        val transactionsTitleAdapter = FastItemAdapter<GenericItem>()

        combine(viewModel.walletTransactionsFlow, viewModel.accountsFlow) { transactions: List<Transaction>, accounts: List<Account> ->

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
            lightningInboundAdapter,
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
        merge(session.accountsAndBalanceUpdated, session.networkAssetManager.assetsUpdateFlow).onEach {
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        // Update on amount visibility change
        settingsManager.appSettingsStateFlow.onEach {
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        val navigateToAssets = {
            navigate(
                WalletOverviewFragmentDirections.actionWalletOverviewFragmentToAssetsFragment(
                    wallet = wallet
                )
            )
        }

        viewModel.balanceDenomination.onEach {
            walletBalanceListItem.denomination = it
            fastAdapter.getPosition(walletBalanceListItem).takeIf { it >= 0 }?.also {
                fastAdapter.notifyAdapterItemChanged(it)
            }

        }.launchIn(lifecycleScope)

        fastAdapter.addClickListener<ListItemWalletBalanceBinding, GenericItem>({ binding -> binding.balanceTextView }) { _, _, _, _ ->
            viewModel.changeDenomination()
        }

        fastAdapter.addClickListener<ListItemWalletBalanceBinding, GenericItem>({ binding -> binding.fiatTextView }) { _, _, _, _ ->
            viewModel.changeDenomination()
        }

        fastAdapter.addClickListener<ListItemLightningInfoBinding, GenericItem>({ binding -> binding.buttonLearnMore }) { _, _, _, _ ->
            openBrowser(Urls.HELP_RECEIVE_CAPACITY)
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

        fastAdapter.addClickListener<ListItemWalletBalanceBinding, GenericItem>({ binding -> binding.buttonDenomination }) { _, _, _, _ ->
            showDenominationAndExchangeRateDialog {
                viewModel.balanceDenomination.value = Denomination.default(session)
                walletBalanceListItem.reset(viewModel.balanceDenomination.value)
                fastAdapter.notifyAdapterDataSetChanged()
            }
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
                    if(item.tx.isLightningSwap){
                        if(item.tx.isRefundableSwap){
                            navigate(
                                WalletOverviewFragmentDirections.actionWalletOverviewFragmentToRecoverFundsFragment(
                                    wallet = wallet,
                                    address = item.tx.inputs.first().address ?: "",
                                    amount = item.tx.satoshiPolicyAsset
                                )
                            )
                        }else{
                            snackbar(R.string.id_swap_is_in_progress)
                        }
                    }else if(!item.tx.isLoadingTransaction) {
                        navigate(
                            WalletOverviewFragmentDirections.actionWalletOverviewFragmentToTransactionDetailsFragment(
                                wallet = wallet,
                                transaction = item.tx
                            )
                        )
                    }
                }
                is AlertListItem -> {
                    if (item.alertType is AlertType.Banner) {
                        viewModel.postEvent(Events.BannerAction)
                    }
                }
            }

            true
        }

        fastAdapter.onLongClickListener = { view: View, _, item: GenericItem, _ ->
                if (item is AccountListItem) {
                    if(!item.account.isLightning) {
                        val menu =
                            if (viewModel.accounts.size == 1) R.menu.menu_account else R.menu.menu_account_archive

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
                                    ArchiveAccountDialogFragment.show(fragmentManager = childFragmentManager)
                                }
                            }
                            true
                        }
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
                getClipboard(requireContext())?.also {
                    openProposal(it)
                }
            }
            else -> {
                CameraBottomSheetDialogFragment.showSingle(screenName = screenName, decodeContinuous = false, childFragmentManager)
            }
        }
    }

    override fun openProposal(link: String) {
        if (!link.startsWith("https://")) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_warning)
                .setMessage(R.string.id_invalid_swap_proposal)
                .setPositiveButton(R.string.id_ok) { _, _ ->

                }
                .show()
            return
        }
        lifecycleScope.launch(context = Dispatchers.Main + logException()) {
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
