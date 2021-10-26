package com.blockstream.green.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.params.SweepParams
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.*
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.utils.*
import com.greenaddress.greenapi.Session
import com.greenaddress.greenbits.ui.send.SendAmountActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@AndroidEntryPoint
class SweepFragment : WalletFragment<SweepFragmentBinding>(
    layout = R.layout.sweep_fragment,
    menuRes = 0
) {
    override val isAdjustResize = true

    val args: SweepFragmentArgs by navArgs()

    override val wallet by lazy { args.wallet }

    private val startForResultSweep = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            findNavController().popBackStack(R.id.overviewFragment, false)
        }
    }

    @Inject
    lateinit var viewModelFactory: SweepViewModel.AssistedFactory
    val viewModel: SweepViewModel by viewModels {
        SweepViewModel.provideFactory(viewModelFactory, wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) {
            it?.let { result ->
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                binding.textInputEditText.setText(result)
            }
        }

        binding.vm = viewModel
        binding.textInputLayout.endIconCopyMode()

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.Navigate>()?.let {
                startForResultSweep.launch(Intent(requireContext(), SendAmountActivity::class.java))
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.open(this)
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.sweep()
        }
    }
}