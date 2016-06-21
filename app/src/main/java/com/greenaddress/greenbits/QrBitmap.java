package com.greenaddress.greenbits;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

public class QrBitmap implements Parcelable {
    public static final Parcelable.Creator<QrBitmap> CREATOR
            = new Parcelable.Creator<QrBitmap>() {
        public QrBitmap createFromParcel(final Parcel in) {
            return new QrBitmap(in);
        }

        public QrBitmap[] newArray(final int size) {
            return new QrBitmap[size];
        }
    };
    public final String data;
    private final int background_color;
    private Bitmap qrcode;

    private QrBitmap(final Parcel in) {
        data = in.readString();
        background_color = in.readInt();
        qrcode = null;
    }

    public QrBitmap(final String data, final int background_color) {
        this.data = data;
        this.background_color = background_color;
        qrcode = null;
    }

    private static Bitmap toBitmap(final QRCode code, final int background_color) {
        final ByteMatrix matrix = code.getMatrix();
        final int SCALE = 4;
        final int height = matrix.getHeight() * SCALE;
        final int width = matrix.getWidth() * SCALE;
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                bmp.setPixel(x, y, matrix.get(x / SCALE, y / SCALE) == 1 ? Color.BLACK : background_color);
            }
        }
        return bmp;
    }

    public Bitmap getQRCode() {
        if (this.qrcode == null) {
            try {
                QRCode code = Encoder.encode(data, ErrorCorrectionLevel.M);
                this.qrcode = toBitmap(code, background_color);
            } catch (WriterException e) {
                throw new RuntimeException(e);
            }
        }
        return this.qrcode;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(data);
        dest.writeInt(background_color);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
