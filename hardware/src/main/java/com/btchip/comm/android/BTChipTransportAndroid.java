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

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.btchip.comm.BTChipTransport;

import java.util.HashMap;

public class BTChipTransportAndroid {

	public static UsbDevice getDevice(UsbManager manager) {
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		for (UsbDevice device : deviceList.values()) {
			if ((device.getVendorId() == VID || device.getVendorId() == VID2) &&
			   ((device.getProductId() == PID_WINUSB) || (device.getProductId() == PID_HID) ||
			    (device.getProductId() == PID_NANOS_LEGACY) || (device.getProductId() >> 8 == PID_NANOS) ||
				(device.getProductId() == PID_NANOX_LEGACY) || (device.getProductId() >> 8 == PID_NANOX) ||
				(device.getProductId() == PID_HID_LEDGER_PROTON) || (device.getProductId() == PID_HID_LEDGER))) {
				return device;
			}
		}
		return null;		
	}
	
	public static BTChipTransport open(UsbManager manager, UsbDevice device) {
		// Must only be called once permission is granted (see http://developer.android.com/reference/android/hardware/usb/UsbManager.html)
		// Important if enumerating, rather than being awaken by the intent notification
		UsbInterface dongleInterface = device.getInterface(0);
        UsbEndpoint in = null;
        UsbEndpoint out = null;
        boolean ledger; 
        for (int i=0; i<dongleInterface.getEndpointCount(); i++) {
            UsbEndpoint tmpEndpoint = dongleInterface.getEndpoint(i);
            if (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                in = tmpEndpoint;
            }
            else {
                out = tmpEndpoint;
            }
        }
        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) {
            return null;
        }
        connection.claimInterface(dongleInterface, true);
        ledger = ((device.getProductId() == PID_HID_LEDGER) || (device.getProductId() == PID_HID_LEDGER_PROTON) ||
		(device.getProductId() == PID_NANOS_LEGACY) || (device.getProductId() >> 8 == PID_NANOS) ||
		(device.getProductId() == PID_NANOX_LEGACY) || (device.getProductId() >> 8 == PID_NANOX));
        if (device.getProductId() == PID_WINUSB) {
        	return new BTChipTransportAndroidWinUSB(connection, dongleInterface, in, out, TIMEOUT);
        }
        else {
        	return new BTChipTransportAndroidHID(device, connection, dongleInterface, in, out, TIMEOUT, ledger);
        }
	}
	
	static final String LOG_STRING = "BTChip";

	public static boolean isLedgerWithScreen(final UsbDevice d) {
		final int pId = d.getProductId();
		final boolean screenDevice = pId == PID_NANOS_LEGACY || pId == PID_NANOX_LEGACY || pId >> 8 == PID_NANOS || pId >> 8 == PID_NANOX;
		return screenDevice && d.getVendorId() == VID2;
	}

	public static boolean isNanoX(final UsbDevice d) {
		final int pId = d.getProductId();
		return pId == PID_NANOX_LEGACY || pId == PID_NANOX;
	}
	
	private static final int VID = 0x2581;
	private static final int VID2 = 0x2c97;
	private static final int PID_WINUSB = 0x1b7c;
	private static final int PID_HID = 0x2b7c;
	private static final int PID_HID_LEDGER = 0x3b7c;
	private static final int PID_HID_LEDGER_PROTON = 0x4b7c;
	private static final int PID_NANOS_LEGACY = 0x0001;
	private static final int PID_NANOX_LEGACY = 0x0004;
	private static final int PID_NANOS = 0x10;
	private static final int PID_NANOX = 0x40;
	private static final int TIMEOUT = 20000;	
}
