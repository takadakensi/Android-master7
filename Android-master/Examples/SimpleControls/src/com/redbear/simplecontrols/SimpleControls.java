//BLE Shield
//Address: FE:53:A0:06:4B:2C
//Local Name: BLE Shield

package com.redbear.simplecontrols;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.System;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.text.format.Time;

public class SimpleControls extends Activity {
    private final static String TAG = SimpleControls.class.getSimpleName();

    private Button connectBtn = null;
    private TextView rssiValue = null;
    private TextView AnalogInValue = null;
    private ToggleButton digitalOutBtn, digitalInBtn, AnalogInBtn;
    private SeekBar servoSeekBar, PWMSeekBar;

    //Question Page
    private TextView question = null;
    private Button button1 = null;
    private Button button2 = null;
    private Button button3 = null;
    private TextView result = null;

    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;

    private int qCategory = 0;
    private int qNum = 0;
    private int[] total = new int[3];
    private int count_yes = 0;

    private byte[] data = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    private int[] count = {0, 0, 0, 0, 0, 0};
    private boolean[] touchFlag = {false, false, false, false, false, false};
    private long touchTime = 0; //msec
    private long touchDelay = 10000; //msec


    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

                readAnalogInValue(data);
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Toast.makeText(this, "こんにちは！", Toast.LENGTH_LONG).show();

        Toast.makeText(this, "ロボットとの通信を開始します！", Toast.LENGTH_LONG).show();

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

        rssiValue = (TextView) findViewById(R.id.rssiValue);

        AnalogInValue = (TextView) findViewById(R.id.AIText);

        digitalInBtn = (ToggleButton) findViewById(R.id.DIntBtn);

        connectBtn = (Button) findViewById(R.id.connect);
        connectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (scanFlag == false) {
                    // ********** connect to BLE Device **********

                    mBluetoothLeService.connect("FE:53:A0:06:4B:2C");
                    scanFlag = true;

                    /*
					scanLeDevice();
					Timer mTimer = new Timer();
					mTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							if (mDevice != null) {
								mDeviceAddress = mDevice.getAddress();
								mBluetoothLeService.connect(mDeviceAddress);
								scanFlag = true;
							} else {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast toast = Toast
												.makeText(
														SimpleControls.this,
														"Couldn't search Ble Shiled device!",
														Toast.LENGTH_SHORT);
										toast.setGravity(0, 0, Gravity.CENTER);
										toast.show();
									}
								});
							}
						}
					}, SCAN_PERIOD);
					*/
                }

