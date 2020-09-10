package com.btchip.comm;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothDevice;

/**
 * Utilities associated to a GATT connection
 */
public class GattUtils {

	/**
	 * Return a string representation of a GATT status
	 * @param status GATT status
	 * @return string representation of the GATT status
	 */
	public static String statusToString(int status) {
		switch(status) {
			case BluetoothGatt.GATT_FAILURE:
				return "GATT_FAILURE";
			case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
				return "GATT_INSUFFICIENT_AUTHENTICATION";
			case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
				return "GATT_INSUFFICIENT_ENCRYPTION";
			case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
				return "GATT_INVALID_ATTRIBUTE_LENGTH";
			case BluetoothGatt.GATT_INVALID_OFFSET:
				return "GATT_INVALID_OFFSET";
			case BluetoothGatt.GATT_READ_NOT_PERMITTED:
				return "GATT_READ_NOT_PERMITTED";
			case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
				return "GATT_REQUEST_NOT_SUPPORTED";
			case BluetoothGatt.GATT_SUCCESS:
				return "GATT_SUCCESS";
			case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
				return "GATT_WRITE_NOT_PERMITTED";
			default:
				return "Unknown GATT status " + status;
		}
	}

	/**
	 * Return a string representation of a GATT connection state
	 * @param state GATT state
	 * @return string representation of the GATT connection state
	 */
	public static String stateToString(int state) {
		switch(state) {
			case BluetoothProfile.STATE_CONNECTED:
				return "STATE_CONNECTED";
			case BluetoothProfile.STATE_CONNECTING:
				return "STATE_CONNECTING";
			case BluetoothProfile.STATE_DISCONNECTED:
				return "STATE_DISCONNECTED";
			case BluetoothProfile.STATE_DISCONNECTING:
				return "STATE_DISCONNECTING";
			default:
				return "Unknown state " + state;
		}
	}

	/**
	 * Return a string representation of a device bonding state
	 * @param bond bonding state
	 * @return string representation of the bonding state
	 */
	public static String bondToString(int bond) {
		switch(bond) {
			case BluetoothDevice.BOND_NONE:
				return "BOND_NONE";
			case BluetoothDevice.BOND_BONDING:
				return "BOND_BONDING";
			case BluetoothDevice.BOND_BONDED:
				return "BOND_BONDED";
			default:
				return "Unknown bond " + bond;
		}
	}
}
