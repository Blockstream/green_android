package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.extensions.getAssetName
import com.blockstream.common.models.demo.DemoViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.DemoFragmentBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class DemoFragment : AppFragment<DemoFragmentBinding>(R.layout.demo_fragment, menuRes = 0) {

    val viewModel: DemoViewModel by viewModel()

    override val screenName = "About"

    override fun getGreenViewModel() = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.buttonLogin.setOnClickListener {
            viewModel.postEvent(DemoViewModel.LocalEvents.EventLogin)
        }

        binding.buttonRefresh.setOnClickListener {
            viewModel.postEvent(DemoViewModel.LocalEvents.EventRefresh)
        }

        viewModel.walletBalance.onEach {
            binding.balance.text = it
        }.launchIn(lifecycleScope)

        viewModel.accounts.onEach {
            binding.accounts.text = it.firstOrNull()?.name
        }.launchIn(lifecycleScope)

        viewModel.transactions.onEach {
            binding.transactions.text = it.firstOrNull()?.txHash.toString()
        }.launchIn(lifecycleScope)

        viewModel.walletAssets.onEach {
            binding.assets.text = it.assets.map {
                it.key.getAssetName(viewModel.gdkSession)
            }.joinToString(separator = "\n")
        }.launchIn(lifecycleScope)
    }
}