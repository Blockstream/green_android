package com.greenaddress.greenbits;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.concurrent.Callable;


public class QrBitmap implements Callable<QrBitmap> {
    public final String data;
    public Bitmap qrcode;
    private final int background_color;

    public QrBitmap(final String data, final int background_color) {
        this.data = data;
        this.background_color = background_color;
    }

    private static Bitmap toBitmap(final BitMatrix matrix, final int qrcode_color, final int background_color) {
        final int height = matrix.getHeight();
        final int width = matrix.getWidth();
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0  ; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                bmp.setPixel(x, y, matrix.get(x, y) ? qrcode_color : background_color);
            }
        }
        return bmp;
    }

    public QrBitmap call() throws WriterException {
        // FIXME: change hardcoded 2048 ?
        this.qrcode = toBitmap(new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 2048, 2048), Color.BLACK, background_color);
        return this;
    }
}