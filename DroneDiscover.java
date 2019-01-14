package e.junki.drone_test2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.ArrayList;
import java.util.List;

public class DroneDiscover {

    private static final String TAG = "DroneDiscoverer";
    /**
     * 発見したドローンリストが更新されたときに呼ばれるコールバックリスナー
     */
    public interface Listener {

        /**
         * 周囲に存在するドローンの情報(ARDiscoveryDeviceService)リストを
         * 返却する。
         *
         * @param dronesList 周囲に存在するドローンの情報(ARDiscoveryDeviceService)リストを
         */
        void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList);
    }
    private final Context mContext;
    private final List<Listener> mListeners;
    private final List<ARDiscoveryDeviceService> mMatchingDrones;
    private final ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;
    private boolean mStartDiscoveryAfterConnection;
    private ServiceConnection mServiceConnection;
    private ARDiscoveryService mArDiscoveryService;

    public DroneDiscover(Context context) {
        mContext = context;
        mListeners = new ArrayList<>();
        mMatchingDrones = new ArrayList<>();
        //レシーバーの作成
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(mDiscoveryListener);
    }
    /**
     * コールバックインターフェースの追加する
     *
     * @param listener コールバックインターフェース
     */
    public void addListener(Listener listener) {
        Log.i(TAG, "コールバックインターフェース、リスナー!!!");
        mListeners.add(listener);
        notifyServiceDiscovered(mMatchingDrones);
    }
    /**
     * コールバックインターフェースの削除をする
     *
     * @param listener コールバックインターフェース
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * クラスのセットアップをする
     */
    public void setup() {
        Log.i(TAG, "セットアップ開始!!!");
        // ARDiscoveryService(ブロードキャストレシーバー)を登録する
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mContext);
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver,
                new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
        // ServiceConnectionを使ってServiceのライフサイクルコールバックを受け取る
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    // 起動したServiceのインスタンスを取得する。
                    mArDiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                    if (mStartDiscoveryAfterConnection) {
                        //検知を開始する。
                        Log.i(TAG, "検知開始!!!");
                        startDiscovering();

                        mStartDiscoveryAfterConnection = false;
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mArDiscoveryService = null;
                }
            };
        }
        //Serviceが起動していない場合は起動する
        if (mArDiscoveryService == null) {
            // ARDiscoveryServiceを起動する。
            Intent i = new Intent(mContext, ARDiscoveryService.class);
            mContext.bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }
    /**
     * 後始末をする
     */
    public void cleanup() {
        //検知を止める
        stopDiscovering();
        if (mArDiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Serviceを止める
                    mArDiscoveryService.stop();
                    mContext.unbindService(mServiceConnection);
                    mArDiscoveryService = null;
                }
            }).start();
        }
        // ARDiscoveryService(ブロードキャストレシーバー)を解除する
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mContext);
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }
    /**
     * ドローン検知機能を有効にする。
     * ドローンが発見されたときは,{@link Listener#onDronesListUpdated(List)}にコールバックする
     */
    public void startDiscovering() {
        if (mArDiscoveryService != null) {
            // ドローン検知Serviceを開始する
            Log.i(TAG, "検知メソッド起動!!!");
            mDiscoveryListener.onServicesDevicesListUpdated();
            mArDiscoveryService.start();
            mStartDiscoveryAfterConnection = false;
        } else {
            mStartDiscoveryAfterConnection = true;
        }
    }
    /**
     * ドローン検知機能を無効にする。
     */
    public void stopDiscovering() {
        if (mArDiscoveryService != null) {
            Log.i(TAG, "Stop discovering");
            mArDiscoveryService.stop();
        }
        mStartDiscoveryAfterConnection = false;
    }
    /**
     * 検知ドローン一覧をコールバックする。
     * @param dronesList 検知ドローン一覧
     */
    private void notifyServiceDiscovered(List<ARDiscoveryDeviceService> dronesList) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDronesListUpdated(dronesList);
        }
    }

    private final ARDiscoveryServicesDevicesListUpdatedReceiverDelegate
            mDiscoveryListener = new ARDiscoveryServicesDevicesListUpdatedReceiverDelegate() {

        @Override
        public void onServicesDevicesListUpdated() {
            if (mArDiscoveryService != null) {
                // 検索済みのドローン一覧を削除
                Log.i(TAG, "検索の中身は"+mMatchingDrones+"。");
                mMatchingDrones.clear();
                Log.i(TAG, "検索済みリストを一度クリア");
                //Service内で保持している検知したドローン一覧を取得する。
                List<ARDiscoveryDeviceService> deviceList = mArDiscoveryService.getDeviceServicesArray();
                Log.i(TAG, "Service内で保持している検知したドローン一覧を取得!!!");
                //検知したドローンが存在した場合
                if (deviceList != null)
                {
                    //検知したドローンを検知したドローン一覧に追加する
                    Log.i(TAG, "device list not null!!!");
                    Log.i(TAG, "検知したドローンリストの中身は"+deviceList+"。");
                    for (ARDiscoveryDeviceService service : deviceList)
                    {
                        mMatchingDrones.add(service);
                    }
                }
                //検知したドローン一覧をコールバックする
                notifyServiceDiscovered(mMatchingDrones);
                Log.i(TAG, "call back drone list!!!");
            }
        }
    };
}
