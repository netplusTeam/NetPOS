package com.woleapp.netpos.app;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import com.horizonpay.smartpossdk.aidl.IAidlDevice;
import com.horizonpay.smartpossdk.aidl.beeper.IAidlBeeper;
import com.horizonpay.smartpossdk.aidl.camera.IAidlCamera;
import com.horizonpay.smartpossdk.aidl.cardreader.IAidlCardReader;
import com.horizonpay.smartpossdk.aidl.cpucard.IAidlCpuCard;
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2;
import com.horizonpay.smartpossdk.aidl.felica.IAidlFelicaCard;
import com.horizonpay.smartpossdk.aidl.led.IAidlLed;
import com.horizonpay.smartpossdk.aidl.m0ccard.IAidlM0CCard;
import com.horizonpay.smartpossdk.aidl.m0ev1card.IAidlM0Ev1Card;
import com.horizonpay.smartpossdk.aidl.m1card.IAidlM1Card;
import com.horizonpay.smartpossdk.aidl.pinpad.IAidlPinpad;
import com.horizonpay.smartpossdk.aidl.printer.IAidlPrinter;
import com.horizonpay.smartpossdk.aidl.psamcard.IAidlPsamCard;
import com.horizonpay.smartpossdk.aidl.serialport.IAidlSerialPort;
import com.horizonpay.smartpossdk.aidl.serialport.IAidlUsbSerialPort;
import com.horizonpay.smartpossdk.aidl.subLcd.IAidSubLcd;
import com.horizonpay.smartpossdk.aidl.sys.IAidlSys;


public class DeviceHelper {

    private final static String TAG = DeviceHelper.class.getName();

    private static IAidlPinpad pinpad;
    private static IAidlSys sysHandle;
    private static IAidlPrinter printer;
    private static IAidlEmvL2 emvHandler;
    private static IAidlCardReader cardReader;
    private static IAidlDevice device;
    private static IAidlLed led;
    private static IAidlBeeper beeper;
    private static IAidlFelicaCard felicaCard;
    private static IAidlCamera camera;
    private static IAidlM1Card m1Card;
    private static IAidlCpuCard cpuCard;
    private static IAidlM0Ev1Card m0Ev1Card;
    private static IAidlM0CCard m0CCard;
    private static IAidlSerialPort serialPort;

    private static NetPosApp application;

    private static IAidSubLcd iAidSubLcd;
    private static IAidlPsamCard iAidlPsamCard;

    private static IAidlUsbSerialPort iAidlUsbSerialPort;

    @SuppressLint("NewApi")
    public static void initDevices(NetPosApp app) throws RemoteException {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("android.intent.action.PACKAGE_ADDED");
//        filter.addDataScheme("package");

        application = app;
        if (application == null) {
            return;
        }
        device = application.getDevice();

        if (device != null) {
            try {
//                emvHandler = device.getNewEmvL2("com.horizonpay.sample");
                pinpad = device.getPinpad(false);
                sysHandle = device.getSysHandler();
                printer = device.getPrinter();
                emvHandler = device.getEmvL2();

                cardReader = device.getCardReader();
                led = device.getLED();
                beeper = device.getBeeper();
                m1Card = device.getM1Card();
                camera = device.getCamera();
                felicaCard = device.getFelicaCard();
                cpuCard = device.getCpuCard();
                m0CCard = device.getM0CCard();
                m0Ev1Card = device.getM0Ev1Card();
                serialPort = device.getSerialPort();
                iAidSubLcd = device.getSubLcd();
                iAidlPsamCard = device.getPsamCard();
                iAidlUsbSerialPort = device.getUsbSerialPort();
            } catch (RemoteException e) {
                e.printStackTrace();
                throw e;
            }
        } else {
            application.bindHorizonDriverService();
            reset();
        }
    }

    public static IAidlDevice getDevice() {
        return application.getDevice();
    }

