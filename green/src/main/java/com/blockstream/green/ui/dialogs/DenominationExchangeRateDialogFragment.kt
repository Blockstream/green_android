package com.blockstream.green.ui.dialogs

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.settings.DenominationExchangeRateViewModel
import com.blockstream.compose.utils.stringResourceId
import com.blockstream.green.databinding.DenominationExchangeDialogBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DenominationExchangeRateDialogFragment :
    AbstractDialogFragment<DenominationExchangeDialogBinding, DenominationExchangeRateViewModel>() {

    override fun inflate(layoutInflater: LayoutInflater): DenominationExchangeDialogBinding =
        DenominationExchangeDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true

    override val viewModel: DenominationExchangeRateViewModel by viewModel {
        parametersOf(BundleCompat.getParcelable(requireArguments(), GREEN_WALLET, GreenWallet::class.java))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Denomination
        binding.denomination.setOnItemClickListener { _, _, position, _ ->
            viewModel.postEvent(
                DenominationExchangeRateViewModel.LocalEvents.Set(
                    unit = viewModel.units[position],
                )
            )
        }

        binding.denomination.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.simple_list_item_1,
                viewModel.units
            )
        )

        viewModel.selectedUnit.onEach {
            binding.denomination.setText(it, false)
        }.launchIn(lifecycleScope)

        // Exchange Rate
        binding.exchangeRate.setOnItemClickListener { _, _, position, _ ->
            viewModel.postEvent(
                DenominationExchangeRateViewModel.LocalEvents.Set(
                    exchangeAndCurrency = viewModel.exchangeAndCurrencies.value[position]
                )
            )
        }

        viewModel.exchangeAndCurrencies.onEach {
            binding.exchangeRate.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.simple_list_item_1,
                    it.map { id ->
                        stringResourceId(requireContext(), id)
                    }
                )
            )
        }.launchIn(lifecycleScope)

        viewModel.selectedExchangeAndCurrency.onEach {
            binding.exchangeRate.setText(stringResourceId(requireContext(), it), false)
        }.launchIn(lifecycleScope)

        binding.cancel.setOnClickListener {
            dismiss()
        }

        binding.ok.setOnClickListener {
            viewModel.postEvent(DenominationExchangeRateViewModel.LocalEvents.Save)
        }
    }

    companion object : KLogging() {
        private const val GREEN_WALLET = "GREEN_WALLET"
        fun show(greenWallet: GreenWallet, fragmentManager: FragmentManager) {
            showSingle(DenominationExchangeRateDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(
                        GREEN_WALLET, greenWallet
                    )
                }
            }, fragmentManager)
        }
    }
}
