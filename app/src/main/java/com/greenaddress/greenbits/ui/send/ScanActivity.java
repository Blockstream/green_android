package com.greenaddress.greenbits.ui.send;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SweepData;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.assets.AssetsSelectActivity;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import de.schildbach.wallet.ui.scan.CameraManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.greenaddress.greenapi.Session.getSession;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;

public class ScanActivity extends LoggedActivity implements TextureView.SurfaceTextureListener, View.OnClickListener,
                                  TextWatcher {

    private static final long AUTO_FOCUS_INTERVAL_MS = 2500L;

    private final CameraManager cameraManager = new CameraManager();

    private boolean isSweep;
    private View contentView;
    private de.schildbach.wallet.ui.scan.ScannerView scannerView;
    private TextureView previewView;
    private TextInputEditText mAddressEditText;
    private Disposable disposable;

    private volatile boolean surfaceCreated = false;
    private Animator sceneTransition = null;

    private HandlerThread cameraThread;
    private volatile Handler cameraHandler;

    private static final int DIALOG_CAMERA_PROBLEM = 0;

    private static final Logger log = LoggerFactory.getLogger(ScanActivity.class);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UI.preventScreenshots(this);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));

        isSweep = getIntent().getBooleanExtra(PrefKeys.SWEEP, false);

        if (isSweep)
            setTitle(R.string.id_sweep);

        // Stick to the orientation the activity was started with. We cannot declare this in the
        // AndroidManifest.xml, because it's not allowed in combination with the windowIsTranslucent=true
        // theme attribute.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        setContentView(R.layout.activity_send_scan);
        contentView = findViewById(android.R.id.content);
        scannerView = findViewById(R.id.scan_activity_mask);
        previewView = findViewById(R.id.scan_activity_preview);
        previewView.setSurfaceTextureListener(this);
        cameraThread = new HandlerThread("cameraThread", Process.THREAD_PRIORITY_BACKGROUND);
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);

        mAddressEditText = UI.find(this, R.id.addressEdit);
        mAddressEditText.setHint(
                isSweep ? R.string.id_enter_a_private_key_to_sweep : R.string.id_enter_an_address);

        UI.find(this, R.id.nextButton).setEnabled(false);
        UI.attachHideKeyboardListener(this, findViewById(R.id.activity_send_scan));

        UI.find(this, R.id.copyButton).setOnClickListener(event -> this.copyFromClipboard());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void maybeTriggerSceneTransition() {
        if (sceneTransition != null) {
            contentView.setAlpha(1);
            sceneTransition.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    getWindow()
                            .setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.black)));
                }
            });
            sceneTransition.start();
            sceneTransition = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mAddressEditText.addTextChangedListener(this);
        UI.find(this, R.id.nextButton).setOnClickListener(this);
        maybeOpenCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraHandler.post(closeRunnable);
        mAddressEditText.removeTextChangedListener(this);
        UI.find(this, R.id.nextButton).setOnClickListener(null);
    }

    @Override
    protected void onDestroy() {
        // cancel background thread
        if (cameraHandler != null) {
            cameraHandler.removeCallbacksAndMessages(null);
        }

        if (cameraThread != null) {
            cameraThread.quit();
        }

        if (previewView != null) {
            previewView.setSurfaceTextureListener(null);
        }

        if (disposable != null)
            disposable.dispose();

        // We're removing the requested orientation because if we don't, somehow the requested orientation is
        // bleeding through to the calling activity, forcing it into a locked state until it is restarted.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
                                           final int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            maybeOpenCamera();
        else
            UI.toast(this, R.string.id_please_enable_camera, Toast.LENGTH_LONG);
    }

    private void maybeOpenCamera() {
        if (surfaceCreated && ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED)
            cameraHandler.post(openRunnable);
    }


    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        surfaceCreated = true;
        maybeOpenCamera();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        surfaceCreated = false;
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onBackPressed() {
        scannerView.setVisibility(View.GONE);
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // don't launch camera app
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraHandler.post(() -> cameraManager.setTorch(keyCode == KeyEvent.KEYCODE_VOLUME_UP));
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private final Runnable openRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                final Camera camera = cameraManager.open(previewView, displayRotation());

                final Rect framingRect = cameraManager.getFrame();
                final RectF framingRectInPreview = new RectF(cameraManager.getFramePreview());
                framingRectInPreview.offsetTo(0, 0);
                final boolean cameraFlip = cameraManager.getFacing() == Camera.CameraInfo.CAMERA_FACING_FRONT;
                final int cameraRotation = cameraManager.getOrientation();

                runOnUiThread(() ->
                        scannerView.setFraming(framingRect, framingRectInPreview, displayRotation(),
                                cameraRotation,
                                cameraFlip));

                final String focusMode = camera.getParameters().getFocusMode();
                final boolean nonContinuousAutoFocus = Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)
                        || Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode);

                if (nonContinuousAutoFocus)
                    cameraHandler.post(new AutoFocusRunnable(camera));

                maybeTriggerSceneTransition();
                cameraHandler.post(fetchAndDecodeRunnable);
            } catch (final Exception x) {
                log.info("problem opening camera", x);
                runOnUiThread(() -> showDialog(DIALOG_CAMERA_PROBLEM));
            }
        }

        private int displayRotation() {
            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
            if (rotation == Surface.ROTATION_0)
                return 0;
            else if (rotation == Surface.ROTATION_90)
                return 90;
            else if (rotation == Surface.ROTATION_180)
                return 180;
            else if (rotation == Surface.ROTATION_270)
                return 270;
            else
                throw new IllegalStateException("rotation: " + rotation);
        }
    };

    private final Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            cameraHandler.removeCallbacksAndMessages(null);
            cameraManager.close();
        }
    };

    @Override
    public void beforeTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {
    }

    @Override
    public void onTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        UI.enableIf(editable.length() > 0, UI.find(this, R.id.nextButton));
        UI.enableIf(isClipboardEmpty(), UI.find(this, R.id.copyButton));
        UI.enableIf(editable.length() == 0, UI.find(this, R.id.copyButton));
    }

    private void copyFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            Log.d(TAG, "****** Clipboard content: " + clipboard.getPrimaryClip().getItemAt(0).getText());
            mAddressEditText.setText(clipboard.getPrimaryClip().getItemAt(0).getText());
            this.onClick(this.contentView);
        }
    }

    private boolean isClipboardEmpty(){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboard.hasPrimaryClip();
    }

    private final class AutoFocusRunnable implements Runnable {
        private final Camera camera;

        AutoFocusRunnable(final Camera camera) {
            this.camera = camera;
        }

        @Override
        public void run() {
            try {
                camera.autoFocus(autoFocusCallback);
            } catch (final Exception x) {
                log.info("problem with auto-focus, will not schedule again", x);
            }
        }

        private final Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(final boolean success, final Camera camera) {
                // schedule again
                cameraHandler.postDelayed(AutoFocusRunnable.this, AUTO_FOCUS_INTERVAL_MS);
            }
        };
    }

    public void handleResult(final Result scanResult, final Bitmap thumbnailImage, final float thumbnailScaleFactor) {
        // vibrator.vibrate(VIBRATE_DURATION);
        //scannerView.drawResultBitmap(thumbnailImage);

        // superimpose dots to highlight the key features of the qr code
        final ResultPoint[] points = scanResult.getResultPoints();
        if (points != null && points.length > 0) {
            final Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.scan_result_dots));
            paint.setStrokeWidth(10.0f);

            final Canvas canvas = new Canvas(thumbnailImage);
            canvas.scale(thumbnailScaleFactor, thumbnailScaleFactor);
            for (final ResultPoint point : points)
                canvas.drawPoint(point.getX(), point.getY(), paint);
        }

        scannerView.setIsResult(true);
        onInserted(scanResult.getText());
    }

    private void onSweep(final String scanned) {
        final Integer subaccount = getActiveAccount();
        final Intent result = new Intent();
        result.putExtra("internal_qr", true);
        result.putExtra(PrefKeys.SWEEP, true);

        disposable = Observable.just(getSession())
                .observeOn(Schedulers.computation())
                .map((session) -> {
                    final ObjectNode jsonResp = session.getReceiveAddress(subaccount).resolve(null,
                            new HardwareCodeResolver(
                                    this));
                    return jsonResp.get("address").asText();
                })
                .map((address) -> {
                    final Long feeRate = getSession().getFees().get(0);
                    final BalanceData balanceData = new BalanceData();
                    balanceData.setAddress(address);
                    final List<BalanceData> balanceDataList = new ArrayList<>();
                    balanceDataList.add(balanceData);
                    SweepData sweepData = new SweepData();
                    sweepData.setPrivateKey(scanned);
                    sweepData.setFeeRate(feeRate);
                    sweepData.setAddressees(balanceDataList);
                    sweepData.setSubaccount(subaccount);
                    return sweepData;
                })
                .map((sweepData) -> {
                    final GDKTwoFactorCall call = getSession().createTransactionRaw(null, sweepData);
                    final ObjectNode transactionRaw = call.resolve(null, new HardwareCodeResolver(this));
                    final String error = transactionRaw.get("error").asText();
                    if (!error.isEmpty())
                        throw new Exception(error);
                    return transactionRaw;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((transactionRaw) -> {
                    removeUtxosIfTooBig(transactionRaw);
                    result.putExtra(PrefKeys.INTENT_STRING_TX, transactionRaw.toString());
                    result.setClass(this, SendAmountActivity.class);
                    startActivityForResult(result, REQUEST_BITCOIN_URL_SEND);
                }, (e) -> {
                    e.printStackTrace();
                    UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
                    cameraHandler.post(fetchAndDecodeRunnable);
                });
    }

    private void onTransaction(final String scanned) {
        final Integer subaccount = getActiveAccount();
        final NetworkData networkData = getNetwork();
        final Intent result = new Intent();
        result.putExtra("internal_qr", true);

        disposable = Observable.just(getSession())
                .observeOn(Schedulers.computation())
                .map((session) -> {
                    final GDKTwoFactorCall call = getSession().createTransactionFromUri(null, scanned, subaccount);
                    final ObjectNode transactionRaw = call.resolve(null, new HardwareCodeResolver(this));
                    if (!transactionRaw.has("addressees"))
                        throw new Exception("Missing field addressees");
                    final String error = transactionRaw.get("error").asText();
                    if (!error.isEmpty() && !"id_invalid_amount".equals(error))
                        throw new Exception(error);
                    return transactionRaw;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((transactionRaw) -> {
                    removeUtxosIfTooBig(transactionRaw);
                    final boolean showAssets = !transactionRaw.get("addressees").get(0).has("asset_tag");
                    if (networkData.getLiquid() && showAssets)
                        result.setClass(this, AssetsSelectActivity.class);
                    else
                        result.setClass(this, SendAmountActivity.class);
                    result.putExtra(PrefKeys.INTENT_STRING_TX, transactionRaw.toString());
                    startActivityForResult(result, REQUEST_BITCOIN_URL_SEND);
                }, (e) -> {
                    e.printStackTrace();
                    UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
                    cameraHandler.post(fetchAndDecodeRunnable);
                });
    }

    public void onInserted(final String scanned) {
        if (isSweep)
            onSweep(scanned);
        else
            onTransaction(scanned);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BITCOIN_URL_SEND && resultCode == RESULT_OK)
            finish();
    }

    @Override
    public void onClick(final View view) {
        onInserted(mAddressEditText.getText() == null ? "" : mAddressEditText.getText().toString());
    }


    private final Runnable fetchAndDecodeRunnable = new Runnable() {
        private final QRCodeReader reader = new QRCodeReader();
        private final Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);

        @Override
        public void run() {
            cameraManager.requestPreviewFrame((data, camera) -> decode(data));
        }

        private void decode(final byte[] data) {
            final PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(data);
            final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK,
                        (ResultPointCallback) dot -> runOnUiThread(() -> scannerView.addDot(dot)));
                final Result scanResult = reader.decode(bitmap, hints);

                final int thumbnailWidth = source.getThumbnailWidth();
                final int thumbnailHeight = source.getThumbnailHeight();
                final float thumbnailScaleFactor = (float) thumbnailWidth / source.getWidth();

                final Bitmap thumbnailImage = Bitmap.createBitmap(thumbnailWidth, thumbnailHeight,
                        Bitmap.Config.ARGB_8888);
                thumbnailImage.setPixels(
                        source.renderThumbnail(), 0, thumbnailWidth, 0, 0, thumbnailWidth, thumbnailHeight);

                runOnUiThread(() -> handleResult(scanResult, thumbnailImage, thumbnailScaleFactor));
            } catch (final ReaderException x) {
                // retry
                cameraHandler.post(fetchAndDecodeRunnable);
            } finally {
                reader.reset();
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(final int id) {
        if (id == DIALOG_CAMERA_PROBLEM) {
            MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                    .title(getResources().getString(R.string.id_camera_problem))
                    .content(getResources().getString(R.string.id_the_camera_has_a_problem_you))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            finish();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                            finish();
                        }

                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            super.onNeutral(dialog);
                            finish();
                        }
                    });
            return builder.build();
        } else {
            throw new IllegalArgumentException();
        }
    }
}
