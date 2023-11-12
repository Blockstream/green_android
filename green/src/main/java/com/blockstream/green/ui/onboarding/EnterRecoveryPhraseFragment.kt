package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.getBip39WordList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.EnterRecoveryPhraseViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.EnterRecoveryPhraseFragmentBinding
import com.blockstream.green.extensions.clearClipboard
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.HelpBottomSheetDialogFragment
import com.blockstream.green.ui.items.RecoveryPhraseWordListItem
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.isProductionFlavor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.AlphaInAnimator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class EnterRecoveryPhraseFragment : AppFragment<EnterRecoveryPhraseFragmentBinding>(
    R.layout.enter_recovery_phrase_fragment,
    menuRes = R.menu.menu_help
) {

    val args: EnterRecoveryPhraseFragmentArgs by navArgs()

    override val title: String?
        get() = args.setupArgs.network?.canonicalName

    override val toolbarIcon: Int?
        get() = args.setupArgs.network?.getNetworkIcon()

    val viewModel: EnterRecoveryPhraseViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    private var itemAdapter: FastItemAdapter<RecoveryPhraseWordListItem> = FastItemAdapter()

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        (sideEffect as? SideEffects.NavigateTo)?.also {
            (it.destination as? NavigateDestinations.SetPin)?.also {
                navigate(
                    EnterRecoveryPhraseFragmentDirections.actionEnterRecoveryPhraseFragmentToPinFragment(
                        setupArgs = it.args,
                    )
                )
            }

            (it.destination as? NavigateDestinations.AddAccount)?.also {
                navigate(
                    EnterRecoveryPhraseFragmentDirections.actionGlobalReviewAddAccountFragment(
                        setupArgs = it.args,
                    )
                )
            }
        }

        (sideEffect as? EnterRecoveryPhraseViewModel.LocalSideEffects.RequestMnemonicPassword)?.also {
            val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))
            dialogBinding.textInputLayout.endIconCustomMode()

            dialogBinding.hint = getString(R.string.id_password)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                dialogBinding.editText.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or EditorInfo.IME_ACTION_GO
            }else{
                dialogBinding.editText.imeOptions = EditorInfo.IME_ACTION_GO
            }
            dialogBinding.editText.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD

            // Until this is fixed https://github.com/material-components/material-components-android/issues/503
            dialogBinding.textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.id_encryption_passphrase)
                .setView(dialogBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.postEvent(EnterRecoveryPhraseViewModel.LocalEvents.MnemonicEncryptionPassword(dialogBinding.text ?: ""))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()

            // set focus to the input field
            dialogBinding.editText.requestFocus()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.postEvent(EnterRecoveryPhraseViewModel.LocalEvents.SetRecoveryPhrase(result.result))
            }
        }

        binding.vm = viewModel
        binding.recoveryPhraseKeyboardView.bridge(viewModel.recoveryPhrase, viewModel.activeWord)

        binding.buttonContinue.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _, _, _, position ->
            binding.recoveryPhraseKeyboardView.toggleActiveWord(position)
            true
        }

        binding.recycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
            itemAnimator = AlphaInAnimator()
            adapter = fastAdapter
        }

        binding.recoveryPhraseKeyboardView.setWordList(wally.getBip39WordList())

        binding.buttonPaste.setOnClickListener {
            getClipboard(requireContext())?.also {
                viewModel.postEvent(EnterRecoveryPhraseViewModel.LocalEvents.SetRecoveryPhrase(it))
            }
            // Clear clipboard
            if(isProductionFlavor) {
                clearClipboard()
            }
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, fragmentManager = childFragmentManager)
        }

        binding.message.setOnClickListener {
            Toast.makeText(requireContext(), binding.message.text, Toast.LENGTH_LONG).show()
        }

        binding.buttonHelp.setOnClickListener {
            HelpBottomSheetDialogFragment.show(childFragmentManager)
        }

        binding.toggleRecoverySize.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked) {
                viewModel.recoveryPhraseSize.value = when (checkedId) {
                    R.id.button12 -> 12
                    R.id.button24 -> 24
                    else -> 27
                }
            }
        }

        viewModel.recoveryPhraseSize.onEach {
            binding.toggleRecoverySize.check(when(it){
                0, 12 -> R.id.button12
                24 -> R.id.button24
                27 -> R.id.button27
                else -> 0
            })
        }.launchIn(lifecycleScope)

        combine(viewModel.recoveryPhrase, viewModel.recoveryPhraseSize, viewModel.activeWord) { recoveryPhrase, _, activeWord ->
            val list = mutableListOf<RecoveryPhraseWordListItem>()

            recoveryPhrase.forEach { word ->
                list += RecoveryPhraseWordListItem(list.size + 1, word, activeWord == list.size)
            }

            while (list.size < viewModel.recoveryPhraseSize.value) {
                list += RecoveryPhraseWordListItem(list.size + 1, "", list.isEmpty())
            }

            viewModel.recoveryPhraseSize.value = when(list.size){
                in 0..12 -> 12
                in 12..24 -> 24
                else -> {
                    27
                }
            }

            FastAdapterDiffUtil.set(itemAdapter.itemAdapter, list, true)
            binding.recycler.scrollToPosition(viewModel.activeWord.value.takeIf { it >= 0 } ?: viewModel.recoveryPhrase.value.size)
        }.launchIn(lifecycleScope)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.help -> {
                HelpBottomSheetDialogFragment.show(childFragmentManager)
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }
}