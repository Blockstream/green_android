package com.blockstream.green.ui.addresses


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.data.Address
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemAddressBinding
import com.blockstream.green.ui.items.AddressListItem
import com.blockstream.green.ui.items.ProgressListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.wallet.AbstractAccountWalletFragment
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.views.EndlessRecyclerOnScrollListener
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class AddressesFragment :
    AbstractAccountWalletFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {

    val args: AddressesFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "PreviousAddresses"

    @Inject
    lateinit var viewModelFactory: AddressesViewModel.AssistedFactory
    val viewModel: AddressesViewModel by viewModels {
        AddressesViewModel.provideFactory(viewModelFactory, args.wallet, args.account)
    }

    override fun getAccountWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        val titleAdapter = FastItemAdapter<GenericItem>()

        val addressesModelAdapter = ModelAdapter { address: Address ->
            AddressListItem(
                index = viewModel.addressesLiveData.value?.indexOf(address) ?: 1,
                address = address,
            )
        }.observeList(viewLifecycleOwner, viewModel.addressesLiveData) {
            if (it.isEmpty()) {
                titleAdapter.set(
                    listOf(
                        TextListItem(
                            text = StringHolder(R.string.id_no_addresses),
                            textColor = R.color.color_on_surface_emphasis_low,
                            textAlignment = View.TEXT_ALIGNMENT_CENTER,
                            paddingTop = R.dimen.dp32,
                            paddingBottom = R.dimen.dp24,
                            paddingLeft = R.dimen.dp24,
                            paddingRight = R.dimen.dp24
                        )
                    )
                )
            } else {
                titleAdapter.clear()
            }
        }

        val footerAdapter = ItemAdapter<GenericItem>()

        val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener(binding.recycler) {
            override fun onLoadMore() {
                footerAdapter.set(listOf(ProgressListItem()))
                disable()
                viewModel.getPreviousAddresses()
            }
        }.also {
            it.disable()
        }

        viewModel.pagerLiveData.observe(viewLifecycleOwner) { hasMoreTransactions ->
            footerAdapter.clear()

            if(hasMoreTransactions == true){
                lifecycleScope.launchWhenResumed {
                    delay(200L)
                    endlessRecyclerOnScrollListener.enable()
                }
            }else{
                endlessRecyclerOnScrollListener.disable()
            }
        }

        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        val fastAdapter = FastAdapter.with(listOf(titleAdapter, addressesModelAdapter))

        fastAdapter.onClickListener = { v, _, item, _ ->
            if(item is AddressListItem) {
                v?.let {
                    showPopupMenu(v.findViewById(R.id.addressTextView) ?: v, item.address)
                }
            }
            true
        }

        fastAdapter.addClickListener<ListItemAddressBinding, GenericItem>({ binding -> binding.buttonCopy }) { _, _, _, item ->
            if(item is AddressListItem) {
                copyToClipboard("Address", item.address.address)
            }
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }

    private fun showPopupMenu(view: View, address: Address) {
        showPopupMenu(
            view,
            R.menu.menu_previous_address
        ) { menuItem ->
            when (menuItem.itemId) {
                R.id.block_explorer -> {
                    openBrowser("${account.network.explorerUrl?.replace("/tx/", "/address/")}${address.address}")
                }
            }
            true
        }
    }
}
