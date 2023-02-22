package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.R
import com.blockstream.green.data.DenominatedValue
import com.blockstream.green.data.Denomination
import com.blockstream.green.databinding.RecyclerBottomSheetBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.AbstractBindingItem
import com.blockstream.green.ui.items.DenominationListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class DenominationBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<RecyclerBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "Denomination"

    @Inject
    lateinit var settingsManager: SettingsManager

    private val denominatedValue by lazy { requireArguments().getParcelable<DenominatedValue>(DENOMINATED_VALUE) ?: DenominatedValue.createDefault(session) }

    override fun inflate(layoutInflater: LayoutInflater) =
        RecyclerBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title = getString(R.string.id_enter_amount_in)

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = createFastAdapter()
            addItemDecoration(
                MaterialDividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                ).also {
                    it.isLastItemDecorated = true
                }
            )
        }
    }

    private fun createFastAdapter(): FastAdapter<AbstractBindingItem<*>> {
        val itemAdapter = ItemAdapter<DenominationListItem>()

        val list = mutableListOf<DenominationListItem>()

        list += DenominationListItem(session = session, DenominatedValue.toDenomination(denominatedValue, Denomination.BTC), denominatedValue.denomination is Denomination.BTC)
        list += DenominationListItem(session = session, DenominatedValue.toDenomination(denominatedValue, Denomination.MBTC), denominatedValue.denomination is Denomination.MBTC)
        list += DenominationListItem(session = session, DenominatedValue.toDenomination(denominatedValue, Denomination.UBTC), denominatedValue.denomination is Denomination.UBTC)
        list += DenominationListItem(session = session, DenominatedValue.toDenomination(denominatedValue, Denomination.BITS), denominatedValue.denomination is Denomination.BITS)
        list += DenominationListItem(session = session, DenominatedValue.toDenomination(denominatedValue, Denomination.SATOSHI), denominatedValue.denomination is Denomination.SATOSHI)

        session.getSettings()?.pricing?.currency?.also {
            list += DenominationListItem(
                session = session,
                denominatedValue = DenominatedValue.toDenomination(denominatedValue, Denomination.FIAT(it)),
                isChecked = denominatedValue.denomination is Denomination.FIAT
            )
        }

        itemAdapter.set(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _: View?, _, item: GenericItem, _: Int ->
                if(item is DenominationListItem){
                    (viewModel as? DenominationListener)?.also {
                        it.setDenomination(item.denominatedValue)
                        dismiss()
                    }
                }

                true
            }

        return fastAdapter as FastAdapter<AbstractBindingItem<*>>
    }

    companion object : KLogging() {
        private const val DENOMINATED_VALUE = "DENOMINATED_VALUE"

        fun show(denominatedValue: DenominatedValue, fragmentManager: FragmentManager) {
            show(DenominationBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(DENOMINATED_VALUE, denominatedValue)
                }
            }, fragmentManager)
        }
    }
}

interface DenominationListener{
    fun setDenomination(denominatedValue: DenominatedValue)
}