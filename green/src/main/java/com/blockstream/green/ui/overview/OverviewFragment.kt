package com.blockstream.green.ui.overview

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.green.databinding.OverviewFragmentBinding
import com.blockstream.green.gdk.getConfirmationsMax
import com.blockstream.green.ui.TwoFactorResetBottomSheetDialogFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.looks.AssetLook
import com.blockstream.green.ui.wallet.AccountIdBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.RenameAccountBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.SystemMessageBottomSheetDialogFragment
import com.blockstream.green.utils.*
import com.blockstream.green.views.EndlessRecyclerOnScrollListener
import com.blockstream.green.views.NpaLinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.properties.Delegates


@AndroidEntryPoint
class OverviewFragment : WalletFragment<OverviewFragmentBinding>(
    layout = R.layout.overview_fragment,
    menuRes = R.menu.overview
) {

    val args: OverviewFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    @Inject
    lateinit var viewModelFactory: OverviewViewModel.AssistedFactory
    val viewModel: OverviewViewModel by viewModels {
        OverviewViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    val AssetFilteringIsEnabled = false

    private val isOverviewState: Boolean
        get() = viewModel.getState().value == OverviewViewModel.State.Overview

    private val isAccountState: Boolean
        get() = viewModel.getState().value == OverviewViewModel.State.Account

    private val isAssetState: Boolean
        get() = viewModel.getState().value == OverviewViewModel.State.Asset

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = closeOpenElements()
        }
    }

    override val title: String
        get() = viewModel.wallet.name

    private fun createBottomBarAnimation(isHide : Boolean): ObjectAnimator {
        return ObjectAnimator.ofFloat(
                binding.bottomBar,
                "translationY",
                if(isHide) binding.bottomBar.height.toFloat() else 0f
        ).apply {
            duration = 200
        }
    }

    var showAccountInToolbar: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue) {
            updateToolbar()
        }
    }

    companion object {
        const val ADD_NEW_ACCOUNT = "add_new_account"
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        getNavigationResult<Long>(ADD_NEW_ACCOUNT)?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.setSubAccount(it)
                clearNavigationResult(ADD_NEW_ACCOUNT)
            }
        }

        // Handle pending BIP-21 uri
        sessionManager.pendingBip21Uri.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { bip21Uri ->
                navigate(
                    OverviewFragmentDirections.actionOverviewFragmentToSendFragment(
                        wallet, address = bip21Uri
                    )
                )
                snackbar(R.string.id_address_was_filled_by_a_payment)
            }
        }

        binding.vm = viewModel

        val fastAdapter = setupAdapters(binding.recycler)

        binding.buttonReceive.setOnClickListener {
            navigate(OverviewFragmentDirections.actionOverviewFragmentToReceiveFragment(viewModel.wallet))
        }

        binding.buttonSend.setOnClickListener {
            when {
                session.isWatchOnly -> {
                    navigate(
                        OverviewFragmentDirections.actionOverviewFragmentToSendFragment(
                            wallet = wallet,
                            isSweep = true
                        )
                    )
                }
                viewModel.getBalancesLiveData().value?.get(session.network.policyAsset) ?: 0 == 0L -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.id_warning)
                        .setMessage(if (session.isLiquid) R.string.id_insufficient_lbtc_to_send_a else R.string.id_you_have_no_coins_to_send)
                        .also {
                            if (session.isLiquid) {
                                it.setPositiveButton(R.string.id_learn_more) { _: DialogInterface, _: Int ->
                                    openBrowser(Urls.HELP_GET_LIQUID)
                                }
                            } else {
                                it.setPositiveButton(R.string.id_receive) { _: DialogInterface, _: Int ->
                                    navigate(
                                        OverviewFragmentDirections.actionOverviewFragmentToReceiveFragment(
                                            viewModel.wallet
                                        )
                                    )
                                }
                            }
                        }
                        .setNegativeButton(R.string.id_cancel, null)
                        .show()

                }
                else -> {
                    navigate(
                        OverviewFragmentDirections.actionOverviewFragmentToSendFragment(
                            wallet
                        )
                    )
                }
            }
        }

        fastAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.recycler.apply {
            layoutManager = NpaLinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.refresh()
        }

        viewModel.getState().distinctUntilChanged().observe(viewLifecycleOwner) {
            // TODO fix it, its not working
            // updateToolbar(false)

            onBackCallback.isEnabled = !isOverviewState
        }

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                showAccountInToolbar = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 0
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                navigate(
                    OverviewFragmentDirections.actionOverviewFragmentToWalletSettingsFragment(wallet)
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

    private fun closeInnerAdapter() {
        viewModel.setState(OverviewViewModel.State.Overview)
        onBackCallback.isEnabled = isDrawerOpen()
    }

    fun closeOpenElements(): Boolean {
        if (isDrawerOpen()) {
            closeDrawer()
        } else if (!isOverviewState) {
            closeInnerAdapter()
        }

        return !isOverviewState
    }

    private fun setupAdapters(recycler: RecyclerView): GenericFastAdapter {
        val managedAssetsAccountIdAdapter: GenericFastItemAdapter = FastItemAdapter()
        val assetsTitleAdapter: GenericFastItemAdapter = FastItemAdapter()

        // Top Account Card
        var topAccountAdapter = FastItemAdapter<GenericItem>()
        var topAccount : AccountListItem? = null

        viewModel.getSubAccountLiveData().observe(viewLifecycleOwner){ subAccount ->
            if(!viewModel.initialScrollToTop){
                // If there is a delay getting the top card recycler can be moved to the middle
                // of the scroll viewport
                recycler.scrollToPosition(0)
                viewModel.initialScrollToTop = true
            }

            // Top Account
            topAccount = AccountListItem(
                session = session,
                subAccount = subAccount,
                walletBalances = session.walletBalances,
                isTopAccount = true
            ).also {
                updateTopAccountCard(it)
            }.also {
                topAccountAdapter.set(listOf(it))
            }

            // Account Id
            if(viewModel.wallet.isLiquid){
                managedAssetsAccountIdAdapter.clear()

                if(subAccount.type == AccountType.AMP_ACCOUNT) {
                    managedAssetsAccountIdAdapter.set(
                        listOf(
                            AccountIdListItem { _ ->
                                AccountIdBottomSheetDialogFragment(subAccount).also {
                                    it.show(childFragmentManager, it.toString())
                                }
                            }
                        )
                    )
                }
            }
        }

        // Account Cards
        val accountsModelAdapter = ModelAdapter { model: SubAccount ->
            AccountListItem(session, model, session.walletBalances, isTopAccount = false, isAccountListOpen = true)
        }.observeList(viewLifecycleOwner, viewModel.getFilteredSubAccounts()) {
            topAccount?.let { updateTopAccountCard(it, topAccountAdapter) }
        }.also {
            it.active = false
        }

        // Add new account button
        val addAccountAdapter = FastItemAdapter<GenericItem>().also {
            it.itemAdapter.active = false
        }

        addAccountAdapter.add(ProgressListItem())

        if(!wallet.isWatchOnly){
            addAccountAdapter.add(ButtonActionListItem(text = StringHolder(R.string.id_add_new_account), useCard = true, extraPadding = true))
        }

        val removeAccountsLoader  = object : Observer<List<SubAccount>> {
            override fun onChanged(p0: List<SubAccount>?) {
                addAccountAdapter.itemAdapter.remove(0) // remove ProgressListItem
                viewModel.getFilteredSubAccounts().removeObserver(this)
            }
        }

        viewModel.getFilteredSubAccounts().observe(viewLifecycleOwner, removeAccountsLoader)

        // Alert cards
        val alertCardsAdapter =  ModelAdapter<AlertType, AlertListItem> {
            AlertListItem(it) { _ ->
                when(it){
                    is AlertType.Abstract2FA -> {
                        TwoFactorResetBottomSheetDialogFragment.newInstance(it.twoFactorReset).also { dialog ->
                            dialog.show(childFragmentManager, dialog.toString())
                        }
                    }
                }
            }
        }.observeList(viewLifecycleOwner, viewModel.getAlerts())

        // System message card
        val systemMessageAdapter = FastItemAdapter<AlertListItem>()

        viewModel.getSystemMessage().observe(viewLifecycleOwner){ message ->
            if(message.isNullOrBlank()){
                systemMessageAdapter.clear()
            }else {
                systemMessageAdapter.set(listOf(AlertListItem(AlertType.SystemMessage(message)) {
                    if (it) {
                        systemMessageAdapter.clear()
                    } else {
                        SystemMessageBottomSheetDialogFragment.newInstance(message).also { dialog ->
                            dialog.show(childFragmentManager, dialog.toString())
                        }
                    }
                }))
            }
        }

        // Block headers
        val blockHeaderAdapter = FastItemAdapter<GenericItem>()
        if(isDevelopmentFlavor()) {
            viewModel.getBlock().observe(viewLifecycleOwner) {
                blockHeaderAdapter.set(
                    listOf(
                        BlockHeaderListItem(
                            StringHolder("Block height: ${it.height}"),
                            session.network
                        )
                    )
                )
            }
        }

        // Assets Balance
        val assetsBalanceAdapter = ModelAdapter<BalancePair, AssetListItem>() {
            AssetListItem(
                session = session,
                balancePair = it,
                showInfo = isAssetState && viewModel.wallet.isLiquid,
                isLoading = (it.first.isEmpty() && it.second == -1L)
            )
        }.observeMap(viewLifecycleOwner, viewModel.getBalancesLiveData() as LiveData<Map<*, *>>, toModel = {
            BalancePair(it.key as String, it.value as Long)
        })

        var titleAdapter = FastItemAdapter<GenericItem>()

        viewModel.getTransactions().distinctUntilChanged().observe(viewLifecycleOwner){
            if(it.isNotEmpty()){
                titleAdapter.set(listOf(TitleListItem(StringHolder(R.string.id_transactions))))
            }else{
                titleAdapter.set(listOf(TextListItem(StringHolder(R.string.id_your_transactions_will_be_shown))))
            }
        }

        val transactionsFooterAdapter = ItemAdapter<GenericItem>()

        val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener(recycler) {
            override fun onLoadMore() {
                transactionsFooterAdapter.set(listOf(ProgressListItem()))
                disable()
                viewModel.loadMoreTransactions()
            }
        }.also {
            it.disable()
        }

        val transactionAdapter = ModelAdapter<Transaction, TransactionListItem> {
            // use getConfirmationsMax to avoid animations after a tx is confirmed
            TransactionListItem(session, it, it.getConfirmationsMax(session))
        }.observeList(viewLifecycleOwner, viewModel.getTransactions()) {
            transactionsFooterAdapter.clear()

            if(session.hasMoreTransactions){
                // enable after a short period of time to avoid being immediately called
                lifecycleScope.launchWhenResumed {
                    delay(200L)
                    endlessRecyclerOnScrollListener.enable()
                }
            }else{
                endlessRecyclerOnScrollListener.disable()
            }
        }

        recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        val adapters = listOf(
            topAccountAdapter,
            accountsModelAdapter,
            addAccountAdapter,
            alertCardsAdapter,
            systemMessageAdapter,
            managedAssetsAccountIdAdapter,
            blockHeaderAdapter,
            assetsTitleAdapter,
            assetsBalanceAdapter,
            titleAdapter,
            transactionAdapter,
            transactionsFooterAdapter
        )

        val fastAdapter = FastAdapter.with(adapters)

        // Notify adapter when we have new assets
        viewModel.getAssetsUpdated().observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                fastAdapter.notifyAdapterDataSetChanged()
            }
        }

        fastAdapter.addClickListener<ListItemAccountBinding, GenericItem>({ binding -> binding.buttonAccountMenu }) { view, _, _, _ ->
            showPopupMenu(view, R.menu.menu_account) { item ->
                when (item.itemId) {
                    R.id.rename -> {
                        RenameAccountBottomSheetDialogFragment().also {
                            it.show(childFragmentManager, it.toString())
                        }
                    }
                }
                true
            }
        }

        fastAdapter.onClickListener = { _, _, item, _ ->
            when (item) {
                is AccountListItem -> {
                    if(item.isTopAccount){
                        if(isOverviewState){
                            viewModel.setState(OverviewViewModel.State.Account)
                        }else{
                            closeInnerAdapter()
                        }
                    }else{
                        viewModel.selectSubAccount(item.subAccount)
                        closeInnerAdapter()
                    }
                }

                is ButtonActionListItem -> {
                    navigate(
                        OverviewFragmentDirections.actionOverviewFragmentToChooseAccountTypeFragment(
                            wallet = args.wallet
                        )
                    )
                    closeInnerAdapter()
                }
                is AssetListItem -> {
                    if (AssetFilteringIsEnabled && isOverviewState && viewModel.wallet.isLiquid) {
                        viewModel.setAsset(item.balancePair)
                    } else {
                        AssetBottomSheetFragment.newInstance(item.balancePair.first).also {
                            it.show(childFragmentManager, it.toString())
                        }
                    }
                }
                is TransactionListItem -> {
                     navigate(OverviewFragmentDirections.actionOverviewFragmentToTransactionDetailsFragment(wallet = wallet, transaction = item.tx))
                }
                is TitleListItem -> {
                    if (item.showBackButton) {
                        closeOpenElements()
                    }
                }
            }

            true
        }

        if(!AssetFilteringIsEnabled){
            assetsTitleAdapter.set(
                    listOf(
                            TitleListItem(
                                    StringHolder(R.string.id_assets),
                                    isAssetState,
                                    withTopPadding = false
                            )
                    )
            )
        }


        viewModel.getState().distinctUntilChanged().observe(viewLifecycleOwner) {
            if(AssetFilteringIsEnabled) {
                if (wallet.isLiquid) {
                    assetsTitleAdapter.set(
                            listOf(
                                    TitleListItem(
                                            StringHolder(if (isOverviewState) R.string.id_assets else R.string.id_all_assets),
                                            isAssetState,
                                            withTopPadding = false
                                    )
                            )
                    )
                }
            }

            val isOverviewOrAssets = isOverviewState || isAssetState
            val isAccount = isAccountState

            if (isAccount) {
//                movingBehavior.first.disable(movingBehavior.second, false)
                createBottomBarAnimation(true).start()
            } else {
//                movingBehavior.first.enable()
                createBottomBarAnimation(false).start()
            }

            topAccount?.let {
                updateTopAccountCard(it, topAccountAdapter)
            }
            accountsModelAdapter.active = isAccount
            addAccountAdapter.itemAdapter.active = isAccount

            alertCardsAdapter.active = isOverviewOrAssets
            systemMessageAdapter.itemAdapter.active = isOverviewOrAssets
            managedAssetsAccountIdAdapter.itemAdapter.active = isOverviewState
            assetsTitleAdapter.itemAdapter.active = isOverviewOrAssets && wallet.isLiquid
            assetsBalanceAdapter.active = isOverviewOrAssets
            blockHeaderAdapter.itemAdapter.active = isOverviewOrAssets
            titleAdapter.itemAdapter.active = isOverviewOrAssets
            transactionAdapter.active = isOverviewOrAssets
            transactionsFooterAdapter.active = isOverviewOrAssets
        }

        return fastAdapter
    }

    private fun updateTopAccountCard(
        topCard: AccountListItem,
        adapter: FastItemAdapter<GenericItem>? = null
    ) {
        // getSubAccounts returns the accounts except the selected one
        topCard.showFakeCard = viewModel.getFilteredSubAccounts().value?.size ?: 0 > 0
        topCard.isAccountListOpen = isAccountState
        adapter?.notifyItemChanged(0)
    }

    override fun updateToolbar() {
        super.updateToolbar()

        var title = viewModel.wallet.name
        var subtitle: String? = null

        if (showAccountInToolbar) {
            if (isOverviewState) {
                viewModel.getSubAccountLiveData().value?.nameOrDefault(getString(R.string.id_main_account))
                    ?.let {
                        subtitle = title // wallet name
                        title = it
                    }
            } else {
                session.network.policyAsset.let{ policyAsset ->
                    viewModel.getBalancesLiveData().value?.get(policyAsset)?.let {
                        val look = AssetLook(policyAsset, it, session)

                        subtitle = title // wallet name
                        title = look.name
                    }
                }
            }
        }

        toolbar.title = title
        toolbar.subtitle = subtitle
    }
}