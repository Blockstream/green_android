package com.blockstream.green.ui.overview

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.BalancePair
import com.blockstream.green.*
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.databinding.OverviewFragmentBinding
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.ui.*
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.looks.AssetListLook
import com.blockstream.green.ui.wallet.*
import com.blockstream.green.utils.*
import com.blockstream.green.views.behaviors.ScrollAwareBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greenaddress.greenbits.ui.TabbedMainActivity
import com.greenaddress.greenbits.ui.preferences.PrefKeys
import com.greenaddress.greenbits.ui.send.ScanActivity
import com.greenaddress.greenbits.ui.transactions.TransactionActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import java.util.HashMap
import javax.inject.Inject


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

    private lateinit var overviewAdapter: GenericFastAdapter
    private lateinit var accountsAdapter: GenericFastAdapter
    private var assetsTitleAdapter: GenericFastItemAdapter = FastItemAdapter()
    private var managedAssetsAccountIdAdapter: GenericFastItemAdapter = FastItemAdapter()

    private val isOverviewState: Boolean
        get() = viewModel.getState().value == OverviewViewModel.State.Overview

    private val isAssetState: Boolean
        get() = viewModel.getState().value == OverviewViewModel.State.Asset

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = closeOpenElements()
        }
    }

    private val movableAnimation by lazy {
        ObjectAnimator.ofFloat(
            binding.bottomBar,
            "translationY",
            binding.bottomBar.height.toFloat()
        ).apply {
            duration = 150
        }
    }

    private val movingBehavior by lazy {
        (binding.bottomBar.layoutParams as CoordinatorLayout.LayoutParams).behavior as ScrollAwareBehavior to binding.bottomBar
    }

    companion object {
        const val ADD_NEW_ACCOUNT = "add_new_account"
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<Long>(ADD_NEW_ACCOUNT)?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.setSubAccount(it)
                clearNavigationResult(ADD_NEW_ACCOUNT)
            }
        }

        binding.vm = viewModel

        overviewAdapter = createOverviewAdapter()
        initAccountsAdapter()

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
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = overviewAdapter
        }

        binding.account.setOnClickListener {
            if (viewModel.getState().value == OverviewViewModel.State.Account) {
                viewModel.setState(OverviewViewModel.State.Overview)
            } else {
                viewModel.setState(OverviewViewModel.State.Account)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.refresh()
        }

        binding.buttonAccountMenu.setOnClickListener { view ->
            showPopupMenu(view, R.menu.menu_account) { item ->
                when(item.itemId){
                    R.id.rename -> {
                        RenameAccountBottomSheetDialogFragment().also {
                            it.show(childFragmentManager, it.toString())
                        }
                    }
                }
                true
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { event ->
                if (event == AbstractWalletViewModel.Event.DELETE_WALLET) {
                    findNavController().popBackStack()
                }
            }
        }

        viewModel.getWalletLiveData().observe(viewLifecycleOwner) {
            setToolbar(it.name, drawable = ContextCompat.getDrawable(requireContext(), it.getIcon()))
        }


        // TODO MOVE ALL CODE HERE
        viewModel.getState().distinctUntilChanged().observe(viewLifecycleOwner) {

            val newAdapter: GenericFastAdapter = if (it == OverviewViewModel.State.Account) {
                accountsAdapter
            } else {
                overviewAdapter
            }

            if (binding.recycler.adapter != newAdapter) {
                binding.recycler.adapter = newAdapter

                if (it == OverviewViewModel.State.Account) {
                    movingBehavior.first.disable(movingBehavior.second, false)
                    movableAnimation.start()
                } else {
                    movingBehavior.first.enable()
                    movableAnimation.reverse()
                }
            }

            assetsTitleAdapter.set(
                listOf(
                    TitleListItem(
                        StringHolder(if (isOverviewState) R.string.id_assets else R.string.id_all_assets),
                        isAssetState,
                        withTopPadding = false
                    )
                )
            )

            // TODO fix it, its not working
            updateToolbar(false)

            onBackCallback.isEnabled = !isOverviewState
        }

        // TODO REFACTOR
        var isAccountTitleShown = false
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val shouldShowAccountTitle = scrollY > TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 54f, resources.displayMetrics).toInt()

            if (isAccountTitleShown != shouldShowAccountTitle) {
                isAccountTitleShown = shouldShowAccountTitle

                updateToolbar(shouldShowAccountTitle)
            }
        }

        updateRecyclerPadding(topPadding = true, bottomPadding = true)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                DeleteWalletBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.toString())
                }
            }

            R.id.rename -> {
                RenameWalletBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.toString())
                }
            }
            R.id.logout -> {
                viewModel.logout()
                return true
            }
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

        // TODO fix
        updateToolbar(false)


        requireActivity().window.navigationBarColor =
            ContextCompat.getColor(requireContext(), R.color.brand_surface_variant)
    }

    override fun onPause() {
        super.onPause()

        requireActivity().window.navigationBarColor =
            ContextCompat.getColor(requireContext(), R.color.brand_background)
    }

    private fun initAccountsAdapter() {
        val accountsModelAdapter = ModelAdapter { model: SubAccount ->
            AccountListItem(model, session.network)
        }.observeList(viewLifecycleOwner, viewModel.getSubAccounts())

        viewModel.getSubAccountLiveData().observe(viewLifecycleOwner) { subAccount ->
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

        val addAccountAdapter = FastItemAdapter<GenericItem>()

        if(!wallet.isWatchOnly){
            addAccountAdapter.add(ButtonActionListItem(StringHolder(R.string.id_add_new_account), true))
        }

        accountsAdapter = FastAdapter.with(listOf(accountsModelAdapter, addAccountAdapter))
        accountsAdapter.onClickListener = { _, _, item, _ ->
            when (item) {
                is AccountListItem -> {
                    viewModel.selectSubAccount(item.subAccount)
                    closeInnerAdapter()
                }

                is ButtonActionListItem -> {
                    navigate(
                        OverviewFragmentDirections.actionOverviewFragmentToChooseAccountTypeFragment(
                            wallet = args.wallet
                        )
                    )
                    closeInnerAdapter()
                }
            }

            true
        }
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

    private fun createOverviewAdapter(): GenericFastAdapter {
        val adapters = mutableListOf<IAdapter<*>>()

        adapters += ModelAdapter<AlertType, AlertListItem> {
            AlertListItem(it) { _ ->
                TwoFactorResetSheetDialogFragment.newInstance(it.twoFactorReset).also { dialog ->
                    dialog.show(childFragmentManager, dialog.toString())
                }
            }
        }.observeList(viewLifecycleOwner, viewModel.getAlerts())


        if(viewModel.wallet.isLiquid) {
            adapters += managedAssetsAccountIdAdapter
            adapters += assetsTitleAdapter
        }

        adapters += ModelAdapter<BalancePair, AssetListItem>() {
            AssetListItem(session, it, isAssetState && viewModel.wallet.isLiquid, (it.first.isEmpty() && it.second == -1L))
        }.observeMap(viewLifecycleOwner, viewModel.getBalancesLiveData() as LiveData<Map<*, *>>) {
            BalancePair(it.key as String, it.value as Long)
        }


        var titleAdapter = FastItemAdapter<GenericItem>()

        viewModel.getTransactions().distinctUntilChanged().observe(viewLifecycleOwner){
            titleAdapter.clear()

            if(it.isNotEmpty()){
                titleAdapter.add(TitleListItem(StringHolder(R.string.id_transactions)))
            }else{
                titleAdapter.add(TextListItem(StringHolder(R.string.id_your_transactions_will_be_shown)))
            }
        }


        adapters += titleAdapter

        adapters += ModelAdapter<Transaction, TransactionListItem>() {
            TransactionListItem(session, it)
        }.observeList(viewLifecycleOwner, viewModel.getTransactions())

//        val viewMoreAdapter = FastItemAdapter<GenericItem>()
//        viewMoreAdapter.add(ButtonActionListItem(StringHolder(R.string.id_view_more), false))
//        adapters += viewMoreAdapter


        val adapter = FastAdapter.with(adapters)


        // Notify the adapter when we have new balances
//        viewModel.getBalances().observe(viewLifecycleOwner){
//            adapter.notifyAdapterDataSetChanged()
//        }

        // Notify adapter when we have new assets
        viewModel.assetsUpdated.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                adapter.notifyAdapterDataSetChanged()
            }
        }

        adapter.onClickListener = { _, _, item, _ ->
            when (item) {
                is AssetListItem -> {
                    if(viewModel.wallet.isLiquid) {
                        if (isOverviewState && viewModel.wallet.isLiquid) {
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
                        startActivityForResult(txIntent, TabbedMainActivity.REQUEST_TX_DETAILS)
                    }
                }
                is TitleListItem -> {
                    if (item.showBackButton) {
                        closeOpenElements()
                    }
                }
                is ButtonActionListItem -> {
                    // navigate(OverviewFragmentDirections.actionOverviewFragmentToTransactionListFragment())
                }
            }

            true
        }

        return adapter
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