    public static void checkState() throws RemoteException {
        if (application == null) {
            throw new RemoteException("Please restart the application.");
        }

        if (application.getDevice() == null) {
            application.bindHorizonDriverService();
            reset();
            throw new RemoteException("Device service connection failed, please try again later.");
        }
    }


    @SuppressLint("NewApi")
    public static IAidlPinpad getPinpad() throws RemoteException {
        if (pinpad == null) {
            checkState();
            try {
                return application.getDevice().getPinpad(false);
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return pinpad;
        }
    }


    @SuppressLint("NewApi")
    public static IAidlCardReader getCardReader() throws RemoteException {
        if (cardReader == null) {
            checkState();
            try {
                return application.getDevice().getCardReader();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return cardReader;
        }
    }


    @SuppressLint("NewApi")
    public static IAidlCpuCard getCpuCardHandler() throws RemoteException {
        if (cpuCard == null) {
            checkState();
            try {
                return application.getDevice().getCpuCard();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return cpuCard;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlM1Card getM1CardHandler() throws RemoteException {
        if (m1Card == null) {
            checkState();
            try {
                return application.getDevice().getM1Card();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return m1Card;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlSys getSysHandle() throws RemoteException {
        if (sysHandle == null) {
            checkState();
            try {
                return application.getDevice().getSysHandler();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return sysHandle;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlLed getLed() throws RemoteException {
        if (led == null) {
            checkState();
            try {
                return application.getDevice().getLED();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return led;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlPrinter getPrinter() throws RemoteException {
        if (printer == null) {
            checkState();
            try {
                return application.getDevice().getPrinter();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return printer;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlBeeper getBeeper() throws RemoteException {
        if (beeper == null) {
            checkState();
            try {
                return application.getDevice().getBeeper();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return beeper;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlEmvL2 getEmvHandler() throws RemoteException {
        if (emvHandler == null) {
            checkState();
            try {
                return application.getDevice().getEmvL2();
//                return application.getDevice().getNewEmvL2("com.horizonpay.sample");
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return emvHandler;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlCamera getCamera() throws RemoteException {
        if (camera == null) {
            checkState();
            try {
                return application.getDevice().getCamera();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return camera;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlM0CCard getM0CCardHandler() throws RemoteException {
        if (m0CCard == null) {
            checkState();
            try {
                return application.getDevice().getM0CCard();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return m0CCard;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlM0Ev1Card getM0Ev1CardHandler() throws RemoteException {
        if (m0Ev1Card == null) {
            checkState();
            try {
                return application.getDevice().getM0Ev1Card();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return m0Ev1Card;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlSerialPort getSerialPort() throws RemoteException {
        if (serialPort == null) {
            checkState();
            try {
                return application.getDevice().getSerialPort();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return serialPort;
        }
    }

    @SuppressLint("NewApi")
    public static IAidSubLcd getSubLcd() throws RemoteException {
        if (iAidSubLcd == null) {
            checkState();
            try {
                return application.getDevice().getSubLcd();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return iAidSubLcd;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlPsamCard getPsamCard() throws RemoteException {
        if (iAidSubLcd == null) {
            checkState();
            try {
                return application.getDevice().getPsamCard();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return iAidlPsamCard;
        }
    }

    @SuppressLint("NewApi")
    public static IAidlUsbSerialPort getUsbSerialPort() throws RemoteException {
        if (iAidlUsbSerialPort == null) {
            checkState();
            try {
                return application.getDevice().getUsbSerialPort();
            } catch (RemoteException e) {
                throw new RemoteException("PinPad service acquisition failed, please try again later.");
            }
        } else {
            return iAidlUsbSerialPort;
        }
    }

    public static void beepPrompt() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    getBeeper().beep(0, 1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void reset() {
        pinpad = null;
        sysHandle = null;
        printer = null;
        emvHandler = null;
        cardReader = null;
        led = null;
        beeper = null;
        m1Card = null;
        camera = null;
        felicaCard = null;
        cpuCard = null;
        m0Ev1Card = null;
        m0CCard = null;
        iAidSubLcd = null;
        iAidlPsamCard = null;

    }


}
