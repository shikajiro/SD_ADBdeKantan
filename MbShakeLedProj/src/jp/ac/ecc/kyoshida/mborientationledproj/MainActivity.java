
package jp.ac.ecc.kyoshida.mborientationledproj;

import java.io.IOException;
import java.util.List;

import org.microbridge.server.Server;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;

    private float mAccel; // acceleration apart from gravity

    private float mAccelCurrent; // current acceleration including gravity

    private float mAccelLast; // last acceleration including gravity

    private MySurfaceView view;

    // Create TCP server
    Server server = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        // Set full screen view, no title
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // そうでなければ新規TCPサーバ接続
        try {
            server = new Server(4567);
            server.start();
            Toast.makeText(this, "TCP server start", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("microbridge", "Unable to start TCP server", e);
            Toast.makeText(this, "Unable to start TCP server\nPlease retry after a short interval", Toast.LENGTH_LONG).show();
            System.exit(-1);
        }

        view = new MySurfaceView(this, server);
        setContentView(view);

        Log.d("microbridge", "onCreate");
    }

    @Override
    protected void onResume() {
        //
        super.onResume();
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() > 0) {
            mSensorManager
                    .registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        }
        Log.d("microbridge", "onResume");
    }


    @Override
    protected void onPause() {
        //
        super.onPause();
        mSensorManager.unregisterListener(this);
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

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // センサーの値が変わったら描画処理へ
            // Shake!!
            float dx = event.values[SensorManager.DATA_X];
            float dy = event.values[SensorManager.DATA_Y];
            float dz = event.values[SensorManager.DATA_Z];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (dx * dx + dy * dy + dz * dz));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter

            if (Math.abs(mAccel) > 2.0) {
                view.onValueChanged();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
    }

    // グラフィックのためのSurfaceViewインラインクラス
    class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {
        private Server server;

        private int screen_width, screen_height;

        private float x, y, r;

        private float sx, sy;// 赤色LED用

        private boolean touchFlag = false;

        Paint paintLetter = new Paint();

        Paint paintTouch = new Paint();

        Paint paintLedPwm = new Paint(); 
        
        private int analogOut = 127;
        private boolean directionFlag = true;
        
        public MySurfaceView(Context context, Server server) {
            super(context);
            getHolder().addCallback(this);

            setWillNotDraw(false);// onDrawを呼び出すようにする。

            if (this.server == null) {
                this.server = server;
            }
            paintLedPwm.setColor(Color.BLUE);
            paintLedPwm.setStyle(Style.FILL);

            paintLetter.setAntiAlias(true);
            paintLetter.setColor(Color.BLACK);
            paintLetter.setTextSize(24);

            paintTouch.setColor(Color.RED);
            paintTouch.setStyle(Style.FILL);
            
            setFocusable(true);
            setFocusableInTouchMode(true);
            this.setOnTouchListener(this);
                        
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            screen_width = width;
//            screen_height = height;
//            x = width / 2.0f;// x軸中心
//            y = height / 2.0f;// y軸中心
//            r = (x < y ? x : y);// スクリーンの高さ・幅の小さい方の半分を半径の最大値とする
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        void onValueChanged() {

            Log.d("microbridge", "r=" + r + " screenWidth=" + screen_width + " screenHeight="
                    + screen_height);
            if(directionFlag){
                analogOut += 5;
            }else{
                analogOut -= 5;
            }
            if(0 > analogOut) analogOut=0;
            if(255 < analogOut) analogOut=255;

            
            // Arduino側へPWM信号を送出
            try {
                server.send(new byte[] {
                        (byte) 0x1, (byte) analogOut
                });
            } catch (IOException e) {
                Log.e("microbridge", "problem sending TCP message", e);
            }
            invalidate();

        }

        @Override
        public void onDraw(Canvas canvas) {
            x = canvas.getWidth() / 2.0f;// x軸中心
            y = canvas.getHeight() / 2.0f;// y軸中心
            r = (x < y ? x : y);// スクリーンの高さ・幅の小さい方の半分を半径の最大値とする

            canvas.drawColor(Color.WHITE);
            paintLedPwm.setAlpha(analogOut);
            

            float radius = (analogOut/ 255.0f) * r;// 傾き90で最大・傾き0で最小の半径
            
            //canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paintLedPwm);
            canvas.drawCircle(x, y, radius, paintLedPwm);
            
            String caption1 = "出力値=" + analogOut + "(0-255)";
            canvas.drawText( caption1,
                    x - paintLetter.measureText(caption1) / 2.0f,
                    /*y,*/
                    (y - radius > paintLetter.getTextSize()) ? y - radius : paintLetter.getTextSize(),
                    paintLetter);

            String caption2 ="シェイクで" +(directionFlag? "5増加": "5減少");
            canvas.drawText(caption2,
                    x - paintLetter.measureText(caption2) / 2.0f,
                    /*y - paintLetter.getTextSize(),*/
                    y + radius +((y - radius > paintLetter.getTextSize())? paintLetter.getTextSize():  0),
                    paintLetter);
            if (touchFlag) {
                canvas.drawCircle(sx, sy, 100, paintTouch);
            }
        }

        public boolean onTouch(View v, MotionEvent event) {

            sx = event.getX();
            sy = event.getY();
            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        server.send(new byte[] {
                                (byte) 0x0, (byte) 0x1
                        });
                        touchFlag = true;
                        directionFlag = ((directionFlag)? false: true); 
                        break;
                    case MotionEvent.ACTION_UP:
                        server.send(new byte[] {
                                (byte) 0x0, (byte) 0x0
                        });
                        Log.d("microbridge", "sx=" + sx + " sy=" + sy);
                        touchFlag = false;
                        break;
                }
            } catch (IOException e) {
                Log.e("microbridge", "problem sending TCP message", e);
            }

            invalidate();
            return true;
        }

    }


}
