package com.btchip.comm;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.io.ByteArrayOutputStream;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import android.os.Build;

import com.btchip.utils.FutureUtils;

/**
 * \brief Communication class with a Ledger device connected using Bluetooth Low Energy (Nano X)
 *
 * The caller is in charge of scanning for the device, establishing a connection to its GATT service, and handle the device connection lifecycle
 *
 * All events notified on the GATT callback shall be passed to the current object instance
 *
 * Due to a temporary BLE stack bug, the connection shall be closed immediately following a successful bonding
 * to commit the keys
 *
 * Open shall only be called when a connection is established with the device
 *
 * The following service UUIDs can be used as a filter when scanning for a device
 *
 * Nano X : 13D63400-2C97-0004-0000-4C6564676572
 *
 */
public class LedgerDeviceBLE implements BTChipTransport {

  /** GATT Service UUID */
  public static final UUID SERVICE_UUID = UUID.fromString("13D63400-2C97-0004-0000-4C6564676572");

  private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("13D63400-2C97-0004-0002-4C6564676572");
  private static final UUID NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("13D63400-2C97-0004-0001-4C6564676572");

  private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private static final byte[] QUERY_MTU = new byte[] { 0x08, 0x00, 0x00, 0x00, 0x00 };

  private static final int DEFAULT_MTU = 20;
  private static final int DEFAULT_MAX_MTU = 100;
  private static final int DEFAULT_QUEUE_CAPACITY = 100;
  private static final int DEFAULT_TIMEOUT_MS = 15000;
  private static final String LOG_STRING = "LedgerDeviceBLE";

  private BluetoothGatt connection;
  private int timeout;
  private int maxMtu;
  private BluetoothGattCharacteristic characteristicWrite;
  private BluetoothGattCharacteristic characteristicNotify;
  private GattCallback gattCallback;
  private LinkedBlockingQueue<GattCallback.GattEvent> blockingQueue;
  private byte transferBuffer[];
  private boolean opened;
  private boolean disconnected;

  private boolean debug;
  private int mtu;

  /** Class constructor
   * @param connection Connection to the device GATT service established by the caller
   * @param timeoutMS timeout when interacting with the device (in milliseconds)
   * @param maxMTU maximum MTU to use when negociating
   */
  public LedgerDeviceBLE(BluetoothGatt connection, int timeoutMS, int maxMTU) {
    this.connection = connection;
    blockingQueue = new LinkedBlockingQueue<GattCallback.GattEvent>();
    gattCallback = new GattCallback(blockingQueue);
    this.timeout = timeoutMS;
    this.maxMtu = maxMTU;
    setMtu(DEFAULT_MTU);
  }

  /** Class constructor using a default 30s timeout and a maximum MTU of 100 bytes
   * @param connection Connection to the device GATT service established by the caller
   */
  public LedgerDeviceBLE(BluetoothGatt connection) {
    this(connection, DEFAULT_TIMEOUT_MS, DEFAULT_MAX_MTU);
  }

  /**
   * Return the GATT callback to which all events shall be redirected
   * @return GATT callback
   */
  public GattCallback getGattCallback() {
    return gattCallback;
  }

  private void setMtu(int mtu) {
    this.mtu = mtu;
    transferBuffer = new byte[mtu];
  }

  private void clearQueue() {
    GattCallback.GattEvent event;
    if (debug) {
      Log.d(LOG_STRING, "Begin clear queue");
    }
    while ((event = blockingQueue.poll()) != null) {
      if (debug) {
        Log.d(LOG_STRING, "Dropping " + event.toString());
      }
    }
    if (debug) {
      Log.d(LOG_STRING, "End clear queue");
    }
  }

  private GattCallback.GattEvent waitEvent(GattCallback.GattEventType eventType) throws LedgerException {
    return waitEvent(eventType, null, null, null);
  }

  private GattCallback.GattEvent waitEvent(GattCallback.GattEventType eventType, UUID uuid) throws LedgerException {
    return waitEvent(eventType, uuid, null, null);
  }

  private GattCallback.GattEvent waitEvent(GattCallback.GattEventType eventType, UUID uuid, GattCallback.GattEventType eventType2, UUID uuid2) throws LedgerException {
    for (;;) {
      GattCallback.GattEvent event = null;
      try {
        event = blockingQueue.poll(timeout, TimeUnit.MILLISECONDS);
      }
      catch(InterruptedException ex) {
        throw new LedgerException(LedgerException.ExceptionReason.INTERNAL_ERROR, ex);
      }
      if (event == null) {
        throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Timeout");
      }
      if (debug) {
        Log.d(LOG_STRING, "Received " + event.toString());
      }
      // Handle generic events
      if (event.getStatus() != BluetoothGatt.GATT_SUCCESS) {
        throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Unsuccessful event " + event.toString());
      }
      switch(event.getEventType()) {
        case GATT_MTU_CHANGED:
          setMtu(event.getMtu());
          break;
        case GATT_CONNECTION_STATE_CHANGE:
          switch(event.getNewState()) {
            case BluetoothProfile.STATE_DISCONNECTED:
            case BluetoothProfile.STATE_DISCONNECTING:
              disconnected = true;
              opened = false;
              throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Disconnected");
          }
          break;
      }
      if (event.getEventType().equals(eventType) && (
              (uuid == null) || (event.getUuid().equals(uuid)))) {
        return event;
      }
      if (eventType2 != null) {
        if (event.getEventType().equals(eventType2) && (
                (uuid2 == null) || (event.getUuid().equals(uuid2)))) {
          return event;
        }
      }
    }
  }

