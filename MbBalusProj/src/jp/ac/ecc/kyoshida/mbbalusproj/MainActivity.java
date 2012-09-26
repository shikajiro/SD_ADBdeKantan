
package jp.ac.ecc.kyoshida.mbbalusproj;

import java.io.IOException;
import java.util.ArrayList;

import org.microbridge.server.Server;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    // Create TCP server
    Server server = null;

    private static final int REQUEST_CODE = 0;

    private Button mButton1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton1 = (Button) findViewById(R.id.button1);
        
        try {
            server = new Server(4567);
            server.start();
        } catch (IOException e) {
            Log.e("microbridge", "Unable to start TCP server", e);
            Toast.makeText(this, "Unable to start TCP server\nPlease retry after a short interval", Toast.LENGTH_LONG).show();
            System.exit(-1);
        }
        
        mButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // ACTION_WEB_SEARCH
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "唱える");
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        //TCP serverが動作している場合ストップをかける
        if(server.isRunning()){
            Log.d("microbridge","server stop...");
            server.stop();
            Toast.makeText(this, "TCP server stops.", Toast.LENGTH_LONG).show();
        }         
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (results.size() > 0) {
                if (results.get(0).equals("バルス")) {
                    byte command = (byte) 0xff;
                    byte value = 0x1;
                    sendCommand(command, value);
                    Toast.makeText(this, "バルス成功！", Toast.LENGTH_LONG).show();
                }else{
                    byte command = (byte) 0xff;
                    byte value = 0x2;
                    sendCommand(command, value);
                    Toast.makeText(this, "バルス失敗！\n"+results.get(0), Toast.LENGTH_LONG).show();                   
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendCommand(byte command, byte value) {
        byte[] buffer = new byte[3];
        buffer[0] = command;
        buffer[1] = value;
        try {
            server.send(buffer);
            Log.d("microbridge", "buffer=" + buffer[0] + "," + buffer[1]);

        } catch (IOException e) {
            Log.e("microbridge", "problem sending TCP message", e);
        }

    }

}
