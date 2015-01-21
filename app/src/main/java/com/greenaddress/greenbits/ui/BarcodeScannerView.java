package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import me.dm7.barcodescanner.core.CameraPreview;
import me.dm7.barcodescanner.core.DisplayUtils;
import me.dm7.barcodescanner.core.ViewFinderView;

public abstract class BarcodeScannerView extends FrameLayout implements Camera.PreviewCallback  {
    private Camera mCamera;
    private CameraPreview mPreview;
    private ViewFinderView mViewFinderView;
    private Rect mFramingRectInPreview;

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            if (c == null) {
                final int cameras = Camera.getNumberOfCameras();
                if (cameras > 0) {
                    // open front camera instead (open() only returns back cameras)
                    c = Camera.open(0);
                }
            }
        }
        catch (final Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public BarcodeScannerView(final Context context) {
        super(context);
        setupLayout();
    }

    public BarcodeScannerView(final Context context, final AttributeSet attributeSet) {
        super(context, attributeSet);
        setupLayout();
    }

    public void setupLayout() {
        mPreview = new CameraPreview(getContext());
        mViewFinderView = new ViewFinderView(getContext());
        addView(mPreview);
        addView(mViewFinderView);
    }

    public void startCamera() {
        mCamera = getCameraInstance();
        if(mCamera != null) {
            mViewFinderView.setupViewFinder();
            mPreview.setCamera(mCamera, this);
            mPreview.initCameraPreview();
        }
    }

    public void stopCamera() {
        if(mCamera != null) {
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
            mCamera.release();
            mCamera = null;
        }
    }

    public synchronized Rect getFramingRectInPreview(final int width, final int height) {
        if (mFramingRectInPreview == null) {
            final Rect framingRect = mViewFinderView.getFramingRect();
            if (framingRect == null) {
                return null;
            }
            final Rect rect = new Rect(framingRect);
            final Point screenResolution = DisplayUtils.getScreenResolution(getContext());
            final Point cameraResolution = new Point(width, height);

            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;

            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }
}
