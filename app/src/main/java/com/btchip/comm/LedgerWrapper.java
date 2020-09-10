package com.btchip.comm;

import java.io.ByteArrayOutputStream;

/**
 * Package commands and responses to be sent over the chosen bearer
*/
public class LedgerWrapper {
	private static final int TAG_APDU = 0x05;
	
	/**
	 * Prepare an APDU to be sent over the chosen bearer
	 * @param channel dummy channel to use
	 * @param command APDU to send
	 * @param packetSize maximum size of a packet for this bearer
	 * @param hasChannel set to true if this bearer includes channel information
	 * @return list of packets to be sent over the chosen bearer
	 */
	private static byte[] wrapCommandAPDUInternal(int channel, byte[] command, int packetSize, boolean hasChannel) throws LedgerException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (packetSize < 3) {
			throw new LedgerException(LedgerException.ExceptionReason.INVALID_PARAMETER, "Can't handle Ledger framing with less than 3 bytes for the report");
		}
		int sequenceIdx = 0;
		int offset = 0;
		int headerSize = (hasChannel ? 7 : 5);
		if (hasChannel) {
			output.write(channel >> 8);
			output.write(channel);
		}
		output.write(TAG_APDU);
		output.write(sequenceIdx >> 8);
		output.write(sequenceIdx);
		sequenceIdx++;
		output.write(command.length >> 8);
		output.write(command.length);
		int blockSize = (command.length > packetSize - headerSize ? packetSize - headerSize : command.length);
		output.write(command, offset, blockSize);
		offset += blockSize;
		while (offset != command.length) {
			if (hasChannel) {
				output.write(channel >> 8);
				output.write(channel);
			}
			output.write(TAG_APDU);
			output.write(sequenceIdx >> 8);
			output.write(sequenceIdx);
			sequenceIdx++;
			blockSize = (command.length - offset > packetSize - headerSize + 2 ? packetSize - headerSize + 2 : command.length - offset);
			output.write(command, offset, blockSize);
			offset += blockSize;			
		}
		if ((output.size() % packetSize) != 0) {
			byte[] padding = new byte[packetSize - (output.size() % packetSize)];
			output.write(padding, 0, padding.length);
		}
		return output.toByteArray();		
	}
	
	/**
	 * Reassemble packets received over the chosen bearer
	 * @param channel dummy channel to use
	 * @param data binary data received so far
	 * @param packetSize maximum size of a packet for this bearer
	 * @param hasChannel set to true if this bearer includes channel information
	 */
	private static byte[] unwrapResponseAPDUInternal(int channel, byte[] data, int packetSize, boolean hasChannel) throws LedgerException {
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		int offset = 0;
		int responseLength;
		int sequenceIdx = 0;
		int headerSize = (hasChannel ? 7 : 5);
		if ((data == null) || (data.length < headerSize)) {
			return null;
		}
		if (hasChannel) {
			if (data[offset++] != (channel >> 8)) {
				throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid channel");
			}
			if (data[offset++] != (channel & 0xff)) {
				throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid channel");
			}
		}
		if (data[offset++] != TAG_APDU) {
			throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid tag");			
		}
		if (data[offset++] != 0x00) {
			throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid sequence");
		}
		if (data[offset++] != 0x00) {
			throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid sequence");
		}
		responseLength = ((data[offset++] & 0xff) << 8);
		responseLength |= (data[offset++] & 0xff);
		if (data.length < headerSize + responseLength) {
			return null;
		}
		int blockSize = (responseLength > packetSize - headerSize ? packetSize - headerSize : responseLength);
		response.write(data, offset, blockSize);
		offset += blockSize;
		while (response.size() != responseLength) {
			sequenceIdx++;
			if (offset == data.length) {
				return null;
			}
			if (hasChannel) {
				if (data[offset++] != (channel >> 8)) {
					throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid channel");
				}
				if (data[offset++] != (channel & 0xff)) {
					throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid channel");
				}
			}
			if (data[offset++] != TAG_APDU) {
				throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid tag");			
			}
			if (data[offset++] != (sequenceIdx >> 8)) {
				throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid sequence");
			}
			if (data[offset++] != (sequenceIdx & 0xff)) {
				throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Invalid sequence");
			}
			blockSize = (responseLength - response.size() > packetSize - headerSize + 2 ? packetSize - headerSize + 2 : responseLength - response.size());
			if (blockSize > data.length - offset) {
				return null;
			}
			response.write(data, offset, blockSize);
			offset += blockSize;			
		}
		return response.toByteArray();
	}	

	/**
	 * Prepare an APDU to be sent over the chosen bearer
	 * @param channel dummy channel to use
	 * @param command APDU to send
	 * @param packetSize maximum size of a packet for this bearer
	 * @return list of packets to be sent over the chosen bearer
	 */
	public static byte[] wrapCommandAPDU(int channel, byte[] command, int packetSize) throws LedgerException {
		return wrapCommandAPDUInternal(channel, command, packetSize, true);
	}

	/**
	 * Prepare an APDU to be sent over the chosen bearer
	 * @param command APDU to send
	 * @param packetSize maximum size of a packet for this bearer
	 * @return list of packets to be sent over the chosen bearer
	 */
	public static byte[] wrapCommandAPDU(byte[] command, int packetSize) throws LedgerException {
		return wrapCommandAPDUInternal(0, command, packetSize, false);
	}

	/**
	 * Reassemble packets received over the chosen bearer
	 * @param channel dummy channel to use
	 * @param data binary data received so far
	 * @param packetSize maximum size of a packet for this bearer
	 */
	public static byte[] unwrapResponseAPDU(int channel, byte[] data, int packetSize) throws LedgerException {
		return unwrapResponseAPDUInternal(channel, data, packetSize, true);
	}

	/**
	 * Reassemble packets received over the chosen bearer
	 * @param data binary data received so far
	 * @param packetSize maximum size of a packet for this bearer
	 */
	public static byte[] unwrapResponseAPDU(byte[] data, int packetSize) throws LedgerException {
		return unwrapResponseAPDUInternal(0, data, packetSize, false);
	}

}
