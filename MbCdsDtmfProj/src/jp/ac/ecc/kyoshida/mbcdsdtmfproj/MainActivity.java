
package jp.ac.ecc.kyoshida.mbcdsdtmfproj;

import java.io.IOException;

import org.microbridge.server.AbstractServerListener;
import org.microbridge.server.Server;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnCheckedChangeListener {
    // Create TCP server
    Server server = null;

    // レイアウトされたウィジェットのインスタンス
    private SeekBar mSeekBar1;
    private TextView mTextView1;
    private RadioGroup mRadioGroup1;
    private CheckBox mCheckBox1;
    // DTMFトーン
    ToneGenerator tone;

    // UIスレッドにデータを受け渡すためのハンドラ
    Handler mHandler = new Handler();
    
    int toneNumber;
    int toneNumberPrev ;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // レイアウトされたシークバーとインスタンス mSeekBar1 の結びつけ
        mSeekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        mSeekBar1.setMax(1024);// シークバーの最大値を1024にする
        mTextView1 = (TextView) findViewById(R.id.textView1);

        mRadioGroup1 =(RadioGroup)findViewById(R.id.radioGroup1);
        mRadioGroup1.setOnCheckedChangeListener(this);
        
        mCheckBox1 = (CheckBox)findViewById(R.id.checkBox1);
        
        // ToneGenerator初期化
        tone = new ToneGenerator(android.media.AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);

        // シークバーを動かしたときの処理
        mSeekBar1.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekView, int progress, boolean fromUser) {
                toneDTMF(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        try {
            server = new Server(4567);
            server.start();
        } catch (IOException e) {
            Log.e("microbridge", "Unable to start TCP server", e);
            Toast.makeText(this, "Unable to start TCP server\nPlease retry after a short interval", Toast.LENGTH_LONG).show();
            System.exit(-1);
        }

        // ADB接続からのデータを受信したときの処理
        server.addListener(new AbstractServerListener() {
            @Override
            public void onReceive(org.microbridge.server.Client client, byte[] data) {
                // データ長が1バイト以上あれば表示処理を行う
                if (data.length >= 1) {
                    // 受信したデータをfinal(定数)にして匿名インナークラスに渡るようにする
                    final byte[] buffer = data;

                    mHandler.post(new Runnable() {
                        public void run() {
                            switch (buffer[0]) {
                                case 0x3: // 光センサ・アナログ入力指定コマンド
                                    if (buffer.length >= 3) {
                                        // 2バイトのデータを16ビットに直して0～1023までの値に変換
                                        int sensorValue = ((buffer[1] & 0xff) << 8 | (buffer[2] & 0xff));
                                        toneDTMF(sensorValue);
                                    }
                                    break;
                                case 0x2: //タクトスイッチ・アナログ入力指定コマンド
                                    //オンならばラジオボタンのチェックを変える
                                    if (buffer.length >= 2 && buffer[1] == 0x1) {
                                        switch(mRadioGroup1.getCheckedRadioButtonId()){
                                            case R.id.radio100://百の位なら
                                                mRadioGroup1.check(R.id.radio10);//十の位へ
                                                break;
                                            case R.id.radio10://十の位なら
                                                mRadioGroup1.check(R.id.radio1);//一の位へ
                                                break;
                                            case R.id.radio1://一の位なら
                                                mRadioGroup1.check(R.id.radio100);//百の位へ
                                                break;
                                        }
                                    }
                            }

                        }
                    });

                }
            };

        });

    }

    @Override
    protected void onDestroy() {
        // 終了時にトーンを止める
        tone.stopTone();
        //TCP serverが動作している場合ストップをかける
        if(server.isRunning()){
            Log.d("microbridge","server stop...");
            server.stop();
            Toast.makeText(this, "TCP server stops.", Toast.LENGTH_LONG).show();
        }
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
    }

    //シークバーの値またはセンサーの値に応じてDTMFトーンを鳴らすメソッド
    private void toneDTMF(int sensorValue) {
        mSeekBar1.setProgress(sensorValue);

        //ラジオボタンに応じて百の位、十の位、一の位の整数値を取り出す
        switch(mRadioGroup1.getCheckedRadioButtonId()){
            case R.id.radio1:
                toneNumber = sensorValue % 10;//一の位
                break;
            case R.id.radio10:
                toneNumber = (sensorValue % 100)  / 10;//十の位
                break;
            case R.id.radio100:
                toneNumber = (sensorValue % 1000)  / 100;//百の位
                break;
        }
        mTextView1.setText("Slide=" + sensorValue
                + "\nPushTone Number=" + (toneNumber));
        
        if(mCheckBox1.isChecked() == true){
            //連続音のチェックが入っていれば、トーンの値が変わらない限り音を変えない
            if( toneNumber != toneNumberPrev){
                tone.startTone(toneNumber);
            }
        }else{//毎回音を鳴らす
            tone.startTone(toneNumber);
        }
        toneNumberPrev = toneNumber;   
    }
}
