package com.btchip.comm;

import java.io.ByteArrayOutputStream;

/**
 * Utility class to convert between an hexadecimal representation and binary content
 */
public class Dump {

	/**
	 * Convert a binary array to its hexadecimal representation
	 * @param buffer buffer containing the data to convert
	 * @param offset offset to the data to convert
	 * @param length length of the data to convert
	 * @return hexadecimal representation of the data
	 */
	public static String dump(byte[] buffer, int offset, int length) {
		if (buffer == null) {
			return "null";
		}
		String result = "";
		for (int i=0; i<length; i++) {
			String temp = Integer.toHexString((buffer[offset + i]) & 0xff);
			if (temp.length() < 2) {
				temp = "0" + temp;
			}
			result += temp;
		}
		return result;
	}

	/**
	 * Convert a binary array to its hexadecimal representation
	 * @param buffer buffer containing the data to convert
	 */
	public static String dump(byte[] buffer) {
		if (buffer == null) {
			return "null";
		}
		return dump(buffer, 0, buffer.length);
	}

	/*
	 * Convert an hexadecimal representation to a binary array
	 * @param src hexadecimal string to convert
	 */
	public static byte[] hexToBin(String src) {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		int i = 0;
		while (i < src.length()) {
			char x = src.charAt(i);
			if (!((x >= '0' && x <= '9') || (x >= 'A' && x <= 'F') || (x >= 'a' && x <= 'f'))) {
				i++;
				continue;
			}
			try {
				result.write(Integer.valueOf("" + src.charAt(i) + src.charAt(i + 1), 16));
				i += 2;
			}
			catch (Exception e) {
				return null;
			}
		}
		return result.toByteArray();
	}
}
