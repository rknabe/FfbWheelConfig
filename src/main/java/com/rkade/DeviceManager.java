package com.rkade;

import com.fazecast.jSerialComm.SerialPort;
import purejavahidapi.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static com.rkade.DataReport.DATA_REPORT_ID;

public final class DeviceManager implements InputReportListener, DeviceRemovalListener {
    private final static Logger logger = Logger.getLogger(DeviceManager.class.getName());
    private final static int LEONARDO_VENDOR_ID = 0x2341;
    private final static int LEONARDO_PRODUCT_ID = 0x8036;
    private final static int OUTPUT_REPORT_DATA_LENGTH = 7;
    private final static int SLEEP_BETWEEN_OUTPUT_REPORT = 10;
    private final static byte AXIS_COUNT = 7;
    private final static List<DeviceListener> deviceListeners = Collections.synchronizedList(new ArrayList<>());
    private final static Map<String, Device> deviceMap = Collections.synchronizedMap(new HashMap<>());
    private static volatile boolean deviceAttached = false;
    private static volatile boolean versionReported = false;
    private static volatile HidDeviceInfo deviceInfo = null;
    private static volatile HidDevice openedDevice = null;

    public DeviceManager() {
        new Thread(new ConnectionRunner()).start();
        new Thread(new OutputReportRunner()).start();
    }

    public void addDeviceListener(DeviceListener deviceListener) {
        deviceListeners.add(deviceListener);
    }

    @Override
    public void onDeviceRemoval(HidDevice hidDevice) {
        logger.info("device removed");
        deviceAttached = false;
        versionReported = false;
        deviceInfo = null;
        openedDevice = null;
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
        //hidPath is not null terminated, force to it null-term and uppercase to match SDL case
        String path = hidDevice.getHidDeviceInfo().getPath().trim().toUpperCase();
        return deviceMap.computeIfAbsent(path, k -> new Device(hidDevice, path));
    }

    @Override
    public void onInputReport(HidDevice hidDevice, byte id, byte[] data, int len) {
        if (id == DATA_REPORT_ID) {
            DataReport report = DataReportFactory.create(id, data);
            notifyListenersDeviceUpdated(getDevice(hidDevice), null, report);
            if (report instanceof VersionDataReport) {
                versionReported = true;
            }
        }
    }

    private void getOutputReport(byte dataType, byte dataIndex, byte[] data) throws IOException {
        data[0] = dataType;
        data[1] = dataIndex;
        int ret = openedDevice.setOutputReport(DataReport.CMD_REPORT_ID, data, OUTPUT_REPORT_DATA_LENGTH);
        if (ret <= 0) {
            throw new IOException("Device returned error for dataType:" + dataType + " dataIndex:" + dataIndex);
        }
        sleep(SLEEP_BETWEEN_OUTPUT_REPORT);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
    }

    private final class ConnectionRunner implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (!deviceAttached) {
                    deviceInfo = null;
                    logger.info("scanning");
                    notifyListenersDeviceUpdated(null, "Scanning...", null);
                    List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
                    for (HidDeviceInfo info : devList) {
                        if (info.getVendorId() == LEONARDO_VENDOR_ID && info.getProductId() == LEONARDO_PRODUCT_ID) {
                            deviceInfo = info;
                            break;
                        }
                    }
                    if (deviceInfo == null) {
                        logger.info("device not found");
                        notifyListenersDeviceUpdated(null, "Device Not Found...", null);
                        sleep(1000);
                    } else {
                        logger.info("device found");
                        notifyListenersDeviceUpdated(null, "Attached...", null);
                        deviceAttached = true;
                        if (deviceAttached) {
                            try {
                                openedDevice = PureJavaHidApi.openDevice(deviceInfo);
                                if (openedDevice != null) {
                                    openedDevice.open();
                                    Device device = getDevice(openedDevice);
                                    notifyListenersDeviceUpdated(device, "Opened", null);

                                    SerialPort[] ports = SerialPort.getCommPorts();
                                    for (SerialPort port : ports) {
                                        if (port.getVendorID() == LEONARDO_VENDOR_ID && port.getProductID() == LEONARDO_PRODUCT_ID) {
                                            device.setName(port.getDescriptivePortName());
                                        }
                                    }
                                    openedDevice.setDeviceRemovalListener(DeviceManager.this);
                                    openedDevice.setInputReportListener(DeviceManager.this);
                                    notifyListenersDeviceAttached(device);
                                }
                            } catch (IOException ex) {
                                logger.warning(ex.getMessage());
                            }
                        }
                    }
                }
                sleep(2000);
            }
        }
    }

    private final class OutputReportRunner implements Runnable {
        @Override
        public void run() {
            int failCount = 0;
            byte[] data = new byte[OUTPUT_REPORT_DATA_LENGTH];
            while (true) {
                if (openedDevice != null) {
                    try {
                        if (!versionReported) {
                            //only need to do this once
                            getOutputReport(DataReport.CMD_GET_VER, (byte) 0, data);
                        }
                        getOutputReport(DataReport.CMD_GET_STEER, (byte) 0, data);
                        for (byte i = 0; i < AXIS_COUNT; i++) {
                            getOutputReport(DataReport.CMD_GET_ANALOG, i, data);
                        }
                        getOutputReport(DataReport.CMD_GET_STEER, (byte) 0, data);
                        getOutputReport(DataReport.CMD_GET_BUTTONS, (byte) 0, data);
                        getOutputReport(DataReport.CMD_GET_GAINS, (byte) 0, data);
                        getOutputReport(DataReport.CMD_GET_MISC, (byte) 0, data);
                        failCount = 0;
                    } catch (IOException ex) {
                        ++failCount;
                        if (failCount > 3) {
                            onDeviceRemoval(openedDevice);
                        }
                        sleep(1000);
                        logger.warning(ex.getMessage());
                    }
                } else {
                    sleep(1000);
                }
            }
        }
    }
}