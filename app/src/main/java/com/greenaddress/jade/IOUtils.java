/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */

package com.greenaddress.jade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, Long.MAX_VALUE);
    }

    public static long copy(InputStream in, OutputStream out, long length) throws IOException {
        try {
            long copied = 0;
            int len = (int) Math.min(length, 4 * 1024);
            byte[] buffer = new byte[len];
            while (length > 0) {
                len = in.read(buffer, 0, len);
                if (len < 0) {
                    break;
                }
                if (out != null) {
                    out.write(buffer, 0, len);
                }
                copied += len;
                length -= len;
                len = (int) Math.min(length, 4 * 1024);
            }
            return copied;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
