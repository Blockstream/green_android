package com.blockstream.green.ui.bottomsheets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import com.blockstream.green.data.Countly
import com.blockstream.green.data.ScreenView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.disposables.CompositeDisposable
import mu.KLogging
import javax.inject.Inject

abstract class AbstractBottomSheetDialogFragment<T : ViewDataBinding>: BottomSheetDialogFragment(),
    ScreenView {
    @Inject
    lateinit var countly: Countly

    internal val disposables = CompositeDisposable()

    protected lateinit var binding: T

    override var screenIsRecorded = false
    override val segmentation: HashMap<String, Any>? = null

    open val expanded: Boolean = false
    open val isAdjustResize: Boolean = false

    abstract fun inflate(layoutInflater: LayoutInflater): T

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).also {
            if(isAdjustResize) {
                it.behavior.skipCollapsed = true
            }
            if(expanded || isAdjustResize){
                it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflate(inflater).also {
            binding = it
            binding.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isAdjustResize){
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    override fun onResume() {
        super.onResume()
        countly.screenView(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    companion object : KLogging() {
        fun show(instance: AbstractBottomSheetDialogFragment<*>, fragmentManager: FragmentManager){
            instance.show(fragmentManager, instance.javaClass.simpleName)
        }

        // Open a single instance
        fun showSingle(instance: AbstractBottomSheetDialogFragment<*>, fragmentManager: FragmentManager){
            val tag = instance.javaClass.simpleName
            if (fragmentManager.findFragmentByTag(tag) == null) {
                show(instance, fragmentManager)
            } else {
                logger.info { "There is already an open instance of ${instance.javaClass.simpleName}" }
            }
        }
    }
}

interface DismissBottomSheetDialogListener{
    fun dialogDismissed(dialog: AbstractBottomSheetDialogFragment<*>)
}