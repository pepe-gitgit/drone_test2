package e.junki.drone_test2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = ";MainActivity";
    public static final String EXTRA_DEVICE_SERVICE = "EXTRA_DEVICE_SERVICE";
    public DroneDiscover mDroneDiscoverer;
    /**
     * 探査済みドローンリスト
     */
    private final List<ARDiscoveryDeviceService>mDronesList = new ArrayList<>();
    static {
        ARSDK.loadSDKLibs();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        Log.i(TAG, "list viewを設定!!!");
        final ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // リストが選択されたときの処理
                Intent intent = null;
                ARDiscoveryDeviceService service = (ARDiscoveryDeviceService) mAdapter.getItem(position);
                ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                switch (product) {
                    case ARDISCOVERY_PRODUCT_MINIDRONE:
                    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_BRICK:
                    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_LIGHT:
                        intent = new Intent(MainActivity.this, MiniDroneActivity.class);
                        break;
                    case ARDISCOVERY_PRODUCT_ARDRONE:
                    case ARDISCOVERY_PRODUCT_BEBOP_2:
                        break;
                    case ARDISCOVERY_PRODUCT_SKYCONTROLLER:
                        break;
                    case ARDISCOVERY_PRODUCT_JS:
                    case ARDISCOVERY_PRODUCT_JS_EVO_LIGHT:
                    case ARDISCOVERY_PRODUCT_JS_EVO_RACE:
                        break;
                    default:
                        Log.e(TAG, "The type " + product + " is not supported by this sample");
                }
                if (intent != null) {
                    intent.putExtra(EXTRA_DEVICE_SERVICE, service);
                    startActivity(intent);
                }
            }
        });
        //ドローン探索機能クラスをインスタンス化
        mDroneDiscoverer = new DroneDiscover(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResumeでセットアップ、検知時のコールバック、探索機能の準備開始");
        // ドローン探索機能をセットアップする。
        mDroneDiscoverer.setup();
        // ドローン探索機能クラスが周囲のドローンを検知したときのコールバックを登録する。
        mDroneDiscoverer.addListener(mDiscovererListener);
        // ドローン探索機能を開始する。
        mDroneDiscoverer.startDiscovering();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ドローン探索機能を停止する。
        mDroneDiscoverer.stopDiscovering();
        // ドローン探索機能の後処理をする。
        mDroneDiscoverer.cleanup();
        // ドローン探索機能クラスが周囲のドローンを検知したときのコールバックを解除する。
        mDroneDiscoverer.removeListener(mDiscovererListener);
    }

    /**
     * 探知済みドローンリストが更新されたときのコールバックリスナー
     */
    private final DroneDiscover.Listener mDiscovererListener = new DroneDiscover.Listener() {
        @Override
        public void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList) {
            //探知済みドローンリストの更新
            mDronesList.clear();
            mDronesList.addAll(dronesList);
            //ListViewの更新
            mAdapter.notifyDataSetChanged();
            Log.i(TAG, "discovered drone list updated !!!");
        }
    };

    /**
     * ListView用のViewHolder
     */
    static class ViewHolder {
        public TextView text;
    }

    /**
     * ListView用のAdapter
     */
    private final BaseAdapter mAdapter = new BaseAdapter()
    {
        @Override
        public int getCount()
        {
            return mDronesList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mDronesList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                LayoutInflater inflater = getLayoutInflater();
                rowView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = (TextView) rowView.findViewById(android.R.id.text1);
                rowView.setTag(viewHolder);
            }
            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            ARDiscoveryDeviceService service = (ARDiscoveryDeviceService) getItem(position);
            holder.text.setText(service.getName() + " on " + service.getNetworkType());
            return rowView;
        }
    };
}