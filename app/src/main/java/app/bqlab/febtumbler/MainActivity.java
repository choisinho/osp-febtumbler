package app.bqlab.febtumbler;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {

    //constants
    final int REQUEST_ENABLE_BLUETOOTH = 0;
    final int REQUEST_DISCOVERABLE = 1;

    //variables
    BluetoothSPP mBluetooth;
    BluetoothAdapter mAdapter;
    NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetooth.stopService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_ENABLE_BLUETOOTH:
                    new AlertDialog.Builder(this)
                            .setMessage("기기의 블루투스를 활성화시키세요.")
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                                }
                            }).show();
                    break;
                case REQUEST_DISCOVERABLE:
                    new AlertDialog.Builder(this)
                            .setMessage("장치를 탐색하기 위해선 사용자의 동의가 필요합니다.")
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setNeutralButton("수동으로 장치 연결", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                                }
                            })
                            .show();
                    break;
            }
        } else {
            switch (requestCode) {
                case BluetoothState.REQUEST_CONNECT_DEVICE:
                    assert data != null;
                    mBluetooth.connect(data);
                    break;
            }
        }
    }

    private void init() {
        mBluetooth = new BluetoothSPP(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        findViewById(R.id.main_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MyService.isConnected) {
                    final EditText e = new EditText(MainActivity.this);
                    e.setInputType(InputType.TYPE_CLASS_NUMBER);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("텀블러 설정")
                            .setMessage("목표 온도를 설정하세요.")
                            .setView(e)
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (MyService.isConnected) {
                                        Toast.makeText(MainActivity.this, "온도를 " + e.getText().toString() + "도로 변경합니다.", Toast.LENGTH_SHORT).show();
                                        mBluetooth.send(e.getText().toString(), true);
                                        MyService.goal = Integer.parseInt(e.getText().toString());
                                    }
                                }
                            })
                            .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("텀블러 설정")
                            .setMessage("장치와 연결되어 있지 않습니다.")
                            .setPositiveButton("연결", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    connectBluetooth();
                                }
                            })
                            .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                }
            }
        });
    }

    private void connectBluetooth() {
        if (!this.mBluetooth.isBluetoothAvailable()) {
            Toast.makeText(this, "지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        } else if (!this.mBluetooth.isBluetoothEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH);
        } else if (!this.mBluetooth.isServiceAvailable()) {
            this.mBluetooth.setupService();
            this.mBluetooth.startService(BluetoothState.DEVICE_OTHER);
            connectBluetooth();
        } else if (mBluetooth.getServiceState() != BluetoothState.STATE_CONNECTED){
            startActivityForResult(new Intent(getApplicationContext(), DeviceList.class), BluetoothState.REQUEST_CONNECT_DEVICE);
            Toast.makeText(this, "연결할 디바이스를 선택하세요.", Toast.LENGTH_LONG).show();
            mBluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
                @Override
                public void onDataReceived(byte[] data, String message) {
                    ((TextView) MainActivity.this.findViewById(R.id.main_temp)).setText(message);
                    MyService.temp = Integer.parseInt(message);
                }
            });
            mBluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                @Override
                public void onDeviceConnected(String name, String address) {
                    Toast.makeText(MainActivity.this, "장치와 연결되었습니다.", Toast.LENGTH_LONG).show();
                    startService(new Intent(MainActivity.this, MyService.class));
                    MyService.isConnected = true;
                }

                @Override
                public void onDeviceDisconnected() {
                    Toast.makeText(MainActivity.this, "장치와 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                    MyService.isConnected = false;
                }

                @Override
                public void onDeviceConnectionFailed() {
                    Toast.makeText(MainActivity.this, "장치와 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                    MyService.isConnected = false;
                }
            });
        }
    }
}
