package com.greenaddress.greenbits.spv;

import android.util.Patterns;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public class Socks5SocketFactory extends SocketFactory {
    private final InetAddress mProxyHost;
    private final int mProxyPort;

    public Socks5SocketFactory(final String host, final String port) throws UnknownHostException {
        mProxyHost = InetAddress.getByName(host);
        mProxyPort = Integer.parseInt(port);
    }

    @Override
    public Socket createSocket() {
        return new Socks5Socket();
    }

    @Override
    public Socket createSocket(final InetAddress host, final int port) throws IOException {
        return createSocketImpl(new InetSocketAddress(host, port));
    }

    @Override
    public Socket createSocket(final InetAddress host, final int port,
                                        final InetAddress localHost, final int localPort) throws IOException {
        return createSocketImpl(new InetSocketAddress(host, port), localHost, localPort);
    }

    @Override
    public Socket createSocket(final String host, final int port) throws IOException {
        return createSocketImpl(new InetSocketAddress(host, port));
    }

    @Override
    public Socket createSocket(final String host, final int port,
                                        final InetAddress localHost, final int localPort) throws IOException {
        return createSocketImpl(new InetSocketAddress(host, port), localHost, localPort);
    }

    private Socket createSocketImpl(final InetSocketAddress host, final InetAddress localHost, final int localPort) throws IOException {
        final Socket s = createSocket();
        if (localHost != null)
            s.bind(new InetSocketAddress(localHost, localPort));
        s.connect(host);
        return s;
    }

    private Socket createSocketImpl(final InetSocketAddress host) throws IOException {
        return createSocketImpl(host, null, 0);
    }

    private class Socks5Socket extends Socket {

        static final int CMD_CONNECT = 0x1;
        static final int ATYP_IPV4 = 0x1;
        static final int ATYP_DOMAINNAME = 0x3;
        static final int ATYP_IPV6 = 0x4;

        String mLocalHost;
        InetAddress mLocalIP;
        Proxied mProxied;
        InetSocketAddress mTarget;

        class Proxied {

            InetAddress mIp;
            int mPort;
            String mHost;

            Proxied(final InputStream in) throws IOException {
                final DataInputStream di = new DataInputStream(in);

                di.readUnsignedByte();
                if (di.readUnsignedByte() != 0)
                    throw new IOException();

                di.readUnsignedByte();
                final int type = di.readUnsignedByte();
                final byte[] h;
                switch (type) {
                    case ATYP_IPV4:
                        h = new byte[4];
                        di.readFully(h);
                        mHost = String.format("%s.%s.%s.%s", h[0] & 0xFF, h[1] & 0xFF, h[2] & 0xFF, h[3] & 0xFF);
                        break;
                    case ATYP_IPV6:
                        // broken on Android?
                        throw new IOException();
                    case ATYP_DOMAINNAME:
                        h = new byte[di.readUnsignedByte()];
                        di.readFully(h);
                        mHost = new String(h);
                        break;
                    default:
                        throw new IOException();
                }

                mPort = di.readUnsignedShort();

                if ((type != ATYP_DOMAINNAME))
                    try {
                        mIp = InetAddress.getByName(mHost);
                    } catch (final UnknownHostException uh_ex) {
                    }
            }
        }

        byte[] getConnectCmd() {
            final byte host[] = mTarget.getHostName().getBytes();
            final byte[] data = new byte[7 + host.length];
            data[0] = (byte) 5;
            data[1] = (byte) CMD_CONNECT;
            data[2] = (byte) 0;
            data[3] = (byte) ATYP_DOMAINNAME;
            data[4] = (byte) host.length;
            System.arraycopy(host, 0, data, 5, host.length);
            data[data.length - 2] = (byte) (mTarget.getPort() >> 8);
            data[data.length - 1] = (byte) (mTarget.getPort());
            return data;
        }

        void sendConnectCommand() throws IOException {

            final InputStream in = getInputStream();
            final OutputStream out = getOutputStream();

            try {
                out.write(new byte[]{5, 1, 0});
                out.flush();

                final int version = in.read();
                final int method = in.read();

                if ((version < 0) || method != 0) {
                    close();
                    throw new IOException();
                }
            } catch (final UnknownHostException | SocketException uh_ex) {
                close();
                throw new IOException(uh_ex);
            }
            out.write(getConnectCmd());
        }

        @Override
        public void connect(final SocketAddress sa, final int a) throws IOException {
            mTarget = (InetSocketAddress) sa;
            if (isLocal()) // ignore crazy timeout
                super.connect(new InetSocketAddress(getDirectInetAddress(), mTarget.getPort()), 1000);
            else
                connectViaProxy(a);
        }

        void connectViaProxy(final int a) throws IOException {
            super.connect(new InetSocketAddress(mProxyHost, mProxyPort), a);
            sendConnectCommand();
            mProxied = new Proxied(getInputStream());
            if (!mProxied.mHost.equals("0.0.0.0")) {
                mLocalHost = mProxied.mHost;
                mLocalIP = mProxied.mIp;
            } else {
                mLocalIP = mProxyHost;
                mLocalHost = mLocalIP.getHostName();
            }
        }

        InetAddress getDirectInetAddress() throws IOException {
            if (mTarget != null) {
                final String hn = mTarget.getHostName();
                if (hn != null && Patterns.IP_ADDRESS.matcher(hn).matches())
                    return InetAddress.getByName(hn);
            }
            throw new IOException();
        }

        boolean isLocal() {
            try {
                final InetAddress address = getDirectInetAddress();
                return address != null && address.isSiteLocalAddress();
            } catch (final IOException e) {
                // ignore, not an IP
                return false;
            }
        }

        @Override
        public int getLocalPort() {
            return isLocal() ? super.getLocalPort() : mProxied.mPort;
        }

        @Override
        public InetAddress getLocalAddress() {
            if (isLocal())
                return super.getLocalAddress();
            if (mLocalIP == null)
                try {
                    mLocalIP = InetAddress.getByName(mLocalHost);
                } catch (final UnknownHostException e) {
                }
            return mLocalIP;
        }
    }
}
