package nordpol;

import java.io.IOException;
import java.util.List;

public interface IsoCard {
    void addOnCardErrorListener(OnCardErrorListener listener);
    void removeOnCardErrorListener(OnCardErrorListener listener);
    void close() throws IOException;
    void connect() throws IOException;
    int getMaxTransceiveLength() throws IOException;
    int getTimeout();
    boolean isConnected();
    void setTimeout(int timeout);
    byte[] transceive(byte[] data) throws IOException;
    List<byte[]> transceive(List<byte[]> data) throws IOException;
}
