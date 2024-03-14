package com.blockstream.green.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockstream.common.ScreenView
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.ui.AppFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KLogging
import org.koin.android.ext.android.inject

// Based on https://dev.to/bhullnatik/how-to-use-material-dialogs-with-dialogfragment-28i1
abstract class AbstractDialogFragment<T : ViewDataBinding, VM: GreenViewModel> : DialogFragment(), ScreenView {
    abstract val viewModel: VM?

    protected val countly: CountlyAndroid by inject()

    private var bindingOrNull: T? = null
    protected val binding: T get() = bindingOrNull!!

    override var screenIsRecorded = false
    override val segmentation: HashMap<String, Any>? = null

    open val isFullWidth: Boolean = false

    val appFragment : AppFragment<*>?
        get() = (parentFragment as? AppFragment<*>)

    abstract fun inflate(layoutInflater: LayoutInflater): T

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(onCreateView(layoutInflater, null, savedInstanceState))
            .create()
    }

    // Warning: onCreateView can be called multiple times
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return bindingOrNull?.root ?: inflate(inflater).also {
            bindingOrNull = it
            // binding.lifecycleOwner = viewLifecycleOwner
            // viewLifecycleOwner is unavailable for MaterialAlertDialogBuilder based dialogs
            binding.lifecycleOwner = parentFragment?.viewLifecycleOwner ?: activity
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isFullWidth){
            dialog?.window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }

        viewModel?.also { viewModel ->
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.sideEffect.onEach {
                        if(it is SideEffects.Dismiss || it is SideEffects.Logout){
                            dismiss()
                        }
                    }.launchIn(lifecycleScope)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        countly.screenView(this)
    }

    companion object : KLogging() {
        fun show(instance: AbstractDialogFragment<*, *>, fragmentManager: FragmentManager){
            instance.show(fragmentManager, instance.javaClass.simpleName)
        }

        // Open a single instance
        fun showSingle(instance: AbstractDialogFragment<*, *>, fragmentManager: FragmentManager){
            val tag = instance.javaClass.simpleName
            if (fragmentManager.findFragmentByTag(tag) == null) {
                show(instance, fragmentManager)
            } else {
                logger.info { "There is already an open instance of ${instance.javaClass.simpleName}" }
            }
        }
    }
}
