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

import java.util.concurrent.Callable;


public class QrBitmap implements Callable<QrBitmap>, Parcelable {
    public final String data;
    public Bitmap qrcode;
    private final int background_color;

    public QrBitmap(Parcel in) {
        data = in.readString();
        background_color = in.readInt();
        qrcode = in.readParcelable(null);
    }

    public QrBitmap(final String data, final int background_color) {
        this.data = data;
        this.background_color = background_color;
    }

    private static Bitmap toBitmap(final QRCode code, final int qrcode_color, final int background_color) {
        final ByteMatrix matrix = code.getMatrix();
        final int SCALE = 4;
        final int height = matrix.getHeight() * SCALE;
        final int width = matrix.getWidth() * SCALE;
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                bmp.setPixel(x, y, matrix.get(x / SCALE, y / SCALE) == 1 ? qrcode_color : background_color);
            }
        }
        return bmp;
    }

    public QrBitmap call() throws WriterException {
        QRCode code = Encoder.encode(data, ErrorCorrectionLevel.M);
        this.qrcode = toBitmap(code, Color.BLACK, background_color);
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
        dest.writeInt(background_color);
        dest.writeParcelable(qrcode, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<QrBitmap> CREATOR
            = new Parcelable.Creator<QrBitmap>() {
        public QrBitmap createFromParcel(Parcel in) {
            return new QrBitmap(in);
        }

        public QrBitmap[] newArray(int size) {
            return new QrBitmap[size];
        }
    };
}