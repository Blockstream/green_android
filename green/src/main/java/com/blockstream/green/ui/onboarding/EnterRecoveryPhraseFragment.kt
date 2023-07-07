package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.getBip39WordList
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.EnterRecoveryPhraseFragmentBinding
import com.blockstream.green.extensions.clearClipboard
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.HelpBottomSheetDialogFragment
import com.blockstream.green.ui.items.RecoveryPhraseWordListItem
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.views.RecoveryPhraseKeyboardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.AlphaInAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class EnterRecoveryPhraseFragment :
    AbstractOnboardingFragment<EnterRecoveryPhraseFragmentBinding>(
        R.layout.enter_recovery_phrase_fragment,
        menuRes = R.menu.menu_help
    ) {

    val args: EnterRecoveryPhraseFragmentArgs by navArgs()

    override val screenName = "OnBoardEnterRecovery"
    override val segmentation: HashMap<String, Any>? = null

    override val title: String?
        get() = args.network?.canonicalName

    override val toolbarIcon: Int?
        get() = args.network?.getNetworkIcon()

    @Inject
    lateinit var assistedFactory: EnterRecoveryPhraseViewModel.AssistedFactory

    val viewModel: EnterRecoveryPhraseViewModel by viewModels{
        EnterRecoveryPhraseViewModel.provideFactory(
            assistedFactory = assistedFactory, recoveryPhrase = args.scannedInput
        )
    }

    private var itemAdapter: FastItemAdapter<RecoveryPhraseWordListItem> = FastItemAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                binding.recoveryPhraseKeyboardView.setRecoveryPhraseState(
                    RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString(result.trim())
                )
            }
        }

        options = args.onboardingOptions

        binding.vm = viewModel

        binding.buttonContinue.setOnClickListener {

            if(viewModel.isEncryptionPasswordRequired){

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

                        // A 27-word mnemonic is a multisig network
                        options?.apply {
                            navigate(
                                options = this,
                                mnemonic = viewModel.recoveryPhraseState.value!!.toMnemonic(),
                                mnemonicPassword = dialogBinding.text ?: ""
                            )
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

                // set focus to the input field
                dialogBinding.editText.requestFocus()

            }else{
                navigate(options = options, mnemonic = viewModel.recoveryPhraseState.value!!.toMnemonic(), mnemonicPassword = "")
            }
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

        binding.recoveryPhraseKeyboardView.setOnRecoveryPhraseKeyboardListener(object :
            RecoveryPhraseKeyboardView.OnRecoveryPhraseKeyboardListener {
            override fun onRecoveryPhraseStateUpdate(state: RecoveryPhraseKeyboardView.RecoveryPhraseState) {
                viewModel.updateRecoveryPhrase(state)
            }
        })

        binding.buttonPaste.setOnClickListener {
            getClipboard(requireContext())?.trim()?.apply {
                if(isNotBlank()){
                    binding.recoveryPhraseKeyboardView.setRecoveryPhraseState(
                        RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString(this)
                    )
                }
            }
            clearClipboard()
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
                viewModel.setRecoveryPhraseSize(when (checkedId) {
                    R.id.button12 -> 12
                    R.id.button24 -> 24
                    else -> 27
                })
            }
        }

        viewModel.recoveryPhraseSize.distinctUntilChanged().observe(viewLifecycleOwner){
            binding.toggleRecoverySize.check(when(it){
                0, 12 -> R.id.button12
                24 -> R.id.button24
                27 -> R.id.button27
                else -> 0
            })
        }

        viewModel.recoveryPhraseState.value?.let {
            binding.recoveryPhraseKeyboardView.setRecoveryPhraseState(it)
        }

        viewModel.recoveryWords.observe(viewLifecycleOwner) {
            FastAdapterDiffUtil.set(itemAdapter.itemAdapter, it, false)
            binding.recycler.scrollToPosition(viewModel.recoveryPhraseState.value?.let { value -> if(value.activeIndex >= 0) value.activeIndex else value.phrase.size }?: 0)
        }
    }

    fun navigate(options: OnboardingOptions? , mnemonic: String, mnemonicPassword: String){
        if(options != null){
            navigate(
                EnterRecoveryPhraseFragmentDirections.actionEnterRecoveryPhraseFragmentToSetPinFragment(
                    restoreWallet = args.wallet,
                    onboardingOptions = options,
                    mnemonic = mnemonic,
                    password = mnemonicPassword
                )
            )
        } else if (args.wallet != null && args.network != null){
            navigate(
                EnterRecoveryPhraseFragmentDirections.actionGlobalReviewAddAccountFragment(
                    wallet = args.wallet!!,
                    assetId = args.assetId!!,
                    network = args.network!!,
                    accountType = AccountType.TWO_OF_THREE,
                    mnemonic = mnemonic
                )
            )
        }
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