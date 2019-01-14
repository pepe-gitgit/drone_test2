package e.junki.drone_test2;

import android.content.Context;
import android.os.Handler;

import android.support.annotation.NonNull;

import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFeatureMiniDrone;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;


import java.util.ArrayList;
import java.util.List;

public class MiniDrone {
    private static final String TAG = ";MiniDrone";;
    /**
     * ドローンのリスナー
     */
    public interface Listener {

        /**
         * 操作対象のドローンの状態が変化したときのコールバック
         * メインスレッドで呼ばれる
         * 状態一覧
         * ・ARCONTROLLER_DEVICE_STATE_UNKNOWN_ENUM_VALUE:無効な状態(code値:-2147483648)
         * ・ARCONTROLLER_DEVICE_STATE_STOPPED:操作停止状態(code値:0)
         * ・ARCONTROLLER_DEVICE_STATE_STARTING:操作開始状態(code値:1)
         * ・ARCONTROLLER_DEVICE_STATE_RUNNING:操作状態(code値:2)
         * ・ARCONTROLLER_DEVICE_STATE_PAUSED:操作中断状態(code値:3)
         * ・ARCONTROLLER_DEVICE_STATE_STOPPING:操作終了状態(code値:4)
         * ・ARCONTROLLER_DEVICE_STATE_MAX:最大コード(code値:5)
         *
         * @param state 状態
         */
        void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state);

