package com.blockstream.green.ui.overview

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.base.IAppReview
import com.blockstream.base.Urls
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.ApplicationScope
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.AccountOverviewFragmentBinding
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.AssetPair
import com.blockstream.green.gdk.needs2faActivation
import com.blockstream.green.ui.bottomsheets.AssetDetailsBottomSheetFragment
import com.blockstream.green.ui.bottomsheets.Call2ActionBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameAccountBottomSheetDialogFragment
import com.blockstream.green.ui.items.*
import com.blockstream.green.ui.wallet.AbstractAccountWalletFragment
import com.blockstream.green.utils.*
import com.blockstream.green.views.AccordionListener
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
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@AndroidEntryPoint
class AccountOverviewFragment : AbstractAccountWalletFragment<AccountOverviewFragmentBinding>(
    layout = R.layout.account_overview_fragment,
    menuRes = R.menu.account_overview
) {

    val args: AccountOverviewFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var appReview: IAppReview

    @Inject
    lateinit var viewModelFactory: AccountOverviewViewModel.AssistedFactory
    val viewModel: AccountOverviewViewModel by viewModels {
        AccountOverviewViewModel.provideFactory(viewModelFactory, args.wallet, args.account)
    }

    override fun getAccountWalletViewModel() = viewModel

    var fastAdapter: FastAdapter<GenericItem>? = null

    private fun networkAssetManager() = session.networkAssetManager

    // Allow super to display hww device if exists
    override val overrideSubtitle: Boolean = false

    override val screenName = "AccountOverview"

    override val segmentation
        get() = if (isSessionAndWalletRequired() && isSessionNetworkInitialized) countly.accountSegmentation(
            session = session,
            account = args.account
        ) else null

    private val singlesigWarningThreshold
        get() =  if(network.isMainnet) 5_000_000 else 100_000 // 0.05 BTC or 0.001 tBTC

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel
        binding.bottomNav.isWatchOnly = wallet.isWatchOnly
        binding.bottomNav.showSwap = false //account.isLiquid && account.isMultisig

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateBack>()?.let {
                popBackStack()
            }
        }

        val fastAdapter = setupAdapters(binding.recycler)

        binding.bottomNav.buttonReceive.setOnClickListener {
            navigate(
                AccountOverviewFragmentDirections.actionAccountOverviewFragmentToReceiveFragment(
                    wallet = viewModel.wallet,
                    accountAsset = AccountAsset.fromAccount(account),
                )
            )
        }

        binding.bottomNav.buttonSend.setOnClickListener {
            when {
                session.isWatchOnly -> {
                    navigate(
                        AccountOverviewFragmentDirections.actionAccountOverviewFragmentToSendFragment(
                            wallet = wallet,
                            accountAsset = AccountAsset.fromAccount(account),
                            isSweep = true
                        )
                    )
                }
                viewModel.policyAsset == 0L -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.id_warning)
                        .setMessage(if (network.isLiquid) R.string.id_insufficient_lbtc_to_send_a else R.string.id_you_have_no_coins_to_send)
                        .also {
                            if (network.isLiquid) {
                                it.setPositiveButton(R.string.id_learn_more) { _: DialogInterface, _: Int ->
                                    openBrowser(Urls.HELP_GET_LIQUID)
                                }
                            } else {
                                it.setPositiveButton(R.string.id_receive) { _: DialogInterface, _: Int ->
                                    navigate(
                                        AccountOverviewFragmentDirections.actionAccountOverviewFragmentToReceiveFragment(
                                            wallet = viewModel.wallet,
                                            accountAsset = AccountAsset.fromAccount(viewModel.account)
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
                        AccountOverviewFragmentDirections.actionAccountOverviewFragmentToSendFragment(
                            wallet = wallet,
                            accountAsset = AccountAsset.fromAccount(account),
                            network = viewModel.account.network
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
    }

    override fun onPrepareMenu(menu: Menu) {
        // Prevent from archiving all your accounts
        menu.findItem(R.id.archive).isVisible = viewModel.accounts.size > 1
        menu.findItem(R.id.help).isVisible = account.isAmp && viewModel.assets.isNotEmpty()
        menu.findItem(R.id.enhance_security).isVisible = account.needs2faActivation(session)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.help -> {
                openBrowser(Urls.HELP_AMP_ASSETS)
                true
            }
            R.id.rename -> {
                RenameAccountBottomSheetDialogFragment.show(
                    account,
                    childFragmentManager
                )
                true
            }
            R.id.archive -> {
                viewModel.archiveAccount()
                snackbar(R.string.id_account_has_been_archived)
                true
            }
            R.id.enhance_security -> {
                navigate(AccountOverviewFragmentDirections.actionGlobalTwoFractorAuthenticationFragment(
                    wallet = wallet,
                    network = account.network
                ))
                true
            }
            R.id.addresses -> {
                navigate(
                    AccountOverviewFragmentDirections.actionAccountOverviewFragmentToAddressesFragment(
                        wallet = wallet,
                        account = account
                    )
                )
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
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

    private fun setupAdapters(recycler: RecyclerView): GenericFastAdapter {

        val accountAdapter: GenericFastItemAdapter = FastItemAdapter()

        viewModel.accountLiveData.observe(viewLifecycleOwner) {
            AccountsListItem(
                session = session,
                accounts = listOf(viewModel.account),
                showArrow = false,
                show2faActivation = false,
                showCopy = account.isAmp,
                listener = object :
                    AccordionListener {
                    override fun expandListener(view: View, position: Int) { }

                    override fun arrowClickListener(view: View, position: Int) { }

                    override fun copyClickListener(view: View, position: Int) {
                        copyToClipboard("AccountID", account.receivingId, requireContext())
                        snackbar(R.string.id_copied_to_clipboard)
                    }

                    override fun warningClickListener(view: View, position: Int) { }

                    override fun longClickListener(view:View, position: Int) {
                        val menu = if (viewModel.accounts.size == 1) R.menu.menu_account else R.menu.menu_account_archive
                        showPopupMenu(view, menu) { menuItem ->
                            when (menuItem.itemId) {
                                R.id.rename -> {
                                    RenameAccountBottomSheetDialogFragment.show(
                                        account,
                                        childFragmentManager
                                    )
                                }
                                R.id.archive -> {
                                    viewModel.archiveAccount()
                                    snackbar(R.string.id_account_has_been_archived)
                                }
                            }
                            true
                        }
                    }

                }).also {
                accountAdapter.set(listOf(it))
            }

            // Update toolbar to shot the account name
            updateToolbar()
        }

        // AMP Id
        val ampAccountHelpAdapter: GenericFastItemAdapter = FastItemAdapter()

        // Assets Balance
        @Suppress("UNCHECKED_CAST")
        val assetsBalanceAdapter = ModelAdapter<AssetPair, AssetListItem> {
            AssetListItem(
                session = session,
                assetPair = it,
                showBalance = true,
                isLoading = (it.first.isEmpty() && it.second == -1L)
            )
        }.observeMap(
            viewLifecycleOwner,
            viewModel.assetsLiveData as LiveData<Map<*, *>>,
            toModel = {
                AssetPair(it.key as String, it.value as Long)
            })

        val call2ActionAdapter: GenericFastItemAdapter = FastItemAdapter()

        viewModel.assetsLiveData.observe(viewLifecycleOwner) {

            // Disabled
            if(account.isMultisig) {
                if (account.needs2faActivation(session)) {
                    call2ActionAdapter.set(
                        listOf(
                            AccountWarningListItem(
                                account = account,
                                style = 1
                            )
                        )
                    )
                } else {
                    call2ActionAdapter.set(emptyList())
                }
            }else{
//                val noMultisigAccount = session.accounts.firstOrNull { it.isMultisig && it.isBitcoin == network.isBitcoin } == null
//                if (noMultisigAccount && account.isSinglesig && (it[network.policyAsset] ?: 0) >= singlesigWarningThreshold) {
//                    call2ActionAdapter.set(
//                        listOf(
//                            AccountWarningListItem(
//                                account = account
//                            )
//                        )
//                    )
//                } else {
//                    call2ActionAdapter.set(emptyList())
//                }
            }

            if (account.isAmp && it.isEmpty()) {
                ampAccountHelpAdapter.set(
                    listOf(
                        ContentCardListItem(
                            title = StringHolder(R.string.id_learn_more_about_amp_the_assets),
                            caption = StringHolder(R.string.id_check_our_6_easy_steps_to_be)
                        )
                    )
                )
            }else {
                ampAccountHelpAdapter.clear()
            }
        }

        val titleAdapter = FastItemAdapter<GenericItem>()

        viewModel.transactionsLiveData.observe(viewLifecycleOwner) { transactions ->
            mutableListOf<GenericItem>(TitleListItem(StringHolder(R.string.id_transactions))).also { list ->
                if(transactions.isEmpty()){
                    list += TextListItem(
                        text = StringHolder(R.string.id_your_transactions_will_be_shown),
                        textColor = R.color.color_on_surface_emphasis_low
                    )
                }
            }.also {
                titleAdapter.set(it)
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
            TransactionListItem(it, session)
        }.observeList(viewLifecycleOwner, viewModel.transactionsLiveData)

        viewModel.transactionsPagerLiveData.observe(viewLifecycleOwner) { hasMoreTransactions ->
            transactionsFooterAdapter.clear()

            if(hasMoreTransactions == true){
                lifecycleScope.launchWhenResumed {
                    delay(200L)
                    endlessRecyclerOnScrollListener.enable()
                }
            }else{
                endlessRecyclerOnScrollListener.disable()
            }
        }

        recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        val adapters = listOfNotNull(
            accountAdapter,
            ampAccountHelpAdapter,
            assetsBalanceAdapter,
            call2ActionAdapter,
            titleAdapter,
            transactionAdapter,
            transactionsFooterAdapter
        )

        val fastAdapter = FastAdapter.with(adapters).also {
            this.fastAdapter = it
        }

        // Notify adapter when assets are updated
        networkAssetManager().assetsUpdateFlow.onEach {
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        // Update transactions
        session.blockFlow(network).onEach {
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        fastAdapter.onClickListener = { _, _, item, _ ->
            when (item) {
                is AssetListItem -> {
                    AssetDetailsBottomSheetFragment.show(
                        item.assetPair.first,
                        account = viewModel.account,
                        childFragmentManager
                    )
                }
                is AccountWarningListItem -> {
                    if(account.isSinglesig) {
                        Call2ActionBottomSheetDialogFragment.showIncreaseSecurity(
                            account,
                            childFragmentManager
                        )
                    }else{
                        // Navigate directly
                        navigate(AccountOverviewFragmentDirections.actionGlobalTwoFractorAuthenticationFragment(
                            wallet = wallet,
                            network = account.network
                        ))
//                        Call2ActionBottomSheetDialogFragment.showEnable2FA(
//                            account,
//                            childFragmentManager
//                        )
                    }
                }
                is ContentCardListItem -> {
                    openBrowser(Urls.HELP_AMP_ASSETS)
                }
                is TransactionListItem -> {
                    navigate(
                        AccountOverviewFragmentDirections.actionAccountOverviewFragmentToTransactionDetailsFragment(
                            wallet = wallet,
                            account = account,
                            transaction = item.tx
                        )
                    )
                }
            }

            true
        }

        return fastAdapter
    }
}