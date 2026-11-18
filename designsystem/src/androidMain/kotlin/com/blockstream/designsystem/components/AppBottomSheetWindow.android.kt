package com.blockstream.designsystem.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Outline
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.blockstream.designsystem.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal actual fun AppBottomSheetWindow(
    onDismissRequest: () -> Unit,
    properties: AppBottomSheetProperties,
    contentColor: Color,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val windowId = rememberSaveable { UUID.randomUUID() }
    val scope = rememberCoroutineScope()
    val dialog = remember(view, density) {
        AppBottomSheetDialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            contentColor = contentColor,
            composeView = view,
            layoutDirection = layoutDirection,
            density = density,
            windowId = windowId,
            predictiveBackProgress = predictiveBackProgress,
            scope = scope
        ).apply {
            setContent(composition) {
                Box(
                    modifier = Modifier.semantics {
                        dialog()
                        testTagsAsResourceId = true
                    }
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.show()
        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    SideEffect {
        dialog.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            contentColor = contentColor,
            layoutDirection = layoutDirection
        )
    }
}

@Suppress("ViewConstructor")
private class AppBottomSheetDialogLayout(
    context: Context,
    override val window: Window
) : AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}

private class AppBottomSheetDialog(
    private var onDismissRequest: () -> Unit,
    private var properties: AppBottomSheetProperties,
    contentColor: Color,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    windowId: UUID,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope
) : ComponentDialog(ContextThemeWrapper(composeView.context, R.style.AppBottomSheetWindowTheme)), ViewRootForInspector {

    private val dialogLayout: AppBottomSheetDialogLayout

    private val backCallback = PredictiveBackCallback(
        enabled = properties.shouldDismissOnBackPress,
        scope = scope,
        progress = predictiveBackProgress,
        onBack = { this@AppBottomSheetDialog.onDismissRequest() }
    )

    override val subCompositionView: AbstractComposeView
        get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        dialogLayout = AppBottomSheetDialogLayout(context, window).apply {
            setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Dialog:$windowId")
            clipChildren = false
            with(density) { elevation = MaxSupportedElevation.toPx() }
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    result.alpha = 0f
                }
            }
        }
        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        onBackPressedDispatcher.addCallback(this, backCallback)
        updateParameters(onDismissRequest, properties, contentColor, layoutDirection)
    }

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        dialogLayout.setContent(parentComposition, children)
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: AppBottomSheetProperties,
        contentColor: Color,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        backCallback.isEnabled = properties.shouldDismissOnBackPress
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)
        setSystemBarAppearance(contentColor)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window?.setSoftInputMode(
            if (Build.VERSION.SDK_INT >= 30) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
        )
    }

    fun disposeComposition() {
        dialogLayout.disposeComposition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result && properties.shouldDismissOnClickOutside) {
            onDismissRequest()
        }
        return result
    }

    override fun cancel() = Unit

    private fun setLayoutDirection(layoutDirection: LayoutDirection) {
        dialogLayout.layoutDirection = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
    }

    private fun setSecurePolicy(securePolicy: AppBottomSheetSecurePolicy) {
        val secure = when (securePolicy) {
            AppBottomSheetSecurePolicy.SecureOn -> true
            AppBottomSheetSecurePolicy.SecureOff -> false
            AppBottomSheetSecurePolicy.Inherit -> composeView.isFlagSecureEnabled()
        }
        window?.setFlags(
            if (secure) WindowManager.LayoutParams.FLAG_SECURE else WindowManager.LayoutParams.FLAG_SECURE.inv(),
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    private fun setSystemBarAppearance(contentColor: Color) {
        val window = window ?: return
        val hostLightStatusBars = composeView.hostWindow()?.let {
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = hostLightStatusBars ?: contentColor.isDark()
            isAppearanceLightNavigationBars = contentColor.isDark()
        }
    }
}

private class PredictiveBackCallback(
    enabled: Boolean,
    private val scope: CoroutineScope,
    private val progress: Animatable<Float, AnimationVector1D>,
    private val onBack: () -> Unit
) : OnBackPressedCallback(enabled) {

    override fun handleOnBackStarted(backEvent: BackEventCompat) {
        scope.launch { progress.snapTo(PredictiveBackEasing.transform(backEvent.progress)) }
    }

    override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        scope.launch { progress.snapTo(PredictiveBackEasing.transform(backEvent.progress)) }
    }

    override fun handleOnBackPressed() {
        onBack()
    }

    override fun handleOnBackCancelled() {
        scope.launch { progress.animateTo(0f) }
    }
}

private fun View.isFlagSecureEnabled(): Boolean {
    val params = rootView.layoutParams as? WindowManager.LayoutParams ?: return false
    return (params.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
}

private fun View.hostWindow(): Window? =
    generateSequence(parent) { it.parent }.filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
        ?: generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()?.window

private fun Color.isDark(): Boolean = this != Color.Transparent && luminance() <= 0.5f

private val PredictiveBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

private val MaxSupportedElevation = 8.dp
