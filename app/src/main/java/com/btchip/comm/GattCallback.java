package com.btchip.comm;

import java.util.concurrent.BlockingQueue;
import java.util.UUID;
import java.util.Arrays;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattCallback;

/**
 * GATT callback reporting events in a queue for further processing
 */
public class GattCallback extends BluetoothGattCallback {

	private BlockingQueue<GattEvent> queue;

	enum GattEventType {
		GATT_CHARACTERISTIC_CHANGED,
		GATT_CHARACTERISTIC_READ,
		GATT_CHARACTERISTIC_WRITE,
		GATT_CONNECTION_STATE_CHANGE,
		GATT_DESCRIPTOR_READ,
		GATT_DESCRIPTOR_WRITE,
		GATT_MTU_CHANGED,
		GATT_SERVICES_DISCOVERED
	};

	class GattEvent {

		private GattEventType eventType;
		private UUID uuid;
		private byte[] data;
		private int status;
		private int newState;
		private int mtu;

		public GattEvent(GattEventType eventType, UUID uuid, int status, byte[] data, int newState, int mtu) {
			this.eventType = eventType;
			this.uuid = uuid;
			this.status = status;
			this.data = data;
			this.newState = newState;
			this.mtu = mtu;
		}

		public GattEvent(GattEventType eventType, UUID uuid, int status, byte[] data) {
			this.eventType = eventType;
			this.uuid = uuid;
			this.status = status;
			this.data = data;
		}

		public GattEvent(GattEventType eventType, UUID uuid, int status) {
			this.eventType = eventType;
			this.uuid = uuid;
			this.status = status;
		}

		public GattEventType getEventType() {
			return eventType;
		}
		public UUID getUuid() {
			return uuid;
		}
		public int getStatus() {
			return status;
		}
		public byte[] getData() {
			return data;
		}
		public int getNewState() {
			return newState;
		}
		public int getMtu() {
			return mtu;
		}

		public String toString() {
			switch(eventType) {
				case GATT_CHARACTERISTIC_CHANGED:
					return "GATT_CHARACTERISTIC_CHANGED " + uuid.toString();
				case GATT_CHARACTERISTIC_READ:
					return "GATT_CHARACTERISTIC_READ " + uuid.toString() + " " + GattUtils.statusToString(status) + " " + Dump.dump(data);
				case GATT_CHARACTERISTIC_WRITE:
					return "GATT_CHARACTERISTIC_WRITE " + uuid.toString() + " " + GattUtils.statusToString(status);
				case GATT_CONNECTION_STATE_CHANGE:
					return "GATT_CONNECTION_STATE_CHANGE " + GattUtils.statusToString(status) + " " + GattUtils.stateToString(newState);
				case GATT_DESCRIPTOR_READ:
					return "GATT_DESCRIPTOR_READ " + uuid.toString() + " " + GattUtils.statusToString(status) + " " + Dump.dump(data);
				case GATT_DESCRIPTOR_WRITE:
					return "GATT_DESCRIPTOR_WRITE " + uuid.toString() + " " + GattUtils.statusToString(status);
				case GATT_MTU_CHANGED:
					return "GATT_MTU_CHANGED " + mtu + " " + GattUtils.statusToString(status);
				case GATT_SERVICES_DISCOVERED:
					return "GATT_SERVICES_DISCOVERED " + GattUtils.statusToString(status);
			}
			return "Unsupported event";
		}

	};

	public GattCallback(BlockingQueue<GattEvent> queue) {
		this.queue = queue;
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		byte[] data = Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length);
		queue.add(new GattEvent(GattEventType.GATT_CHARACTERISTIC_CHANGED, characteristic.getUuid(), BluetoothGatt.GATT_SUCCESS, data));
	}

	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		byte[] data = null;
		if (status == BluetoothGatt.GATT_SUCCESS) {
			data = Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length);
		}
		queue.add(new GattEvent(GattEventType.GATT_CHARACTERISTIC_READ, characteristic.getUuid(), status, data));
	}

	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		queue.add(new GattEvent(GattEventType.GATT_CHARACTERISTIC_WRITE, characteristic.getUuid(), status));
	}

	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		queue.add(new GattEvent(GattEventType.GATT_CONNECTION_STATE_CHANGE, null, status, null, newState, 0));
	}

	@Override
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		byte[] data = null;
		if (status == BluetoothGatt.GATT_SUCCESS) {
			data = Arrays.copyOf(descriptor.getValue(), descriptor.getValue().length);
		}
		queue.add(new GattEvent(GattEventType.GATT_DESCRIPTOR_READ, descriptor.getUuid(), status, data));
	}

	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		queue.add(new GattEvent(GattEventType.GATT_DESCRIPTOR_WRITE, descriptor.getUuid(), status));
	}

	@Override
	public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
		queue.add(new GattEvent(GattEventType.GATT_MTU_CHANGED, null, status, null, 0, mtu));
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		queue.add(new GattEvent(GattEventType.GATT_SERVICES_DISCOVERED, null, status));
	}
}
