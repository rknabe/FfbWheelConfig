package com.rkade;

import com.fazecast.jSerialComm.SerialPort;
import purejavahidapi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceManager implements InputReportListener, DeviceRemovalListener {
    private final static int LEONARDO_VENDOR_ID = 0x2341;
    private final static int LEONARDO_PRODUCT_ID = 0x8036;
    volatile static boolean deviceAttached = false;
    private static byte currentDataType = DataReport.CMD_GET_VER;
    private static byte currentDataIndex = 0;
    private static HidDeviceInfo deviceInfo = null;
    private static HidDevice openedDevice = null;
    private final List<DeviceListener> deviceListeners = new ArrayList<>();

    public DeviceManager() {
        new Thread(new ConnectionThread()).start();
        new Thread(new OutputReportThread()).start();
    }

    public void addDeviceListener(DeviceListener deviceListener) {
        deviceListeners.add(deviceListener);
    }

    @Override
    public void onDeviceRemoval(HidDevice hidDevice) {
        System.out.println("device removed");
        deviceAttached = false;
        deviceInfo = null;
        openedDevice = null;
        currentDataType = DataReport.CMD_GET_VER;
        currentDataIndex = 0;
        Device device = getDevice(hidDevice);
        notifyListenersDeviceDetached(device);
    }

    private void notifyListenersDeviceAttached(Device device) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceAttached(device);
        }
    }

    private void notifyListenersDeviceDetached(Device device) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceDetached(device);
        }
    }

    private void notifyListenersDeviceUpdated(Device device, String status, DataReport report) {
        for (DeviceListener deviceListener : deviceListeners) {
            deviceListener.deviceUpdated(device, status, report);
        }
    }

    private Device getDevice(HidDevice hidDevice) {
        return new Device(hidDevice);
    }

    @Override
    public void onInputReport(HidDevice hidDevice, byte id, byte[] data, int len) {
        if (id == 16) {
            DataReport report = new DataReport(id, data);
            notifyListenersDeviceUpdated(new Device(hidDevice), "Updating...", report);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class ConnectionThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (!deviceAttached) {
                    deviceInfo = null;
                    System.out.println("scanning");
                    notifyListenersDeviceUpdated(null, "Scanning...", null);
                    List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
                    for (HidDeviceInfo info : devList) {
                        if (info.getVendorId() == (short) LEONARDO_VENDOR_ID && info.getProductId() == (short) LEONARDO_PRODUCT_ID) {
                            deviceInfo = info;
                            break;
                        }
                    }
                    if (deviceInfo == null) {
                        System.out.println("device not found");
                        notifyListenersDeviceUpdated(null, "Device Not Found...", null);
                        sleep(1000);
                    } else {
                        System.out.println("device found");
                        notifyListenersDeviceUpdated(null, "Attached...", null);
                        deviceAttached = true;
                        if (deviceAttached) {
                            try {
                                openedDevice = PureJavaHidApi.openDevice(deviceInfo);
                                if (openedDevice != null) {
                                    Device device = getDevice(openedDevice);
                                    notifyListenersDeviceUpdated(device, "Opened", null);
                                    SerialPort[] ports = SerialPort.getCommPorts();
                                    for (SerialPort port : ports) {
                                        if (port.getVendorID() == LEONARDO_VENDOR_ID && port.getProductID() == LEONARDO_PRODUCT_ID) {
                                            device.setName(port.getDescriptivePortName());
                                        }
                                    }
                                    notifyListenersDeviceAttached(device);
                                    openedDevice.setDeviceRemovalListener(DeviceManager.this);
                                    openedDevice.setInputReportListener(DeviceManager.this);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
                sleep(2000);
            }
        }
    }

    private class OutputReportThread implements Runnable {

        @Override
        public void run() {
            int failCount = 0;
            while (true) {
                if (openedDevice != null) {
                    byte[] data = new byte[7];
                    data[0] = currentDataType;
                    data[1] = currentDataIndex;
                    int ret = openedDevice.setOutputReport(DataReport.CMD_REPORT_ID, data, 7);
                    if (ret > 0) {
                        failCount = 0;
                        switch (currentDataType) {
                            case DataReport.CMD_GET_VER:
                                currentDataIndex = 0;
                                currentDataType = DataReport.CMD_GET_STEER;
                                break;
                            case DataReport.CMD_GET_STEER:
                                currentDataIndex = 0;
                                currentDataType = DataReport.CMD_GET_ANALOG;
                                break;
                            case DataReport.CMD_GET_ANALOG:
                                ++currentDataIndex;
                                if (currentDataIndex > 6) {
                                    currentDataIndex = 0;
                                    currentDataType = DataReport.CMD_GET_BUTTONS;
                                }
                                break;
                            case DataReport.CMD_GET_BUTTONS:
                                currentDataIndex = 0;
                                currentDataType = DataReport.CMD_GET_GAINS;
                                break;
                            case DataReport.CMD_GET_GAINS:
                                currentDataIndex = 0;
                                currentDataType = DataReport.CMD_GET_MISC;
                                break;
                            default:
                                currentDataIndex = 0;
                                currentDataType = DataReport.CMD_GET_VER;
                                break;
                        }
                    } else {
                        System.out.println("Error sending OutputReport");
                        ++failCount;
                        if (failCount > 3) {
                            onDeviceRemoval(openedDevice);
                        }
                        sleep(1000);
                    }
                    sleep(1);
                } else {
                    sleep(1000);
                }
            }
        }
    }
}