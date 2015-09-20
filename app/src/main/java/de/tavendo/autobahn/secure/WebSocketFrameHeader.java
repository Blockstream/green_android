package de.tavendo.autobahn.secure;

public class WebSocketFrameHeader {
	private int mOpcode;
	private boolean mFin;
	private int mReserved;
	private int mHeaderLen;
	private int mPayloadLen;
	private int mTotalLen;
	private byte[] mMask;
	
	public int getOpcode() {
		return mOpcode;
	}
	public void setOpcode(int opcode) {
		this.mOpcode = opcode;
	}
	public boolean isFin() {
		return mFin;
	}
	public void setFin(boolean fin) {
		this.mFin = fin;
	}
	public int getReserved() {
		return mReserved;
	}
	public void setReserved(int reserved) {
		this.mReserved = reserved;
	}
	public int getHeaderLength() {
		return mHeaderLen;
	}
	public void setHeaderLength(int headerLength) {
		this.mHeaderLen = headerLength;
	}
	public int getPayloadLength() {
		return mPayloadLen;
	}
	public void setPayloadLength(int payloadLength) {
		this.mPayloadLen = payloadLength;
	}
	public int getTotalLength() {
		return mTotalLen;
	}
	public void setTotalLen(int totalLength) {
		this.mTotalLen = totalLength;
	}
	public byte[] getMask() {
		return mMask;
	}
	public void setMask(byte[] mask) {
		this.mMask = mask;
	}
}