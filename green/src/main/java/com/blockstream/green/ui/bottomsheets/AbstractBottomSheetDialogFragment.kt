package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import com.blockstream.green.data.Countly
import com.blockstream.green.data.ScreenView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mu.KLogging
import javax.inject.Inject

abstract class AbstractBottomSheetDialogFragment<T : ViewDataBinding>: BottomSheetDialogFragment(),
    ScreenView {
    @Inject
    lateinit var countly: Countly

    protected lateinit var binding: T

    override var screenIsRecorded = false
    override val segmentation: HashMap<String, Any>? = null

    abstract fun inflate(layoutInflater: LayoutInflater): T

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

    override fun onResume() {
        super.onResume()
        countly.screenView(this)
    }

    companion object : KLogging() {

        fun show(instance: AbstractBottomSheetDialogFragment<*>, fragmentManager: FragmentManager){
            instance.show(fragmentManager, instance.toString())
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