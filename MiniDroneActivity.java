package e.junki.drone_test2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

public class MiniDroneActivity extends AppCompatActivity {
    private static final String TAG = "MiniDroneActivity";
    /**MiniDroneクラス*/
    private MiniDrone mMiniDrone;
    /**接続・切断ダイアログ*/
    private ProgressDialog mConnectionProgressDialog;

    private TextView mMessageLabel;
    private Button mTakeOffOrLanButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minidrone);
        initializeView();
        //前画面から接続するDroneのARDiscoveryDeviceServiceを取得する
        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);
        //ARDiscoveryDeviceServiceを使いMiniDroneをインスタンス化する。
        mMiniDrone = new MiniDrone(this, service);
        //Drone接続状況やDrone飛行状況をコールバックするリスナーを設定する。
        mMiniDrone.addListener(mMiniDroneListener);
    }

    private void initializeView() {
        //ログ用のTextViewをインスタンス化
        mMessageLabel = (TextView) findViewById(R.id.textMessage);
        //離着陸ボタンをインスタンス化
        mTakeOffOrLanButton = (Button) findViewById(R.id.buttonLandOrTakeOff);
        //離着陸ボタンのクリックリスナーを設定
        mTakeOffOrLanButton.setOnClickListener(takeOnOffOnClickListener);
        // 上昇ボタン
        findViewById(R.id.buttonGazUp).setOnTouchListener(gazUpOnTouchListener);
        // 下降ボタン
        findViewById(R.id.buttonGazDown).setOnTouchListener(gazDownOnTouchListener);
        // 右旋回ボタン
        findViewById(R.id.buttonYawRight).setOnTouchListener(yawRightListener);
        // 左旋回ボタン
        findViewById(R.id.buttonYawLeft).setOnTouchListener(yawLeftOnTouchListener);
        // 前方移動ボタン
        findViewById(R.id.buttonForward).setOnTouchListener(forwardOnTouchListener);
        // 後方移動ボタン
        findViewById(R.id.buttonBack).setOnTouchListener(backOnTouchListener);
        // 右方移動ボタン
        findViewById(R.id.buttonRollRight).setOnTouchListener(rollRightOnTouchListener);
        // 左方移動ボタン
        findViewById(R.id.buttonRollLeft).setOnTouchListener(rollLeftOnTouchListener);
    }


    /**
     * 右移動ボタンのリスナー
     */
    private View.OnTouchListener rollRightOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setRoll((byte) 50);
                    mMiniDrone.setFlag((byte) 1);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setRoll((byte) 0);
                    mMiniDrone.setFlag((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 左移動ボタンのリスナー
     */
    private View.OnTouchListener rollLeftOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setRoll((byte) -50);
                    mMiniDrone.setFlag((byte) 1);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setRoll((byte) 0);
                    mMiniDrone.setFlag((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 前方移動ボタンのリスナー
     */
    private View.OnTouchListener forwardOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setPitch((byte) 50);
                    mMiniDrone.setFlag((byte) 1);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setPitch((byte) 0);
                    mMiniDrone.setFlag((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 後方移動ボタンのリスナー
     */
    private View.OnTouchListener backOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setPitch((byte) -50);
                    mMiniDrone.setFlag((byte) 1);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setPitch((byte) 0);
                    mMiniDrone.setFlag((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 上昇ボタンのリスナー
     */
    private View.OnTouchListener gazUpOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setGaz((byte) 50);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setGaz((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 下降ボタンのリスナー
     */
    private View.OnTouchListener gazDownOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setGaz((byte) -50);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setGaz((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 右旋回ボタンのリスナー
     */
    private View.OnTouchListener yawRightListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setYaw((byte) 50);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setYaw((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };


    /**
     * 左旋回ボタンのリスナー
     */
    private View.OnTouchListener yawLeftOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    mMiniDrone.setYaw((byte) -50);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    mMiniDrone.setYaw((byte) 0);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * 離着陸ボタンのリスナー
     */
    private View.OnClickListener takeOnOffOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (mMiniDrone.getFlyingState()) {
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    appendMessage("離陸命令");
                    //着陸命令を実行
                    mMiniDrone.takeOff();
                    break;
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    //離陸命令を実行
                    appendMessage("着陸命令");
                    mMiniDrone.land();
                    break;
                default:
            }
        }
    };

    private void appendMessage(String message) {
        mMessageLabel.append(message);
        mMessageLabel.append("\n");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // MiniDroneがインスタンス化できている、MiniDroneが接続状態でない場合は接続する。
        if ((mMiniDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mMiniDrone.getConnectionState()))) {
            //ダイアログを設定する
            mConnectionProgressDialog = new ProgressDialog(this);
            mConnectionProgressDialog.setIndeterminate(true);
            //ダイアログのメッセージをConnecting ...にする
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            //ダイアログを表示する
            mConnectionProgressDialog.show();
            //ドローンの接続要求の成否を判定する
            if (!mMiniDrone.connect()) {
                //ドローンの接続要求に失敗した場合はActivityを終了する
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mMiniDrone != null) {
            //ダイアログの設定する
            mConnectionProgressDialog = new ProgressDialog(this);
            mConnectionProgressDialog.setIndeterminate(true);
            //ダイアログのメッセージをDisconnecting ...にする
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();
            //ドローンの切断要求の成否を判定する
            if (!mMiniDrone.disconnect()) {
                //ドローンの切断要求に失敗した場合はActivityを終了する
                finish();
            }
        } else {
            finish();
        }
    }

    private final MiniDrone.Listener mMiniDroneListener = new MiniDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state) {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    Log.d("Drone" , "ARCONTROLLER_DEVICE_STATE_RUNNING");
                    mConnectionProgressDialog.dismiss();
                    break;
                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    break;
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    break;
                default:
            }
        }
    };
}