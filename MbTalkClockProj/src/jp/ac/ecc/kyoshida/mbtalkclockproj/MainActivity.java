
package jp.ac.ecc.kyoshida.mbtalkclockproj;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import org.microbridge.server.AbstractServerListener;
import org.microbridge.server.Server;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener {

    // Create TCP server
    Server server = null;

    // UIスレッドにデータを受け渡すためのハンドラ
    Handler mHandler = new Handler();

    // レイアウトされたテキストビュー用インスタンス
    private TextView mTextView1;

    // テキスト読み上げインスタンス
    private TextToSpeech mTts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // レイアウトされたテキストビューとインスタンス mTextView1 の結びつけ
        mTextView1 = (TextView) findViewById(R.id.textView1);
        mTts = new TextToSpeech(this, this);

        try {
            server = new Server(4567);
            server.start();
            Toast.makeText(this, "TCP server start", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("microbridge", "Unable to start TCP server", e);
            Toast.makeText(this, "Unable to start TCP server\nPlease retry after a short interval", Toast.LENGTH_LONG).show();
            System.exit(-1);
        }
        // ADBシェル接続からのデータを受信したときの処理
        server.addListener(new AbstractServerListener() {
            @Override
            public void onReceive(org.microbridge.server.Client client, byte[] data) {
                // データ長が1バイト以上あれば表示処理を行う
                if (data.length >= 1) {
                    // 受信したデータをfinal定数にして匿名インナークラスに渡るようにする
                    final byte[] buffer = data;

                    mHandler.post(new Runnable() {
                        public void run() {
                            switch (buffer[0]) {
                                case 0x2: // タクトスイッチ・オン／オフ入力指定コマンド(8)
                                    if (buffer.length >= 2) {
                                        // 1ならばタクトスイッチONなので時刻を読み上げる
                                        if (buffer[1] == 1)
                                            sendSpeak();
                                    }
                                    break;
                            }

                        }
                    });

                }
            };

        });

    }

    @Override
    public void onInit(int status) {
        // TTSの初期化
        if (status == TextToSpeech.SUCCESS) {
            int result = mTts.setLanguage(Locale.JAPAN);// 日本語の指定
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 言語（日本語）が使えない
                result = mTts.setLanguage(Locale.ENGLISH);// 英語の指定
            }
        } else {
            // 初期化失敗
        }

    }

    @Override
    protected void onDestroy() {
        // TTS終了時にShutdown()を入れる必要がある
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        //TCP serverが動作している場合ストップをかける
        if(server.isRunning()){
            Log.d("microbridge","server stop...");
            server.stop();
            Toast.makeText(this, "TCP server stops.", Toast.LENGTH_LONG).show();
        }      
        super.onDestroy();
    }

    public void onClickButton(View view) {
        // MicroBridge未接続時のテスト用ボタン
        sendSpeak();
    }

    String[] message = {"元気出していこう。","頑張ってください！","気楽に行きましょうー","明日から本気出す！"};
    Random rnd =new Random();
    
    private void sendSpeak() {
        // 現在時刻を喋らせる
        Calendar now = Calendar.getInstance();
        int am_pm = now.get(Calendar.AM_PM);
        int h = now.get(Calendar.HOUR); // 時を取得
        int m = now.get(Calendar.MINUTE); // 分を取得
        int s = now.get(Calendar.SECOND); // 秒を取得
        String hhmmss = ((am_pm == 0) ? "午前" : "午後") + h + "時" + m + "分" + s + "秒です！\n";

        int random = rnd.nextInt(message.length);
        hhmmss += message[random];
        
        mTts.speak(hhmmss, TextToSpeech.QUEUE_FLUSH, null);

        mTextView1.setText(hhmmss);

        Log.d("microbridge", hhmmss);

    }

}
