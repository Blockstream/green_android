package com.greenaddress.greenbits;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;

public class QrBitmap implements Parcelable {
    private static final int SCALE = 4;

    private final String mData;
    private final int mBackgroundColor;
    private Bitmap mQRCode;

    public QrBitmap(final String data, final int backgroundColor) {
        mData = data;
        mBackgroundColor = backgroundColor;
    }

    public String getData() {
        return mData;
    }

    public Bitmap getQRCode() {
        if (mQRCode == null) {
            final ByteMatrix matrix;
            try {
                matrix = Encoder.encode(mData, ErrorCorrectionLevel.M).getMatrix();
            } catch (final WriterException e) {
                throw new RuntimeException(e);
            }
            final int height = matrix.getHeight() * SCALE;
            final int width = matrix.getWidth() * SCALE;
            mQRCode = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; ++x)
                for (int y = 0; y < height; ++y)
                    mQRCode.setPixel(x, y, matrix.get(x / SCALE, y / SCALE) == 1 ? Color.BLACK : mBackgroundColor);
        }
        return mQRCode;
    }

    // Parcelable support

    private QrBitmap(final Parcel in) {
        mData = in.readString();
        mBackgroundColor = in.readInt();
    }

    public static final Parcelable.Creator<QrBitmap> CREATOR
            = new Parcelable.Creator<QrBitmap>() {
        public QrBitmap createFromParcel(final Parcel in) {
            return new QrBitmap(in);
        }

        public QrBitmap[] newArray(final int size) {
            return new QrBitmap[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mData);
        dest.writeInt(mBackgroundColor);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