        /**
         * ドローンの飛行状態が変化したときにコールバックされる
         * メインスレッドで呼ばれる
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_UNKNOWN_ENUM_VALUE:無効な状態(code値:-2147483648)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:着陸状態(code値:0)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_TAKINGOFF:離陸状態(code値:1)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:ホバリング状態(code値:2)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:飛行状態(code値:3)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDING:着陸状態(code値:4)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_EMERGENCY:緊急状態(code値:5)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ROLLING:旋回状態(code値:6)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_INIT:初期化状態(code値:7)
         * ・ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_MAX:最大コード(code値:8)
         *
         * @param state ドローンの飛行状態
         */
        void onPilotingStateChanged(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state);
    }

    private final List<Listener> mListeners;

    private final Handler mHandler;

    private ARDeviceController mDeviceController;

    private final Context mContext;

    private ARCONTROLLER_DEVICE_STATE_ENUM mState;

    private ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM mFlyingState;

    private String mCurrentRunId;

    private ARDISCOVERY_PRODUCT_ENUM mProductType;

    private ARDiscoveryDeviceService mDeviceService;

    private static final int DEVICE_PORT = 21;

    /**
     * コンストラクタ
     *
     * @param context       コンテキスト
     * @param deviceService ARDevice情報
     */
    public MiniDrone(Context context, @NonNull ARDiscoveryDeviceService deviceService) {
        mContext = context;
        mDeviceService = deviceService;
        mListeners = new ArrayList<>();
        // コールバックをメインスレッドに返却するためにハンドラーを作る
        mHandler = new Handler(context.getMainLooper());
        // ARデバイスの制御状態
        mState = ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED;
        // プロダクトIDからプロダクトタイプを識別する
        mProductType = ARDiscoveryService.getProductFromProductID(deviceService.getProductID());
        // プロダクトタイプからプロダクト種類を識別する
        ARDISCOVERY_PRODUCT_FAMILY_ENUM family = ARDiscoveryService.getProductFamily(mProductType);
        // このクラスはMINIDRONE種類だけをサポートしているのでそれ以外の場合は終了する
        if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_MINIDRONE.equals(family)) {
            ARDiscoveryDevice discoveryDevice = createDiscoveryDevice(context, deviceService, mProductType);
            if (discoveryDevice != null) {
                Log.i(TAG, ";discoverDevice != null");
                mDeviceController = createDeviceController(discoveryDevice);
                discoveryDevice.dispose();
                Log.i(TAG, ";DeviceService type is supported by MiniDrone");
            }
        }
        else
            {
            Log.i(TAG, ";DeviceService type is not supported by MiniDrone");
        }
    }

    public void dispose()
    {
        if (mDeviceController != null)
            mDeviceController.dispose();
    }

    public void addListener(Listener listener) {
        Log.i(TAG, "リスナー");
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * ドローンに接続する
     *
     * @return 接続要求の成功可否
     * ドローンに接続に成功したからtrueが返却されるわけではないことに注意して下さい。
     * 実際に接続の成否を知るには{@link Listener#onDroneConnectionChanged}を通して知る必要があります。
     */
    public boolean connect() {
        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.start();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * ドローンから切断する
     *
     * @return 切断要求の成功可否
     * ドローンに切断に成功したからtrueが返却されるわけではないことに注意して下さい。
     * 実際に切断の成否を知るには{@link Listener#onDroneConnectionChanged}を通して知る必要があります。
     */
    public boolean disconnect() {
        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.stop();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * 現在の接続状態を返却する
     *
     * @return 現在の接続状態
     */
    public ARCONTROLLER_DEVICE_STATE_ENUM getConnectionState() {
        return mState;
    }

    /**
     * 現在の飛行状態を返却する
     *
     * @return 現在の飛行状態
     */
    public ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getFlyingState() {
        return mFlyingState;
    }

    public void takeOff() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().sendPilotingTakeOff();
        }
    }

    public void land() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().sendPilotingLanding();
        }
    }

    public void setPitch(byte pitch) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDPitch(pitch);
        }
    }

    public void setRoll(byte roll) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDRoll(roll);
        }
    }

    public void setYaw(byte yaw) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDYaw(yaw);
        }

    }
    public void setGaz(byte gaz) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDGaz(gaz);
        }
    }

    /**
     * Take in account or not the pitch and roll values
     * @param flag 1 if the pitch and roll values should be used, 0 otherwise
     */
    public void setFlag(byte flag) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDFlag(flag);
        }
    }

    /**
     * 接続対象製品サービスクラスから接続対象製品機器クラスを作成する
     *
     * @param context                  コンテキスト
     * @param service                  接続対象の製品
     * @param productType              製品種別
     * @return 製品機器
     */
    private ARDiscoveryDevice createDiscoveryDevice(Context context, @NonNull ARDiscoveryDeviceService service, ARDISCOVERY_PRODUCT_ENUM productType) {
        ARDiscoveryDevice device = null;
        try {
            Log.i(TAG, "ARDiscoveryDevice!!!");
            device = new ARDiscoveryDevice(mContext, service);
        } catch (ARDiscoveryException e) {
            Log.e(TAG, "Exception", e);
            Log.e(TAG, "Error: " + e.getError());
        }
        return device;
    }

    /**
     * 接続対象製品機器クラスから接続対象製品機器操作クラスを作成する
     *
     * @param discoveryDevice 接続対象製品機器クラス
     * @return 接続対象製品機器操作クラス
     */
    private ARDeviceController createDeviceController(@NonNull ARDiscoveryDevice discoveryDevice) {
        ARDeviceController deviceController = null;
        try {
            deviceController = new ARDeviceController(discoveryDevice);
            deviceController.addListener(mDeviceControllerListener);
        } catch (ARControllerException e) {
            Log.e(TAG, "Exception", e);
        }
        return deviceController;
    }

    /**
     * 接続対象の機器状態が変わったときにすべてのコールバックリスナーにコールバックする
     *
     * @param state 機器状態
     */
    private void notifyConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDroneConnectionChanged(state);
        }
    }

    /**
     * 接続対象の飛行状態が変わったときにすべてのコールバックリスナーにコールバックする
     *
     * @param state 飛行状態
     */
    private void notifyPilotingStateChanged(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onPilotingStateChanged(state);
        }
    }

    /**
     * 操作対象からのコールバックインターフェースを実装
     */
    private final ARDeviceControllerListener mDeviceControllerListener = new ARDeviceControllerListener() {
        @Override
        public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
            mState = newState;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyConnectionChanged(mState);
                }
            });
        }

        @Override
        public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {
        }

        @Override
        public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
            if (elementDictionary == null) {
                return;
            }
            switch (commandKey) {
                case ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED: {
                    // 飛行状態が更新されたときにコールバックを実施
                    ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                    if (args != null) {
                        final ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureMiniDrone.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mFlyingState = state;
                                notifyPilotingStateChanged(state);
                            }
                        });
                    }
                }
                break;

                case ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED: {
                    // 現在の飛行IDを設定する
                    ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                    if (args != null) {
                        final String runID = (String) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED_RUNID);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCurrentRunId = runID;
                            }
                        });
                    }
                }
                break;
            }
        }
    };
}