                System.out.println(connState);
                if (connState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }
            }
        });

        digitalOutBtn = (ToggleButton) findViewById(R.id.DOutBtn);
        digitalOutBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                byte buf[] = new byte[] { (byte) 0x01, (byte) 0x00, (byte) 0x00 };

                if (isChecked == true)
                    buf[1] = 0x01;
                else
                    buf[1] = 0x00;

                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        });

        AnalogInBtn = (ToggleButton) findViewById(R.id.AnalogInBtn);
        AnalogInBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                byte[] buf = new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00 };

                if (isChecked == true) {
                    buf[1] = 0x01;
                    Toast.makeText(getApplicationContext(), "具合の悪い部分に触って！",
                            Toast.LENGTH_SHORT).show();

                }else {
                    buf[1] = 0x00;
                }
                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);

            }


        });

        servoSeekBar = (SeekBar) findViewById(R.id.ServoSeekBar);
        servoSeekBar.setEnabled(false);
        servoSeekBar.setMax(180);
        servoSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                byte[] buf = new byte[] { (byte) 0x03, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) servoSeekBar.getProgress();

                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        });

        PWMSeekBar = (SeekBar) findViewById(R.id.PWMSeekBar);
        PWMSeekBar.setEnabled(false);
        PWMSeekBar.setMax(255);
        PWMSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                byte[] buf = new byte[] { (byte) 0x02, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) PWMSeekBar.getProgress();

                characteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(characteristicTx);
            }
        });

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(SimpleControls.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private void displayData(String data) {
        if (data != null) {
            rssiValue.setText(data);
        }
    }


    private void changeVisibility(int iQuestion, int iButton1, int iButton2, int iButton3, int iResult)
    {
        question.setVisibility(iQuestion);
        button1.setVisibility(iButton1);
        button2.setVisibility(iButton2);
        button3.setVisibility(iButton3);
        result.setVisibility(iResult);
    }

    private void changeText(CharSequence cQuestion, CharSequence cButton1, CharSequence cButton2, CharSequence cButton3, CharSequence cResult)
    {
        question.setText(cQuestion);
        button1.setText(cButton1);
        button2.setText(cButton2);
        button3.setText(cButton3);
        result.setText(cResult);
    }

    private void showQuestion(int questionCategory, int questionNumber)
    {
        int i = 0;
        int res = 0;

        changeVisibility(View.VISIBLE, View.VISIBLE,View.VISIBLE,View.INVISIBLE,View.INVISIBLE);

        if (questionCategory == 0x00) //頭
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("何の前ぶれもなく襲ってきた頭痛である？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\nすでに小さな脳梗塞が起きている恐れもあります。\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                            count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x03) //目
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("何の前ぶれもなく頭痛や肩こりに襲われることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\n\nすでに小さな脳梗塞が起きている恐れもあります。\n\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x04) //耳
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("高熱（39～40度）ありますか？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("からだがだるく、食欲が低下しましたか？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("からだのふしぶしや腰、筋肉が痛いですか？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("せきが出ますか？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("頭痛がしますか","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("のどが痛いですか？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("腹痛や下痢がありますか？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("ふつうのかぜよりも重いと感じますか？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("周囲（職場や学校、家族）でインフルエンザにかかった人がいますか？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("糖尿病や心臓の持病がありますか？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("熱が１週間たっても下がりませんか？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("せきがいつまでも止まらなかったり、むしろ激しくなっていますか？","はい","いいえ","","");
                    break;
                case 12: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=8)

                        changeText("", "", "", "", "かぜやインフルエンザの疑いがおおいにあります。\n" +
                                "すぐに医師の診察を受けましょう。");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "かぜやインフルエンザの疑いがあります。\n" +
                                "医師の診察を受けましょう。\n");
                    else
                        changeText("", "", "", "", "あなたの症状からは、かぜやインフルエンザかどうかわかりません。\n" +
                                "ほかの病気でもその症状はみられます。一度、医師の診察を受けましょう。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x05) //鼻
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("高熱（39～40度）ありますか？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("からだがだるく、食欲が低下しましたか？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("からだのふしぶしや腰、筋肉が痛いですか？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("せきが出ますか？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("頭痛がしますか","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("のどが痛いですか？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("腹痛や下痢がありますか？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("ふつうのかぜよりも重いと感じますか？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("周囲（職場や学校、家族）でインフルエンザにかかった人がいますか？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("糖尿病や心臓の持病がありますか？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("熱が１週間たっても下がりませんか？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("せきがいつまでも止まらなかったり、むしろ激しくなっていますか？","はい","いいえ","","");
                    break;
                case 12: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=8)

                        changeText("", "", "", "", "かぜやインフルエンザの疑いがおおいにあります。\n" +
                                "すぐに医師の診察を受けましょう。");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "かぜやインフルエンザの疑いがあります。\n" +
                                "医師の診察を受けましょう。\n");
                    else
                        changeText("", "", "", "", "あなたの症状からは、かぜやインフルエンザかどうかわかりません。\n" +
                                "ほかの病気でもその症状はみられます。一度、医師の診察を受けましょう。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x07) //肩
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("何の前ぶれもなく襲ってきた肩こりである？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\nすでに小さな脳梗塞が起きている恐れもあります。\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x0A) //喉
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("のどが痛いですか？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("からだがだるく、食欲が低下しましたか？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("からだのふしぶしや腰、筋肉が痛いですか？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("せきが出ますか？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("頭痛がしますか","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("高熱（39～40度）はありますか？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("腹痛や下痢がありますか？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("ふつうのかぜよりも重いと感じますか？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("周囲（職場や学校、家族）でインフルエンザにかかった人がいますか？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("糖尿病や心臓の持病がありますか？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("熱が１週間たっても下がりませんか？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("せきがいつまでも止まらなかったり、むしろ激しくなっていますか？","はい","いいえ","","");
                    break;
                case 12: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=8)

                        changeText("", "", "", "", "かぜやインフルエンザの疑いがおおいにあります。\n" +
                                "すぐに医師の診察を受けましょう。");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "かぜやインフルエンザの疑いがあります。\n" +
                                "医師の診察を受けましょう。\n");
                    else
                        changeText("", "", "", "", "あなたの症状からは、かぜやインフルエンザかどうかわかりません。\n" +
                                "ほかの病気でもその症状はみられます。一度、医師の診察を受けましょう。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x0E) //胸
        {
            switch (questionNumber) {
                case 0:
                    changeText("突然、胸の中央からみずおちにかけて、締めつけられるような激しい痛みがありましたか？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("数週間～数日前からくり返し、胸の痛みや締めつけられる感じや動悸がありましたか？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("吐き気があったり、実際に吐いたりしますか？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("冷や汗や脂汗が出ますか？", "はい", "いいえ", "", "");
                    break;
                case 4:
                    changeText("不整脈（脈の乱れ）がありましたか？", "はい", "いいえ", "  ", "");
                    break;
                case 5:
                    changeText("肥満で高血圧、高脂血症がありますか？", "はい", "いいえ", "", "");
                    break;
                case 6:
                    changeText("たばこはすいますか？", "はい", "いいえ", "", "");
                    break;
                case 7:
                    changeText("最近、食欲が低下しましたか？", "はい", "いいえ", "", "");
                    break;
                case 8:
                    changeText("最近、意識消失（意識朦朧）することがありますか？", "はい", "いいえ", "", "");
                    break;
                case 9:
                    changeText("最近急に息切れ、呼吸困難が出てきましたか？", "はい", "いいえ", "", "");
                    break;
                case 10: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE);

                    if (count_yes >= 4)

                        changeText("", "", "", "", "心筋梗塞の疑いがあります。\n" +
                                "医師の診察を受けましょう。\n");
                    else
                        changeText("", "", "", "", "あなたの症状からは心筋梗塞かどうかわかりません。\n" +
                                "ほかの病気でもその症状はみられます。一度、医師の診察を受けましょう。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x0F) //腹
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("下痢がありますか？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("からだがだるく、食欲が低下しましたか？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("からだのふしぶしや腰、筋肉が痛いですか？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("せきが出ますか？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("頭痛がしますか","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("のどが痛いですか？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("高熱（39～40度）はありますか？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("ふつうのかぜよりも重いと感じますか？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("周囲（職場や学校、家族）でインフルエンザにかかった人がいますか？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("糖尿病や心臓の持病がありますか？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("熱が１週間たっても下がりませんか？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("せきがいつまでも止まらなかったり、むしろ激しくなっていますか？","はい","いいえ","","");
                    break;
                case 12: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=8)

                        changeText("", "", "", "", "かぜやインフルエンザの疑いがおおいにあります。\n" +
                                "すぐに医師の診察を受けましょう。");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "かぜやインフルエンザの疑いがあります。\n" +
                                "医師の診察を受けましょう。\n");
                    else
                        changeText("", "", "", "", "あなたの症状からは、かぜやインフルエンザかどうかわかりません。\n" +
                                "ほかの病気でもその症状はみられます。一度、医師の診察を受けましょう。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x10) //左手
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("何の前ぶれもなく頭痛や肩こりが襲ってくることがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\nすでに小さな脳梗塞が起きている恐れもあります。\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x11) //右手
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("何の前ぶれもなく頭痛や肩こりが襲ってくることがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\nすでに小さな脳梗塞が起きている恐れもあります。\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x12) //左足
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("何の前ぶれもなく頭痛や肩こりが襲ってくることがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\nすでに小さな脳梗塞が起きている恐れもあります。\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }

        if (questionCategory == 0x13) //左手
        {
            switch (questionNumber)
            {
                case 0:
                    changeText("体の片側がしびれたり、手足に力が入らないことがある？", "はい", "いいえ", "", "");
                    break;
                case 1:
                    changeText("高血圧、糖尿病、高脂血症、心疾患のいずれかの持病がある？", "はい", "いいえ", "", "");
                    break;
                case 2:
                    changeText("肥満、または肥満ぎみである？", "はい", "いいえ", "", "");
                    break;
                case 3:
                    changeText("脂っこい食事や塩辛い食べ物が好きで、野菜や果物はあまり食べない？","はい","いいえ","","");
                    break;
                case 4:
                    changeText("定期的に（週2回以上）運動はしていない？","はい","いいえ","  ","");
                    break;
                case 5:
                    changeText("タバコを1日に15本以上吸う？","はい","いいえ","","");
                    break;
                case 6:
                    changeText("週に5日は飲酒し、酔っぱらうこともある？","はい","いいえ","","");
                    break;
                case 7:
                    changeText("何の前ぶれもなく頭痛や肩こりが襲ってくることがある？","はい","いいえ","","");
                    break;
                case 8:
                    changeText("ろれつが回らなかったり、急に言葉が出ないことがある？","はい","いいえ","","");
                    break;
                case 9:
                    changeText("ものが二重に見えたり、片方の視界が暗くなり、一時的にものが見えなくなることがある？","はい","いいえ","","");
                    break;
                case 10:
                    changeText("食べ物が一時的に飲み込めないことがある？","はい","いいえ","","");
                    break;
                case 11:
                    changeText("歩くときに足がもつれたり、つまずいたりすることがある？","はい","いいえ","","");
                    break;
                case 12:
                    changeText("めまいがしたり、立っているとふらつくことがある？","はい","いいえ","","");
                    break;
                case 13:
                    changeText("突然気分が落ち込んだり、うつ状態になったりすることがある？","はい","いいえ","","");
                    break;
                case 14:
                    changeText("自覚的なストレスがある？","はい","いいえ","","");
                    break;
                case 15: //結果画面
                    changeVisibility(View.INVISIBLE, View.INVISIBLE,View.INVISIBLE,View.INVISIBLE,View.VISIBLE);

                    if(count_yes>=6)

                        changeText("", "", "", "", "危険！\n脳梗塞の症状がみられます\n" +
                                "脳梗塞の前兆といわれる「隠れ脳梗塞（一過性虚血発作）」の症状が多くみられます。\nすでに小さな脳梗塞が起きている恐れもあります。\n早めに医療機関を受診することをおすすめします。\n");
                    else if(count_yes>=4)
                        changeText("", "", "", "", "隠れ脳梗塞の危険性あり\n" +
                                "「隠れ脳梗塞（一過性虚血発作）」の症状がみられます。これは、短時間で症状が治まりますが、脳へいく血液の流れが一時的に悪くなって起きる症状。心配な人は、血管の状態を調べる検査を受けることをおすすめします。\n");
                    else
                        changeText("", "", "", "", "いまのところ脳梗塞の危険性は低そうです\n" +
                                "ただし、脳梗塞は高血圧、高脂血症、糖尿病、喫煙などが危険因子といわれ、脳梗塞を起こす可能性は誰にでもあります。予防の第一歩は、禁煙・節酒・塩分や脂肪を控えた食事・適度な運動。規則正しい生活を心がけて。\n");

                    count_yes = 0;
                    break;
                default:
                    break;
            }
        }


    }
    private void readAnalogInValue(byte[] data)
    {
        if (data.length == 2 && data[0] == 0x0A)
        {
            qCategory = data[1];
            qNum = 0;
            int i;
            for (i = 0; i < total.length; i++)
                total[i] = 0;





            setContentView(R.layout.head);
            question = (TextView) findViewById(R.id.myLabel);
            button1 = (Button) findViewById(R.id.myButton01);
            button2 = (Button) findViewById(R.id.myButton02);
            button3 = (Button) findViewById(R.id.myButton03);
            result = (TextView) findViewById(R.id.editText);
            button1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    count_yes++;
                  //  total[0]++;
                    //qCategoryのqNum番の回答がbutton1だった
                    qNum++;
                    //qCategoryのqNumの最大値を超えた場合 -> 結果を表示
                    //else
                    showQuestion(qCategory, qNum);
                }
            });

            button2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                   // total[1]++;
                    qNum++;
                    showQuestion(qCategory, qNum);
                }
            });
            button3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //total[2]++;
                    qNum++;
                    showQuestion(qCategory, qNum);
                }
            });
            showQuestion(data[1], qNum);


        }
        else if (data[0] == 0x0B)
        {
/*
                int Value = 0;
                int pinNum = 0;
                for (int j = 1; j < data.length; j += 2) {
                    Value = ((data[j] << 8) & 0x0000ff00)
                            | (data[j + 1] & 0x000000ff);
                    if (Value > 0) break;
                    pinNum += 1;
                }
*/
                /*
                long nowTime = System.currentTimeMillis();
                if (nowTime >= touchTime + touchDelay) {
                        Toast.makeText(this, "pin" + pinNum, Toast.LENGTH_LONG).show();
                        touchTime = System.currentTimeMillis();
                        count[pinNum] += 1;
                        if (count[pinNum] >= 5) {
                            Toast.makeText(this, "MAX LEVEL (pin" + pinNum + "'s count = " + count[pinNum] + ")", Toast.LENGTH_LONG).show();
                        } else if (count[pinNum] >= 4) {
                            Toast.makeText(this, "MIDDLE LEVEL (pin" + pinNum + "'s count = " + count[pinNum] + ")", Toast.LENGTH_LONG).show();
                        } else if (count[pinNum] >= 3) {
                            Toast.makeText(this, "MIN LEVEL (pin" + pinNum + "'s count = " + count[pinNum] + ")", Toast.LENGTH_LONG).show();
                        }
                }
                else
                    Toast.makeText(this, "wait " + ((touchDelay - (nowTime - touchTime)) / 1000) + "sec", Toast.LENGTH_LONG).show();
                */
/*
                if (Value > 0) {
                    if (touchFlag[pinNum] == false) {
                        Toast.makeText(this, "pin" + pinNum, Toast.LENGTH_LONG).show();
                        touchFlag[pinNum] = true;
                        count[pinNum] += 1;
                        if (count[pinNum] >= 5) {
                            Toast.makeText(this, "MAX LEVEL (pin" + pinNum + "'s count = " + count[pinNum] + ")", Toast.LENGTH_LONG).show();

                        } else if (count[pinNum] >= 4) {
                            Toast.makeText(this, "MIDDLE LEVEL (pin" + pinNum + "'s count = " + count[pinNum] + ")", Toast.LENGTH_LONG).show();

                        } else if (count[pinNum] >= 3) {
                            Toast.makeText(this, "MIN LEVEL (pin" + pinNum + "'s count = " + count[pinNum] + ")", Toast.LENGTH_LONG).show();

                        }
                    }
                }
                else {
                    for (int j = 0; j < touchFlag.length; j++) touchFlag[j] = false;
                }

                AnalogInValue.setText("pin" + pinNum + " = " + Value);
*/
        }
    }

    private void setButtonEnable() {
        flag = true;
        connState = true;

        digitalOutBtn.setEnabled(flag);
        AnalogInBtn.setEnabled(flag);
        servoSeekBar.setEnabled(flag);
        PWMSeekBar.setEnabled(flag);
        connectBtn.setText("Disconnect");
    }

    private void setButtonDisable() {
        flag = false;
        connState = false;

        digitalOutBtn.setEnabled(flag);
        AnalogInBtn.setEnabled(flag);
        servoSeekBar.setEnabled(flag);
        PWMSeekBar.setEnabled(flag);
        connectBtn.setText("Connect");
    }

    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 32, j = 0; i >= 17; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = bytesToHex(serviceUuidBytes);
                    if (stringToUuidString(serviceUuid).equals(
                            RBLGattAttributes.BLE_SHIELD_SERVICE
                                    .toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    @Override
    protected void onStop() {
        super.onStop();

        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
