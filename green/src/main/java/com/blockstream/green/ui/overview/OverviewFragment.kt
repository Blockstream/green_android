package com.blockstream.green.ui.overview

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.green.databinding.OverviewFragmentBinding
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.ui.TwoFactorResetSheetDialogFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.looks.AssetListLook
import com.blockstream.green.ui.wallet.*
import com.blockstream.green.utils.*
import com.blockstream.green.views.EndlessRecyclerOnScrollListener
import com.blockstream.green.views.NpaLinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.ui.preferences.PrefKeys
import com.greenaddress.greenbits.ui.send.ScanActivity
import com.greenaddress.greenbits.ui.transactions.TransactionActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.ui.items.ProgressItem
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.util.*
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

    private fun createBottomBarAnimation(isHide : Boolean): ObjectAnimator {
        return ObjectAnimator.ofFloat(
                binding.bottomBar,
                "translationY",
                if(isHide) binding.bottomBar.height.toFloat() else 0f
        ).apply {
            duration = 200
        }
    }

//    private val movingBehavior by lazy {
//        (binding.bottomBar.layoutParams as CoordinatorLayout.LayoutParams).behavior as ScrollAwareBehavior to binding.bottomBar
//    }

    var showAccountInToolbar: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue) {
            updateToolbar(newValue)
        }
    }

    private val startForResultTransactionDetails = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshTransactions()
        }
    }

    companion object {
        const val ADD_NEW_ACCOUNT = "add_new_account"
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isFinishingGuard) return

        getNavigationResult<Long>(ADD_NEW_ACCOUNT)?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.setSubAccount(it)
                clearNavigationResult(ADD_NEW_ACCOUNT)
            }
        }

        binding.vm = viewModel

        val fastAdapter = setupAdapters(binding.recycler)

        binding.buttonReceive.setOnClickListener {
            navigate(OverviewFragmentDirections.actionOverviewFragmentToReceiveFragment(viewModel.wallet))
        }

        binding.buttonSend.setOnClickListener {
            if (viewModel.isLiquid().value == true &&
                (viewModel.getBalancesLiveData().value?.get(session.network.policyAsset) ?: 0) == 0L
            ){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_warning)
                    .setMessage(R.string.id_insufficient_lbtc_to_send_a)
                    .setPositiveButton(R.string.id_receive) { _: DialogInterface, _: Int ->
                        navigate(OverviewFragmentDirections.actionOverviewFragmentToReceiveFragment(viewModel.wallet))
                        true
                    }
                    .setNegativeButton(R.string.id_cancel, null)
                    .show()

            }else{
                val intent = Intent(activity, ScanActivity::class.java)
                intent.putExtra(PrefKeys.SWEEP, session.isWatchOnly)
                startActivity(intent)
            }
        }

        binding.recycler.apply {
            layoutManager = NpaLinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.refresh()
        }

        viewModel.getWalletLiveData().observe(viewLifecycleOwner) {
            setToolbar(it.name, drawable = ContextCompat.getDrawable(requireContext(), it.getIcon()))
        }

        viewModel.getState().distinctUntilChanged().observe(viewLifecycleOwner) {
            // TODO fix it, its not working
            updateToolbar(false)

            onBackCallback.isEnabled = !isOverviewState
        }

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                showAccountInToolbar = (recyclerView.layoutManager as NpaLinearLayoutManager).findFirstVisibleItemPosition() > 0
            }
        })

        updateRecyclerPadding(topPadding = true, bottomPadding = true)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.delete -> {
//                DeleteWalletBottomSheetDialogFragment().also {
//                    it.show(childFragmentManager, it.toString())
//                }
//            }
//
//            R.id.rename -> {
//                RenameWalletBottomSheetDialogFragment().also {
//                    it.show(childFragmentManager, it.toString())
//                }
//            }
//            R.id.logout -> {
//                viewModel.logout()
//                return true
//            }
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

    private fun updateRecyclerPadding(topPadding: Boolean, bottomPadding: Boolean) {
        binding.recycler.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Ensure you call it only once
                binding.recycler.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val top = if (topPadding) resources.getDimension(R.dimen.dp16).toInt() else 0
                val bottom = if (bottomPadding) binding.bottomBar.height else 0

                binding.recycler.updatePadding(
                    top = top,
                    bottom = bottom
                )

                binding.recycler.scrollToPosition(0)
            }
        })
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
        var topAccountAdapter = FastItemAdapter<AccountListItem>()
        var topAccount : AccountListItem? = null

        viewModel.getSubAccountLiveData().observe(viewLifecycleOwner){ subAccount ->
            // Move to top
            recycler.scrollToPosition(0)

            // Use that if you don't want animations
//            topAccount?.let {
//                it.subAccount = subAccount
//            } ?: run {
//                topAccount = AccountListItem(subAccount, session.network, true, false)
//                topAccountAdapter.set(listOf(topAccount!!))
//            }

            // and this if you want
            topAccount = AccountListItem(subAccount, session.network, true).also {
                updateTopAccountCard(it)
            }
            topAccountAdapter.set(listOf(topAccount!!))

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
            AccountListItem(model, session.network)
        }.observeList(viewLifecycleOwner, viewModel.getFilteredSubAccounts(),{
            topAccount?.let { updateTopAccountCard(it) }
        }).also {
            it.active = false
        }

        // Add new account button
        val addAccountAdapter = FastItemAdapter<GenericItem>().also {
                it.itemAdapter.active = false
            }

        if(!wallet.isWatchOnly){
            addAccountAdapter.add(ButtonActionListItem(StringHolder(R.string.id_add_new_account), true))
        }

        // Alert cards
        val alertCardsAdapter =  ModelAdapter<AlertType, AlertListItem> {
            AlertListItem(it) { _ ->
                when(it){
                    is AlertType.Abstract2FA -> {
                        TwoFactorResetSheetDialogFragment.newInstance(it.twoFactorReset).also { dialog ->
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

        // Assets Balance
        val assetsBalanceAdapter =  ModelAdapter<BalancePair, AssetListItem>() {
            AssetListItem(session, it, isAssetState && viewModel.wallet.isLiquid, (it.first.isEmpty() && it.second == -1L))
        }.observeMap(viewLifecycleOwner, viewModel.getBalancesLiveData() as LiveData<Map<*, *>>) {
            BalancePair(it.key as String, it.value as Long)
        }

        var titleAdapter = FastItemAdapter<GenericItem>()

        viewModel.getTransactions().distinctUntilChanged().observe(viewLifecycleOwner){
            if(it.isNotEmpty()){
                titleAdapter.set(listOf(TitleListItem(StringHolder(R.string.id_transactions))))
            }else{
                titleAdapter.set(listOf(TextListItem(StringHolder(R.string.id_your_transactions_will_be_shown))))
            }
        }

        val transactionsFooterAdapter = ItemAdapter<ProgressItem>()

        val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore() {
                transactionsFooterAdapter.set(listOf(ProgressItem()))
                disable()
                viewModel.loadMoreTransactions()
            }
        }.also {
            it.disable()
        }

        val transactionAdapter = ModelAdapter<Transaction, TransactionListItem>() {
            TransactionListItem(session, it)
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
            assetsTitleAdapter,
            assetsBalanceAdapter,
            titleAdapter,
            transactionAdapter,
            transactionsFooterAdapter
        )

        val fastAdapter = FastAdapter.with(adapters)

        // Notify adapter when we have new assets
        viewModel.assetsUpdated.observe(viewLifecycleOwner) {
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
                    if(viewModel.wallet.isLiquid) {
                        if (AssetFilteringIsEnabled && isOverviewState && viewModel.wallet.isLiquid) {
                            viewModel.setAsset(item.balancePair)
                        } else {
                            if(item.balancePair.first != session.network.policyAsset){
                                navigate(
                                        OverviewFragmentDirections.actionOverviewFragmentToAssetBottomSheetFragment(
                                                item.balancePair.first,
                                                wallet
                                        )
                                )
                            }
                        }
                    }
                }
                is TransactionListItem -> {
                    // navigate(OverviewFragmentDirections.actionOverviewFragmentToTransactionDetailsFragment())

                    val txIntent = Intent(activity, TransactionActivity::class.java)
                    viewModel.getBalancesLiveData().value?.let {
                        txIntent.putExtra("TRANSACTION", item.tx.getTransactionDataV3())
                        txIntent.putExtra("BALANCE", HashMap(it))
                        startForResultTransactionDetails.launch(txIntent)
                    }
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
                updateTopAccountCard(it)
            }

            accountsModelAdapter.active = isAccount
            addAccountAdapter.itemAdapter.active = isAccount

            alertCardsAdapter.active = isOverviewOrAssets
            systemMessageAdapter.itemAdapter.active = isOverviewOrAssets
            managedAssetsAccountIdAdapter.itemAdapter.active = isOverviewState
            assetsTitleAdapter.itemAdapter.active = isOverviewOrAssets && wallet.isLiquid
            assetsBalanceAdapter.active = isOverviewOrAssets
            titleAdapter.itemAdapter.active = isOverviewOrAssets
            transactionAdapter.active = isOverviewOrAssets
            transactionsFooterAdapter.active = isOverviewOrAssets
        }

        return fastAdapter
    }

    private fun updateTopAccountCard(topCard: AccountListItem) {
        // getSubAccounts returns the accounts except the selected one
        topCard.showFakeCard = viewModel.getFilteredSubAccounts().value?.size ?: 0 > 0
        topCard.isAccountListOpen = isAccountState
    }

    private fun updateToolbar(showSecondary: Boolean) {
        var title = viewModel.wallet.name
        var subtitle: String? = null

        var icon = ContextCompat.getDrawable(
            requireContext(),
            viewModel.wallet.getIcon()
        )

        if (showSecondary) {
            if (isOverviewState) {
                viewModel.getSubAccountLiveData().value?.nameOrDefault(getString(R.string.id_main_account))
                    ?.let {
                        subtitle = title // wallet name
                        title = it
                    }
            } else {
                session.network.policyAsset.let{ policyAsset ->
                    viewModel.getBalancesLiveData().value?.get(policyAsset)?.let {
                        val asset = session.getAsset(policyAsset)
                        val look = AssetListLook(policyAsset, it, asset, session)

                        subtitle = title // wallet name
                        title = look.name
                        icon = look.icon(requireContext())
                    }
                }
            }
        }

        setToolbar(title, subtitle = subtitle, drawable = icon)
    }
}