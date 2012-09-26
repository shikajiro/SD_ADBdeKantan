package jp.ac.ecc.kyoshida.mbdaioproj;

import java.io.IOException;
import org.microbridge.server.AbstractServerListener;
import org.microbridge.server.Server;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
  // TCP serverの生成
  Server server = null;
  // UIスレッドにデータを受け渡すためのハンドラ
  Handler mHandler = new Handler();

  // デジタル出力用トグルボタンのインスタンス
  private ToggleButton mToggleButton1;
  // アナログ出力用シークバーのインスタンス
  private SeekBar mSeekBar1;
  // デジタル入力用テキストビューのインスタンス
  private TextView mTextView1;
  // アナログ入力用テキストビューのインスタンス
  private TextView mTextViewLight1; // パーセント表示
  private TextView mTextViewLight2; // アナログ量表示

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // layout/activity_main.xmlの画面レイアウトをセット
    setContentView(R.layout.activity_main);
    // レイアウトされたウィジェットとインスタンスの結びつけ
    mToggleButton1 = (ToggleButton) findViewById(R.id.toggleButton1);
    mSeekBar1 = (SeekBar) findViewById(R.id.seekBar1);
    mSeekBar1.setMax(255);// シークバーの最大値を255にする
    mTextView1 = (TextView) findViewById(R.id.textView1);
    mTextViewLight1 = (TextView) findViewById(R.id.textViewLight1);
    mTextViewLight2 = (TextView) findViewById(R.id.textViewLight2);

    try {
      server = new Server(4567);
      server.start();
      Toast.makeText(this, "TCP server start", Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      Log.e("microbridge", "Unable to start TCP server", e);
      Toast.makeText(this, "Unable to start TCP server\nPlease retry after a short interval", Toast.LENGTH_LONG).show();
      System.exit(-1);
    }

    // トグルボタンを押したときの処理
    mToggleButton1.setOnCheckedChangeListener(
        new OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(
              CompoundButton buttonView, boolean isChecked) {
            // デジタル出力の処理
            byte command = (byte) 0x0; // LEDオン・オフ出力指定コマンド
            // トグルボタンが押されたら0x1そうでなければ0x0
            byte value = (byte) (isChecked ? 0x1 : 0x0);
            sendCommand(command, value); // 送信処理へ
          }
        });

    // シークバーを動かしたときの処理
    mSeekBar1.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(
              SeekBar seekView, int progress, boolean fromUser) {
            // アナログ(PWM)出力処理
            byte command = (byte) 0x1; // LED PWM出力指定コマンド
            byte value = (byte) progress; // SeekBarをスライドした量をセット
            sendCommand(command, value); // 送信処理へ
          }
          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
          }
          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
          }
        });

    // ADB接続からのデータを受信したときの処理
    server.addListener(new AbstractServerListener() {
      @Override
      public void onReceive(
          org.microbridge.server.Client client, byte[] data) {
        // データ長が1バイト以上あれば表示処理を行う
        if (data.length >= 1) {
          // 受信したデータをfinal定数にして匿名メソッドに渡るようにする
          final byte[] buffer = data;
          // ハンドラを用いてUIスレッドに表示処理をさせる
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              switch (buffer[0]) {
                case 0x2: // タクトスイッチ・オン/オフ入力指定コマンド
                  if (buffer.length >= 2) {
                    // 1ならばONを表示0ならばOFFを表示
                    if(buffer[1] != 0){
                      mTextView1.setText("ON");
                      mTextView1.setBackgroundColor(Color.RED);
                    }else{
                      mTextView1.setText("OFF");
                      mTextView1.setBackgroundColor(Color.BLACK);
                    }
                  }
                  break;
                case 0x3: // 光センサ・アナログ入力指定コマンド
                  if (buffer.length >= 3) {
                    //バイト配列の2つの要素を上位8ビット・下位8ビットとして1つの値にまとめる
                    int val = ((buffer[1] & 0xff) << 8 | (buffer[2] & 0xff));
                    mTextViewLight1.setText((val / 1023.0 * 100.0) + "%");
                    mTextViewLight2.setText(val + "/1023");
                    
                  }
                  break;
                default:
                  break;
              }
            }
          });
        }
      };
    });
  }

  // 出力：Androidアプリ（USBアクセサリ）-> Arduino（USBホスト）
  private void sendCommand(byte command, byte value) {
    byte[] buffer = new byte[3];
    switch (command) {
      case 0x0:// デジタル出力をバイト配列にセット(6)
        buffer[0] = 0x0;// LEDオン・オフ出力指定コマンド
        buffer[1] = value;// 0x1 LEDオン 0x0 LEDオフ
        break;
      case 0x1:// アナログ(PWM)出力をバイト配列にセット
        buffer[0] = 0x1;// LED PWM出力指定コマンド
        buffer[1] = value;// 明るさ0～255
        break;
    }
    try {
      server.send(buffer);
      Log.d("microbridge", "buffer=" + buffer[0] + "," + buffer[1]);

    } catch (IOException e) {
      Log.e("microbridge", "problem sending TCP message", e);
    }
  }

  @Override
  protected void onDestroy() {
    // TCP serverが動作している場合ストップをかける
    if (server.isRunning()) {
      Log.d("microbridge", "server stop...");
      server.stop();
      Toast.makeText(this, "TCP server stop", Toast.LENGTH_SHORT).show();
    }
    super.onDestroy();
  }
}