  public void connect() {
    GattCallback.GattEvent event;
    if (opened) {
      Log.d(LOG_STRING, "Already opened");
      return;
    }
    if (disconnected) {
      Log.d(LOG_STRING, "Disconnected");
      return;
    }
    clearQueue();
    if (!connection.discoverServices()) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to initiate GATT service discovery");
    }
    event = waitEvent(GattCallback.GattEventType.GATT_SERVICES_DISCOVERED);
    characteristicWrite = null;
    characteristicNotify = null;
    List<BluetoothGattService> services = connection.getServices();
    for (BluetoothGattService service : services) {
      if (!service.getUuid().equals(SERVICE_UUID)) {
        continue;
      }
      List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
      for (BluetoothGattCharacteristic characteristic : characteristics) {
        if (characteristic.getUuid().equals(WRITE_CHARACTERISTIC_UUID)) {
          characteristicWrite = characteristic;
        }
        else
        if (characteristic.getUuid().equals(NOTIFY_CHARACTERISTIC_UUID)) {
          characteristicNotify = characteristic;
        }
      }
    }
    if ((characteristicWrite == null) || (characteristicNotify == null)) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to find all service characteristics");
    }
    if (!connection.setCharacteristicNotification(characteristicNotify, true)) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to enable local notifications");
    }
    BluetoothGattDescriptor descriptor = characteristicNotify.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    if (!connection.writeDescriptor(descriptor)) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to enable remote notifications");
    }
    waitEvent(GattCallback.GattEventType.GATT_DESCRIPTOR_WRITE, CLIENT_CHARACTERISTIC_CONFIG);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (!connection.requestMtu(maxMtu)) {
        throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to request MTU");
      }
      waitEvent(GattCallback.GattEventType.GATT_MTU_CHANGED);
    }
    /* Extra test, request the MTU from the device side on the application layer */
    characteristicWrite.setValue(QUERY_MTU);
    if (!connection.writeCharacteristic(characteristicWrite)) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to write query_mtu message");
    }
    waitEvent(GattCallback.GattEventType.GATT_CHARACTERISTIC_WRITE, WRITE_CHARACTERISTIC_UUID);
    event = waitEvent(GattCallback.GattEventType.GATT_CHARACTERISTIC_CHANGED, NOTIFY_CHARACTERISTIC_UUID);
    byte[] data = event.getData();
    Log.d(LOG_STRING, "Device MTU answer " + Dump.dump(data));
    setMtu(data[5] & 0xff);
    opened = true;
  }

  @Override
  public Future<byte[]> exchange(byte[] apdu) throws LedgerException {
    GattCallback.GattEvent event = null;
    ByteArrayOutputStream response = new ByteArrayOutputStream();
    byte[] responseData = null;
    int offset = 0;
    int responseSize;
    int result;
    if (!opened) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Device is not opened");
    }
    if (disconnected) {
      throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Device is disconnected");
    }
    clearQueue();
    if (debug) {
      Log.d(LOG_STRING, "=> " + Dump.dump(apdu));
    }
    apdu = LedgerWrapper.wrapCommandAPDU(apdu, mtu);
    while (offset != apdu.length) {
      int blockSize = (apdu.length - offset > mtu ? mtu : apdu.length - offset);
      System.arraycopy(apdu, offset, transferBuffer, 0, blockSize);
      if (debug) {
        Log.d(LOG_STRING, "=> Fragment " + Dump.dump(transferBuffer));
      }
      characteristicWrite.setValue(transferBuffer);
      if (!connection.writeCharacteristic(characteristicWrite)) {
        throw new LedgerException(LedgerException.ExceptionReason.IO_ERROR, "Failed to write fragment");
      }
      event = waitEvent(GattCallback.GattEventType.GATT_CHARACTERISTIC_WRITE, WRITE_CHARACTERISTIC_UUID,
              GattCallback.GattEventType.GATT_CHARACTERISTIC_CHANGED, NOTIFY_CHARACTERISTIC_UUID);
      offset += blockSize;
    }
    if (!event.getEventType().equals(GattCallback.GattEventType.GATT_CHARACTERISTIC_CHANGED)) {
      event = null;
    }
    int packageSize = mtu;
    while (responseData == null) {
      if (event == null) {
        event = waitEvent(GattCallback.GattEventType.GATT_CHARACTERISTIC_CHANGED, NOTIFY_CHARACTERISTIC_UUID);
      }
      byte[] data = event.getData();
      if (debug) {
        Log.d(LOG_STRING, "<= Fragment " + Dump.dump(data));
      }

      response.write(data, 0, data.length);
      responseData = LedgerWrapper.unwrapResponseAPDU(response.toByteArray(), packageSize);
      if (responseData == null)
        packageSize = data.length;
      event = null;
    }
    if (debug) {
      Log.d(LOG_STRING, "<= " + Dump.dump(responseData));
    }
    return FutureUtils.getDummyFuture(responseData);
  }

  @Override
  public void close() throws LedgerException {
    connection.disconnect();
  }

  @Override
  public void setDebug(boolean debugFlag) {
    this.debug = debugFlag;
  }

  @Override
  public Boolean isUsb() {
    return false;
  }

  public boolean isOpened() {
    return opened;
  }

}
