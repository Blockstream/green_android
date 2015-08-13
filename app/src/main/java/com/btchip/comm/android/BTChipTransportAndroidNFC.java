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

package com.btchip.comm.android;

import java.util.concurrent.Future;

import nordpol.android.AndroidCard;

import android.util.Log;

import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.utils.Dump;
import com.btchip.utils.FutureUtils;

public class BTChipTransportAndroidNFC implements BTChipTransport {
	
	public static final int DEFAULT_TIMEOUT = 30000;
	private static final byte DEFAULT_AID[] = Dump.hexToBin("a0000006170054bf6aa94901");
	                                                            		
	private AndroidCard card;
	private int timeout;
	private boolean debug;
	private byte[] aid;
	private boolean selected;
	
	public BTChipTransportAndroidNFC(AndroidCard card, int timeout) {
		this.card = card;
		this.timeout = timeout;
		card.setTimeout(timeout);
		aid = DEFAULT_AID;
	}
	
	public BTChipTransportAndroidNFC(AndroidCard card) {
		this(card, DEFAULT_TIMEOUT);
	}
	
	public void setAID(byte[] aid) {
		this.aid = aid;
	}
	
	@Override
	public Future<byte[]> exchange(byte[] command) throws BTChipException {
		if (!selected) {
			byte[] selectCommand = new byte[aid.length + 5];
			selectCommand[0] = (byte)0x00;
			selectCommand[1] = (byte)0xA4;
			selectCommand[2] = (byte)0x04;
			selectCommand[3] = (byte)0x00;
			selectCommand[4] = (byte)aid.length;
			System.arraycopy(aid, 0, selectCommand, 5, aid.length);
			try {
				exchangeInternal(selectCommand);
			}
			catch(Exception e) {				
			}
			selected = true;
		}
		return exchangeInternal(command);
	}
	
	public Future<byte[]> exchangeInternal(byte[] command) throws BTChipException {		
		try {
			if (!card.isConnected()) {
				card.connect();
				card.setTimeout(timeout);
				if (debug) {
					Log.d(BTChipTransportAndroid.LOG_STRING, "Connected");
				}
			}
			if (debug) {
				Log.d(BTChipTransportAndroid.LOG_STRING, "=> " + Dump.dump(command));
			}
			byte[] commandLe = new byte[command.length + 1];
			System.arraycopy(command, 0, commandLe, 0, command.length);
			byte[] response = card.transceive(commandLe);
			if (debug) {
				Log.d(BTChipTransportAndroid.LOG_STRING, "<= " + Dump.dump(response));
			}
			return FutureUtils.getDummyFuture(response);			
		}
		catch(Exception e) {
			try {
				card.close();
			}
			catch(Exception e1) {				
			}
			throw new BTChipException("I/O error", e);
		}
		
	}

	@Override
	public void close() throws BTChipException {
		try {
			if (card.isConnected()) {
				card.close();
			}			
		}
		catch(Exception e) {			
		}
	}

	@Override
	public void setDebug(boolean debugFlag) {
		this.debug = debugFlag;
	}
}
