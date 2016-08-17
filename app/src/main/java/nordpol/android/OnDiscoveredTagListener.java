package nordpol.android;

import android.nfc.Tag;

public interface OnDiscoveredTagListener {
    void tagDiscovered(Tag t);
}
