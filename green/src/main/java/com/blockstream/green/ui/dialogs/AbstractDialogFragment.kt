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
import com.blockstream.green.data.Countly
import com.blockstream.green.data.ScreenView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mu.KLogging
import javax.inject.Inject

// Based on https://dev.to/bhullnatik/how-to-use-material-dialogs-with-dialogfragment-28i1
abstract class AbstractDialogFragment<T : ViewDataBinding> : DialogFragment(), ScreenView{
    @Inject
    lateinit var countly: Countly

    private var bindingOrNull: T? = null
    protected val binding: T get() = bindingOrNull!!

    override var screenIsRecorded = false
    override val segmentation: HashMap<String, Any>? = null

    open val isFullWidth: Boolean = false

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
    }

    override fun onResume() {
        super.onResume()
        countly.screenView(this)
    }

    companion object : KLogging() {
        fun show(instance: AbstractDialogFragment<*>, fragmentManager: FragmentManager){
            instance.show(fragmentManager, instance.javaClass.simpleName)
        }

        // Open a single instance
        fun showSingle(instance: AbstractDialogFragment<*>, fragmentManager: FragmentManager){
            val tag = instance.javaClass.simpleName
            if (fragmentManager.findFragmentByTag(tag) == null) {
                show(instance, fragmentManager)
            } else {
                logger.info { "There is already an open instance of ${instance.javaClass.simpleName}" }
            }
        }
    }
}
