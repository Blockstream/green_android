/*
*******************************************************************************
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.btchip;

import com.blockstream.libwally.Wally;
import com.btchip.comm.BTChipTransport;
import com.btchip.utils.BufferUtils;
import com.btchip.utils.CoinFormatUtils;
import com.btchip.utils.Dump;
import com.btchip.utils.VarintUtils;
import com.google.common.primitives.Longs;
import com.greenaddress.greenapi.data.InputOutputData;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BTChipDongle implements BTChipConstants {

	private BTChipFirmware firmwareVersion;
	private BTChipApplication application;

	public enum OperationMode {
		WALLET(0x01),
		RELAXED_WALLET(0x02),
		SERVER(0x04),
		DEVELOPER(0x08);

		private int value;

		OperationMode(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum Feature {
		UNCOMPRESSED_KEYS(0x01),
		RFC6979(0x02),
		FREE_SIGHASHTYPE(0x04),
		NO_2FA_P2SH(0x08);

		private int value;

		Feature(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum UserConfirmation {
		NONE(0x00),
		KEYBOARD(0x01),
		KEYCARD_DEPRECATED(0x02),
		KEYCARD_SCREEN(0x03),
		KEYCARD(0x04),
		KEYCARD_NFC(0x05);

		private int value;

		UserConfirmation(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public class BTChipPublicKey {
		private byte[] publicKey;
		private String address;
		private byte[] chainCode;

		public BTChipPublicKey(byte[] publicKey, String address, byte[] chainCode) {
			this.publicKey = publicKey;
			this.address = address;
			this.chainCode = chainCode;
		}

		public byte[] getPublicKey() {
			return publicKey;
		}
		public String getAddress() {
			return address;
		}
		public byte[] getChainCode() {
			return chainCode;
		}

		@Override
		public String toString() {
			return String.format("Address %s public key %s chaincode %s", address, Dump.dump(publicKey), Dump.dump(chainCode));
		}
	}

	public class BTChipSignature {
		private byte[] signature;
		private int yParity;

		public BTChipSignature(byte[] signature, int yParity) {
			this.signature = signature;
			this.yParity = yParity;
		}

		public byte[] getSignature() {
			return signature;
		}
		public int getYParity() {
			return yParity;
		}

		@Override
		public String toString() {
			return String.format("Signature %s y parity %s", Dump.dump(signature), yParity);
		}
	}

	public class BTChipFirmware {

		private int features;
		private int architecture;
		private int major;
		private int minor;
		private int patch;

		public BTChipFirmware(int features, int architecture, int major, int minor, int patch) {
			this.features = features;
			this.architecture = architecture;
			this.major = major;
			this.minor = minor;
			this.patch = patch;
		}

		public int getFeatures() {
			return features;
		}
		public int getArchitecture() {
			return architecture;
		}
		public int getMajor() {
			return major;
		}
		public int getMinor() {
			return minor;
		}
		public int getPatch() {
			return patch;
		}

		public boolean compressedKeys() {
			return (features & 0x1) == 0x1;
		}

		@Override
		public String toString() {
			return String.format("%s.%s.%s  (architecture 0x%x, features 0x%x)", major, minor, patch, architecture, features);
		}
	}

	public class BTChipApplication {
		private String name;
		private String version;

		public BTChipApplication(String name, String version) {
			this.name = name;
			this.version = version;
		}

		public String getName() { return name; }
		public String getVersion() { return version; }

		@Override
		public String toString() {
			return name + " " + version;
		}
	}

	public class BTChipInput {
		private byte[] value;
		private byte[] sequence;
		private boolean trusted;
		private boolean segwit;

		public BTChipInput(byte[] value, byte[] sequence, boolean trusted, boolean segwit) {
			this.value = value;
			this.sequence = sequence;
			this.trusted = trusted;
			this.segwit = segwit;
		}

		public byte[] getValue() {
			return value;
		}
		public boolean isTrusted() {
			return trusted;
		}
		public byte[] getSequence() {
			return sequence;
		}
		public boolean isSegwit() {
			return segwit;
		}

		@Override
		public String toString() {
			return String.format("Value %s trusted %b sequence %s segwit %b", Dump.dump(value), trusted, (sequence != null ? Dump.dump(sequence) : ""), segwit);
		}
	}

	public class BTChipLiquidInput {
		private byte[] value;
		private byte[] sequence;

		public BTChipLiquidInput(byte[] value, byte[] sequence) {
			this.value = value;
			this.sequence = sequence;
		}

		public byte[] getValue() {
			return value;
		}
		public byte[] getSequence() {
			return sequence;
		}

		@Override
		public String toString() {
			return String.format("Liquid Value %s sequence %s", Dump.dump(value), (sequence != null ? Dump.dump(sequence) : ""));
		}
	}

	public class BTChipLiquidTrustedCommitments {
		private byte[] value;

		public BTChipLiquidTrustedCommitments(byte[] value) {
			this.value = value;
		}

		public byte[] getValue() {
			return value;
		}

		public byte[] getDataForFinalize() {
			return Arrays.copyOfRange(value, 64, 207);
		}

		public byte[] getAbf() {
			return Arrays.copyOfRange(value, 0, 32);
		}

		public byte[] getVbf() {
			return Arrays.copyOfRange(value, 32, 64);
		}

		public byte[] getAssetCommitment() {
			return Arrays.copyOfRange(value, 69, 69 + 33);
		}

		public byte[] getValueCommitment() {
			return Arrays.copyOfRange(value, 69 + 33, 69 + 33 + 33);
		}

		public byte[] getFlags() {
			return Arrays.copyOfRange(value, 64, 65);
		}
	}

	public class BTChipOutput {
		private byte[] value;
		private UserConfirmation userConfirmation;

		public BTChipOutput(byte[] value, UserConfirmation userConfirmation) {
			this.value = value;
			this.userConfirmation = userConfirmation;
		}

		public byte[] getValue() {
			return value;
		}
		public boolean isConfirmationNeeded() {
			return (!userConfirmation.equals(UserConfirmation.NONE));
		}
		public UserConfirmation getUserConfirmation() {
			return userConfirmation;
		}

		@Override
		public String toString() {
			return String.format("Value %s confirmation type %s", Dump.dump(value), userConfirmation);
		}
	}

	public class BTChipOutputKeycard extends BTChipOutput {
		private byte[] keycardIndexes;

		public BTChipOutputKeycard(byte[] value, UserConfirmation userConfirmation, byte[] keycardIndexes) {
			super(value, userConfirmation);
			this.keycardIndexes = keycardIndexes;
		}

		public byte[] getKeycardIndexes() {
			return keycardIndexes;
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(super.toString());
			buffer.append(" address indexes ");
			for (int i=0; i<keycardIndexes.length; i++) {
				buffer.append(i).append(" ");
			}
			return buffer.toString();
		}
	}

	public class BTChipOutputKeycardScreen extends BTChipOutputKeycard {
		private byte[] screenInfo;

		public BTChipOutputKeycardScreen(byte[] value, UserConfirmation userConfirmation, byte[] keycardIndexes, byte[] screenInfo) {
			super(value, userConfirmation, keycardIndexes);
			this.screenInfo = screenInfo;
		}

		public byte[] getScreenInfo() {
			return screenInfo;
		}

		@Override
		public String toString() {
			return String.format("%s screen data %s", super.toString(), Dump.dump(screenInfo));
		}
	}

	private BTChipTransport transport;
	private int lastSW;
	private boolean supportScreen;

	private static final int OK[] = { SW_OK };
	private static final int OK_OR_NOT_SUPPORTED[] = { SW_OK, SW_INS_NOT_SUPPORTED };
	private static final byte DUMMY[] = { 0 };

	public BTChipDongle(BTChipTransport transport, boolean supportScreen) {
		this.transport = transport;
		this.supportScreen = supportScreen;
	}

	public boolean supportScreen() {
		return supportScreen;
	}

	public BTChipTransport getTransport() {
		return transport;
	}

	public void setTransport(BTChipTransport transport) {
		this.transport = transport;
	}

	private byte[] exchange(byte[] apdu) throws BTChipException {
		byte[] response;
		try {
			response = transport.exchange(apdu).get();
		}
		catch(Exception e) {
			throw new BTChipException("I/O error", e);
		}
		if (response.length < 2) {
			throw new BTChipException("Truncated response");
		}
		lastSW = ((response[response.length - 2] & 0xff) << 8) |
				response[response.length - 1] & 0xff;
		byte[] result = new byte[response.length - 2];
		System.arraycopy(response, 0, result, 0, response.length - 2);
		return result;
	}

	private byte[] exchangeCheck(byte[] apdu, int acceptedSW[]) throws BTChipException {
		byte[] response = exchange(apdu);
		if (acceptedSW == null) {
			return response;
		}
		for (int SW : acceptedSW) {
			if (lastSW == SW) {
				return response;
			}
		}
		throw new BTChipException("Invalid status", lastSW);
	}

	private byte[] exchangeApdu(byte cla, byte ins, byte p1, byte p2, byte[] data, int acceptedSW[]) throws BTChipException {
		byte[] apdu = new byte[data.length + 5];
		apdu[0] = cla;
		apdu[1] = ins;
		apdu[2] = p1;
		apdu[3] = p2;
		apdu[4] = (byte)(data.length);
		System.arraycopy(data, 0, apdu, 5, data.length);
		return exchangeCheck(apdu, acceptedSW);
	}

	private byte[] exchangeApdu(byte cla, byte ins, byte p1, byte p2, int length, int acceptedSW[]) throws BTChipException {
		byte[] apdu = new byte[length != 0 ? 5 : 4];
		apdu[0] = cla;
		apdu[1] = ins;
		apdu[2] = p1;
		apdu[3] = p2;
		if (length != 0) {
			apdu[4] = (byte)(length);
		}
		return exchangeCheck(apdu, acceptedSW);
	}

	private byte[] exchangeApduSplit(byte cla, byte ins, byte p1, byte p2, byte[] data, int acceptedSW[]) throws BTChipException {
		int offset = 0;
		byte[] result = null;
		while (offset < data.length) {
			int blockLength = ((data.length - offset) > 255 ? 255 : data.length - offset);
			byte[] apdu = new byte[blockLength + 5];
			apdu[0] = cla;
			apdu[1] = ins;
			apdu[2] = p1;
			apdu[3] = p2;
			apdu[4] = (byte)(blockLength);
			System.arraycopy(data, offset, apdu, 5, blockLength);
			result = exchangeCheck(apdu, acceptedSW);
			offset += blockLength;
		}
		return result;
	}

	private byte[] exchangeApduSplit2(byte cla, byte ins, byte p1, byte p2, byte[] data, byte[] data2, int acceptedSW[]) throws BTChipException {
		// If data is empty, just send data2 immediately
		if (data.length == 0) {
			return exchangeApdu(cla, ins, p1, p2, data2, acceptedSW);
		}

		// Send data potentially in chunks, appending data2 to the last one
		int offset = 0;
		byte[] result = null;
		int maxBlockSize = 255 - data2.length;
		while (offset < data.length) {
			int blockLength = ((data.length - offset) > maxBlockSize ? maxBlockSize : data.length - offset);
			boolean lastBlock = ((offset + blockLength) == data.length);
			byte[] apdu = new byte[blockLength + 5 + (lastBlock ? data2.length : 0)];
			apdu[0] = cla;
			apdu[1] = ins;
			apdu[2] = p1;
			apdu[3] = p2;
			apdu[4] = (byte)(blockLength + (lastBlock ? data2.length : 0));
			System.arraycopy(data, offset, apdu, 5, blockLength);
			if (lastBlock) {
				System.arraycopy(data2, 0, apdu, 5 + blockLength, data2.length);
			}
			result = exchangeCheck(apdu, acceptedSW);
			offset += blockLength;
		}
		return result;
	}

	public void verifyPin(byte[] pin) throws BTChipException {
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_VERIFY_PIN, (byte)0x00, (byte)0x00, pin, OK);
	}

	public int getVerifyPinRemainingAttempts() throws BTChipException {
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_VERIFY_PIN, (byte)0x80, (byte)0x00, DUMMY, null);
		if ((lastSW & 0xfff0) != 0x63c0) {
			throw new BTChipException("Invalid status", lastSW);
		}
		return (lastSW - 0x63c0);
	}

	public BTChipPublicKey getWalletPublicKey(final List<Integer> path) throws BTChipException {
		byte data[] = pathToByteArray(path);
		byte response[] = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_WALLET_PUBLIC_KEY, (byte)0x00, (byte)0x00, data, OK);
		int offset = 0;
		byte publicKey[] = new byte[response[offset]];
		offset++;
		System.arraycopy(response, offset, publicKey, 0, publicKey.length);
		offset += publicKey.length;
		byte address[] = new byte[response[offset]];
		offset++;
		System.arraycopy(response, offset, address, 0, address.length);
		offset += address.length;
		byte chainCode[] = new byte[32];
		System.arraycopy(response, offset, chainCode, 0, chainCode.length);
		return new BTChipPublicKey(publicKey, new String(address), chainCode);
	}

	public BTChipPublicKey getBlindingKey(final byte[] script) throws BTChipException {
		byte response[] = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_LIQUID_BLINDING_KEY, (byte)0x00, (byte)0x00, script, OK);
		byte blindingKey[] = new byte[65];
		System.arraycopy(response, 0, blindingKey, 0, blindingKey.length);
		return new BTChipPublicKey(blindingKey, null, null);
	}

	// TODO: use an ad-hoc return type
	public BTChipPublicKey getBlindingNonce(final byte[] pubkey, final byte[] script) throws BTChipException {
		byte data[] = new byte[65 + script.length];
		System.arraycopy(pubkey, 0, data, 0, 65);
		System.arraycopy(script, 0, data, 65, script.length);

		byte response[] = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_LIQUID_NONCE, (byte)0x00, (byte)0x00, data, OK);

		byte nonce[] = new byte[32];
		System.arraycopy(response, 0, nonce, 0, nonce.length);

		return new BTChipPublicKey(nonce, null, null);
	}

	public boolean shouldUseTrustedInputForSegwit() {
		try {
			if (this.firmwareVersion == null) this.getFirmwareVersion();
		} catch (BTChipException e) {
			return false;
		}

		// Only applies to Nano S/X
		if (this.firmwareVersion.getArchitecture() != BTCHIP_ARCH_NANO_SX) {
		    return false;
		}

		// True for ver >= 1.4.0
		return this.firmwareVersion.getMajor() > 1 ||
			(this.firmwareVersion.getMajor() == 1 && this.firmwareVersion.getMinor() >= 4);
	}

	public BTChipInput getTrustedInput(BitcoinTransaction transaction, long index, long sequence, boolean segwit) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		// Header
		BufferUtils.writeUint32BE(data, index);
		BufferUtils.writeBuffer(data, transaction.getVersion());
		VarintUtils.write(data, transaction.getInputs().size());
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		// Each input
		for (BitcoinTransaction.BitcoinInput input : transaction.getInputs()) {
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, input.getPrevOut());
			VarintUtils.write(data, input.getScript().length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, input.getScript());
			exchangeApduSplit2(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), input.getSequence(), OK);
		}
		// Number of outputs
		data = new ByteArrayOutputStream();
		VarintUtils.write(data, transaction.getOutputs().size());
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
		// Each output
		for (BitcoinTransaction.BitcoinOutput output : transaction.getOutputs()) {
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, output.getAmount());
			VarintUtils.write(data, output.getScript().length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, output.getScript());
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
		}
		// Locktime
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, transaction.getLockTime(), OK);
		ByteArrayOutputStream sequenceBuf = new ByteArrayOutputStream();
		BufferUtils.writeUint32LE(sequenceBuf, sequence);
		return new BTChipInput(response, sequenceBuf.toByteArray(), true, segwit);
	}

	public BTChipInput createInput(byte[] value, byte[] sequence, boolean trusted, boolean segwit) {
		return new BTChipInput(value, sequence, trusted, segwit);
	}

	public BTChipLiquidInput createLiquidInput(byte[] value, byte[] sequence) {
		return new BTChipLiquidInput(value, sequence);
	}

	public void startUntrustedTransaction(long txVersion, boolean newTransaction, long inputIndex, BTChipInput usedInputList[], byte[] redeemScript, boolean segwit) throws BTChipException {
		// Start building a fake transaction with the passed inputs
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeUint32LE(data, txVersion);
		VarintUtils.write(data, usedInputList.length);
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x00, (newTransaction ? (segwit ? (byte)0x02 : (byte)0x00) : (byte)0x80), data.toByteArray(), OK);
		// Loop for each input
		long currentIndex = 0;
		for (BTChipInput input : usedInputList) {
			byte[] script = (currentIndex == inputIndex ? redeemScript : new byte[0]);
			data = new ByteArrayOutputStream();
			data.write(input.isTrusted() ? (byte)0x01 : input.isSegwit() ? (byte)0x02 : (byte)0x00);
			if (input.isTrusted()) {
				// other inputs have constant length
				data.write(input.getValue().length);
			}
			BufferUtils.writeBuffer(data, input.getValue());
			VarintUtils.write(data, script.length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, script);
			BufferUtils.writeBuffer(data, input.getSequence());
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			currentIndex++;
		}
	}

	public void startUntrustedLiquidTransaction(long txVersion, boolean newTransaction, long inputIndex, BTChipLiquidInput usedInputList[], byte[] redeemScript) throws BTChipException {
		// Start building a fake transaction with the passed inputs
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeUint32LE(data, txVersion);
		VarintUtils.write(data, usedInputList.length);
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x00, (newTransaction ? (byte)0x06 : (byte)0x80), data.toByteArray(), OK);
		// Loop for each input
		long currentIndex = 0;
		for (BTChipLiquidInput input : usedInputList) {
			byte[] script = (currentIndex == inputIndex ? redeemScript : new byte[0]);
			data = new ByteArrayOutputStream();
			data.write((byte)0x03); // Liquid inputW
			BufferUtils.writeBuffer(data, input.getValue());
			VarintUtils.write(data, script.length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, script);
			BufferUtils.writeBuffer(data, input.getSequence());
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			currentIndex++;
		}
	}

	private byte[] foldListOfByteArray(List<byte[]> l) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		for (byte[] arr : l) {
			data.write(arr, 0, arr.length);
		}

		return data.toByteArray();
	}

	public void provideLiquidIssuanceInformation(final int numInputs) throws BTChipException {
		// we can safely assume that we will never sign any issuance here
		ByteArrayOutputStream data = new ByteArrayOutputStream(numInputs);
		for (int i = 0; i < numInputs; i++) {
			data.write(0x00);
		}

		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_LIQUID_ISSUANCE_INFORMATION, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
	}

	public List<BTChipLiquidTrustedCommitments> getLiquidCommitments(List<Long> values, List<byte[]> abfs, List<byte[]> vbfs, final long numInputs, List<InputOutputData> outputData) throws BTChipException {
		ByteArrayOutputStream data;
		List<BTChipLiquidTrustedCommitments> out = new ArrayList<>();

		int i = 0;
		for (InputOutputData output : outputData) {
			// skip the fee output TODO: also skip the voluntarily unblinded outputs (currently unsupported by gdk)
			if (output.getScript().length() == 0) {
				out.add(null);
				i++;
				continue;
			}

			values.add(output.getSatoshi());

			boolean last = false;
			if (i == outputData.size() - 2) {
				last = true;
			}

			data = new ByteArrayOutputStream();
			data.write(output.getAssetIdBytes(), 0, 32);
			BufferUtils.writeUint64BE(data, output.getSatoshi());
			BufferUtils.writeUint32BE(data, i);

			if (last) {
				// get only the abf
				ByteArrayOutputStream getAbfData = new ByteArrayOutputStream(4);
				BufferUtils.writeUint32BE(getAbfData, i);
				byte abfResponse[] = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_LIQUID_BLINDING_FACTOR, (byte)0x01, (byte)0x00, getAbfData.toByteArray(), OK);
				abfs.add(Arrays.copyOfRange(abfResponse, 0, 32));

				// generate the last vbf based on the others
				byte finalVbf[] = Wally.asset_final_vbf(Longs.toArray(values), numInputs, foldListOfByteArray(abfs), foldListOfByteArray(vbfs));
				vbfs.add(finalVbf);
				data.write(finalVbf, 0, 32);
			}

			byte response[] = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_LIQUID_COMMITMENTS, last ? (byte)0x02 : (byte)0x01, (byte)0x00, data.toByteArray(), OK);
			out.add(new BTChipLiquidTrustedCommitments(response));

			if (!last) {
				abfs.add(Arrays.copyOfRange(response, 0, 32));
				vbfs.add(Arrays.copyOfRange(response, 32, 64));
			}

			i++;
		}

		return out;
	}

	private BTChipOutput convertResponseToOutput(byte[] response) throws BTChipException {
		BTChipOutput result = null;
		byte[] value = new byte[response[0] & 0xff];
		System.arraycopy(response, 1, value, 0, value.length);
		byte userConfirmationValue = response[1 + value.length];
		if (userConfirmationValue == UserConfirmation.NONE.getValue()) {
			result = new BTChipOutput(value, UserConfirmation.NONE);
		}
		else
		if (userConfirmationValue == UserConfirmation.KEYBOARD.getValue()) {
			result = new BTChipOutput(value, UserConfirmation.KEYBOARD);
		}
		else
		if (userConfirmationValue == UserConfirmation.KEYCARD_DEPRECATED.getValue()) {
			byte[] keycardIndexes = new byte[response.length - 2 - value.length];
			System.arraycopy(response, 2 + value.length, keycardIndexes, 0, keycardIndexes.length);
			result = new BTChipOutputKeycard(value, UserConfirmation.KEYCARD_DEPRECATED, keycardIndexes);
		}
		else
		if (userConfirmationValue == UserConfirmation.KEYCARD.getValue()) {
			byte keycardIndexesLength = response[2 + value.length];
			byte[] keycardIndexes = new byte[keycardIndexesLength];
			System.arraycopy(response, 3 + value.length, keycardIndexes, 0, keycardIndexes.length);
			result = new BTChipOutputKeycard(value, UserConfirmation.KEYCARD, keycardIndexes);
		}
		else
		if (userConfirmationValue == UserConfirmation.KEYCARD_NFC.getValue()) {
			byte keycardIndexesLength = response[2 + value.length];
			byte[] keycardIndexes = new byte[keycardIndexesLength];
			System.arraycopy(response, 3 + value.length, keycardIndexes, 0, keycardIndexes.length);
			result = new BTChipOutputKeycard(value, UserConfirmation.KEYCARD_NFC, keycardIndexes);
		}
		else
		if (userConfirmationValue == UserConfirmation.KEYCARD_SCREEN.getValue()) {
			byte keycardIndexesLength = response[2 + value.length];
			byte[] keycardIndexes = new byte[keycardIndexesLength];
			byte[] screenInfo = new byte[response.length - 3 - value.length - keycardIndexes.length];
			System.arraycopy(response, 3 + value.length, keycardIndexes, 0, keycardIndexes.length);
			System.arraycopy(response, 3 + value.length + keycardIndexes.length, screenInfo, 0, screenInfo.length);
			result = new BTChipOutputKeycardScreen(value, UserConfirmation.KEYCARD_SCREEN, keycardIndexes, screenInfo);
		}
		if (result == null) {
			throw new BTChipException("Unsupported user confirmation method");
		}
		return result;
	}

	public BTChipOutput finalizeInput(String outputAddress, String amount, String fees, final List<Integer> changePath) throws BTChipException {
		BTChipOutput result;
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = pathToByteArray(changePath);
		data.write(outputAddress.length());
		BufferUtils.writeBuffer(data, outputAddress.getBytes());
		BufferUtils.writeUint64BE(data, CoinFormatUtils.toSatoshi(amount));
		BufferUtils.writeUint64BE(data, CoinFormatUtils.toSatoshi(fees));
		BufferUtils.writeBuffer(data, path);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE, (byte)0x02, (byte)0x00, data.toByteArray(), OK);
		result = convertResponseToOutput(response);
		return result;
	}

	public BTChipOutput finalizeInputFull(byte[] data, final List<Integer> changePath, boolean skipChangeCheck) throws BTChipException {
		BTChipOutput result = null;
		int offset = 0;
		byte[] response = null;
		byte[] path;
		boolean oldAPI = false;
		if (!skipChangeCheck) {
			if (changePath != null) {
				path = pathToByteArray(changePath);
				exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte)0xFF, (byte)0x00, path, null);
				oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2));
			}
			else {
				exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte)0xFF, (byte)0x00, new byte[1], null);
				oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2));
			}
		}
		while (offset < data.length) {
			int blockLength = ((data.length - offset) > 255 ? 255 : data.length - offset);
			byte[] apdu = new byte[blockLength + 5];
			apdu[0] = BTCHIP_CLA;
			apdu[1] = BTCHIP_INS_HASH_INPUT_FINALIZE_FULL;
			apdu[2] = ((offset + blockLength) == data.length ? (byte)0x80 : (byte)0x00);
			apdu[3] = (byte)0x00;
			apdu[4] = (byte)(blockLength);
			System.arraycopy(data, offset, apdu, 5, blockLength);
			response = exchangeCheck(apdu, OK);
			offset += blockLength;
		}
		if (oldAPI) {
			byte value = response[0];
			if (value == UserConfirmation.NONE.getValue()) {
				result = new BTChipOutput(new byte[0], UserConfirmation.NONE);
			}
			else
			if (value == UserConfirmation.KEYBOARD.getValue()) {
				result = new BTChipOutput(new byte[0], UserConfirmation.KEYBOARD);
			}
		}
		else {
			result = convertResponseToOutput(response);
		}
		if (result == null) {
			throw new BTChipException("Unsupported user confirmation method");
		}
		return result;
	}

	public BTChipOutput finalizeInputLiquidFull(List<byte[]> data, final List<Integer> changePath, boolean skipChangeCheck) throws BTChipException {
		BTChipOutput result = null;
		byte[] response = null;

		int count = 0;
		for (byte[] packet : data) {
			int blockLength = packet.length;

			if (blockLength > 255) {
				throw new BTChipException("Block too long");
			}

			byte[] apdu = new byte[blockLength + 5];
			apdu[0] = BTCHIP_CLA;
			apdu[1] = BTCHIP_INS_HASH_INPUT_FINALIZE_FULL;
			apdu[2] = (count == (data.size() - 1) ? (byte)0x80 : (byte)0x00);
			apdu[3] = (byte)0x00;
			apdu[4] = (byte)(blockLength);
			System.arraycopy(packet, 0, apdu, 5, blockLength);
			response = exchangeCheck(apdu, OK);

			count++;
		}

		result = convertResponseToOutput(response);

		if (result == null) {
			throw new BTChipException("Unsupported user confirmation method");
		}
		return result;
	}

	public BTChipOutput finalizeInputFull(byte[] data) throws BTChipException {
		return finalizeInputFull(data, null, false);
	}

	public BTChipOutput finalizeLiquidInputFull(List<byte[]> data) throws BTChipException {
		return finalizeInputLiquidFull(data, null, false);
	}

	public BTChipOutput finalizeInputFull(byte[] data, final List<Integer> changePath) throws BTChipException {
		return finalizeInputFull(data, changePath, false);
	}

	public BTChipOutput finalizeInput(byte[] outputScript, String outputAddress, String amount, String fees, final List<Integer> changePath) throws BTChipException {
		// Try the new API first
		boolean oldAPI;
		byte[] path;
		if (changePath != null) {
			path = pathToByteArray(changePath);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte)0xFF, (byte)0x00, path, null);
			oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2));
		}
		else {
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte)0xFF, (byte)0x00, new byte[1], null);
			oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2));
		}
		if (oldAPI) {
			return finalizeInput(outputAddress, amount, fees, changePath);
		}
		else {
			return finalizeInputFull(outputScript, null, true);
		}
	}

	public byte[] untrustedHashSign(final List<Integer> privateKeyPath, String pin, long lockTime, byte sigHashType) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = pathToByteArray(privateKeyPath);
		BufferUtils.writeBuffer(data, path);
		data.write(pin.length());
		BufferUtils.writeBuffer(data, pin.getBytes());
		BufferUtils.writeUint32BE(data, lockTime);
		data.write(sigHashType);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_SIGN, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		response[0] = (byte)0x30;
		return response;
	}

	public byte[] untrustedLiquidHashSign(final List<Integer> privateKeyPath, long lockTime, byte sigHashType) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = pathToByteArray(privateKeyPath);
		BufferUtils.writeBuffer(data, path);
		//data.write(pin.length());
		data.write((byte)0x00);
		//BufferUtils.writeBuffer(data, pin.getBytes());
		BufferUtils.writeUint32BE(data, lockTime);
		data.write(sigHashType);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_SIGN, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		response[0] = (byte)0x30;
		return response;
	}

	public byte[] untrustedHashSign(final List<Integer> privateKeyPath, String pin) throws BTChipException {
		return untrustedHashSign(privateKeyPath, pin, 0, (byte)0x01);
	}

	public boolean shouldUseNewSigningApi() {
		try {
			if (this.firmwareVersion == null) this.getFirmwareVersion();
		} catch (BTChipException e) {
			return false;
		}

		// True for Nano S/X
		if (this.firmwareVersion.getArchitecture() == BTCHIP_ARCH_NANO_SX) {
			return true;
		}

		// True for ver >= 1.1.2 on ledger 1.x
		if (this.firmwareVersion.getArchitecture() == BTCHIP_ARCH_LEDGER_1) {
			return this.firmwareVersion.getMajor() > 1 ||
				(this.firmwareVersion.getMajor() == 1 && this.firmwareVersion.getMinor() > 1) ||
				(this.firmwareVersion.getMajor() == 1 && this.firmwareVersion.getMinor() == 1 && this.firmwareVersion.getPatch() >= 2);
		}

		return false;
	}

	public boolean signMessagePrepare(final List<Integer> path, byte[] message) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeBuffer(data, pathToByteArray(path));
		if (this.shouldUseNewSigningApi()) {
			// length has two bytes in Ledger 1.0.2+
			data.write((byte)0);
		}
		data.write((byte)message.length);
		BufferUtils.writeBuffer(data, message);
		final byte p2 = (byte) (this.shouldUseNewSigningApi() ? 0x01 : 0x00);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SIGN_MESSAGE, (byte)0x00, p2, data.toByteArray(), OK);
		return (response[0] == (byte)0x01);
	}

	public BTChipSignature signMessageSign(byte[] pin) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		if (pin == null) {
			data.write((byte)0);
		}
		else {
			data.write((byte)pin.length);
			BufferUtils.writeBuffer(data, pin);
		}
		final byte p2 = (byte) (this.shouldUseNewSigningApi() ? 0x01 : 0x00);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SIGN_MESSAGE, (byte)0x80, p2, data.toByteArray(), OK);
		int yParity = (response[0] & 0x0F);
		response[0] = (byte)0x30;
		return new BTChipSignature(response, yParity);
	}

	public BTChipFirmware getFirmwareVersion() throws BTChipException {
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_FIRMWARE_VERSION, (byte)0x00, (byte)0x00, 0x00, OK);

		int features = response[0] & 0xff;
		int architecture = response[1] & 0xff;
		int major = response[2] & 0xff;
		int minor = response[3] & 0xff;
		int patch = response[4] & 0xff;
		this.firmwareVersion = new BTChipFirmware(features, architecture, major, minor, patch);

		return this.firmwareVersion;
	}

	private static String readString(byte[] buffer, int offset, int length) {
		return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(Arrays.copyOfRange(buffer, offset, offset + length))).toString();
	}

	public BTChipApplication getApplication() throws BTChipException {
		final int APP_DETAILS_FORMAT_VERSION = 1;

		byte[] response = exchangeApdu(BTCHIP_CLA_COMMON_SDK, BTCHIP_INS_GET_APP_NAME_AND_VERSION, (byte)0x00, (byte)0x00, 0x00, OK);

		int offset = 0;
		if (response[offset++] != APP_DETAILS_FORMAT_VERSION) {
			throw new BTChipException("Unsupported application format");
		}

		int nameLength = (response[offset++] & 0xff);
		final String name = BTChipDongle.readString(response, offset, nameLength);
		offset += nameLength;

		int versionLength = (response[offset++] & 0xff);
		final String version = BTChipDongle.readString(response, offset, versionLength);

		this.application = new BTChipApplication(name, version);
		return this.application;
	}

	public String getGreenAddress(final boolean csv, final long subaccount, final long branch, final long pointer, final long csvBlocks) throws BTChipException {
		final ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeUint32BE(data, subaccount);
		BufferUtils.writeUint32BE(data, branch);
		BufferUtils.writeUint32BE(data, pointer);
		if (csv)
			BufferUtils.writeUint32BE(data, csvBlocks);

		final byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_LIQUID_GREEN_ADDRESS, (byte)0x01, csv ? (byte)0x00 : (byte)0x01, data.toByteArray(), OK_OR_NOT_SUPPORTED);
		return  BTChipDongle.readString(response, 0, response.length);
	}

	public void setKeymapEncoding(byte[] keymapEncoding) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeBuffer(data, keymapEncoding);
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SET_KEYMAP, (byte)0x00, (byte)0x00, data.toByteArray(), OK_OR_NOT_SUPPORTED);
	}

	public boolean setup(OperationMode supportedOperationModes[], Feature features[], int keyVersion, int keyVersionP2SH, byte[] userPin, byte[] wipePin, byte[] keymapEncoding, byte[] seed, byte[] developerKey) throws BTChipException {
		int operationModeFlags = 0;
		int featuresFlags = 0;
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		for (OperationMode currentOperationMode : supportedOperationModes) {
			operationModeFlags |= currentOperationMode.getValue();
		}
		for (Feature currentFeature : features) {
			featuresFlags |= currentFeature.getValue();
		}
		data.write(operationModeFlags);
		data.write(featuresFlags);
		data.write(keyVersion);
		data.write(keyVersionP2SH);
		if ((userPin.length < 0x04) || (userPin.length > 0x20)) {
			throw new BTChipException("Invalid user PIN length");
		}
		data.write(userPin.length);
		BufferUtils.writeBuffer(data, userPin);
		if (wipePin != null) {
			if (wipePin.length > 0x04) {
				throw new BTChipException("Invalid wipe PIN length");
			}
			data.write(wipePin.length);
			BufferUtils.writeBuffer(data, wipePin);
		}
		else {
			data.write(0);
		}
		if (seed != null) {
			if ((seed.length < 32) || (seed.length > 64)) {
				throw new BTChipException("Invalid seed length");
			}
			data.write(seed.length);
			BufferUtils.writeBuffer(data, seed);
		}
		else {
			data.write(0);
		}
		if (developerKey != null) {
			if (developerKey.length != 0x10) {
				throw new BTChipException("Invalid developer key");
			}
			data.write(developerKey.length);
			BufferUtils.writeBuffer(data, developerKey);
		}
		else {
			data.write(0);
		}
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SETUP, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		if (keymapEncoding != null) {
			setKeymapEncoding(keymapEncoding);
		}
		return (response[0] == (byte)0x01);
	}

	private byte[] pathToByteArray(final List<Integer> path) throws BTChipException {
		final int len = path.size();
		if (len == 0) {
			return new byte[] { 0 };
		}
		if (len > 10) {
			throw new BTChipException("Path too long");
		}
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		result.write((byte)len);
		for (final Integer element : path) {
			BufferUtils.writeUint32BE(result, element);
		}
		return result.toByteArray();
	}
}
