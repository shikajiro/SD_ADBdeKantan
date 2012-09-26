#include <Adb.h> //MicroBridgeを利用するためのライブラリの読み込み

//定数の指定：デジタル・アナログ入出力のピンを定数に指定
#define  LED      2 // デジタル出力LED用のピンをD2番に指定する
#define  LED_PWM  3 // PWM出力を行うLED用のピンをD3番に指定する
#define  BUTTON   4 // デジタル入力を行うタクトスイッチ用のピンをD4番に指定する
#define  SENSOR  A0 // アナログ入力を行う（CdSセル）用のピンをA0番に指定する

Connection *connection;  // ADBの接続インスタンス
uint32_t lastTime;    // ADC（アナログ／デジタル変換） サンプリングの経過時間
uint8_t b0;       // タクトスイッチのひとつ前の状態を保存する
uint8_t count = 0;// ループカウンタ用
boolean mb_status = false;//MicroBridge通信の有無を示す状態フラグ

void setup()  //最初に一度だけ実行される部分
{
  Serial.begin(57600);//シリアルポートの初期化
  ADB::init();  //ADBサブシステムの初期化
  pinMode(LED, OUTPUT);  //LED用ピンを出力ポートにする
  pinMode(LED_PWM, OUTPUT);//LED(PWM)用ピンを出力ポートにする
  pinMode(BUTTON, INPUT);  //タクトスイッチ用ピンを出力ポートにする
  //アナログ入力ピンのみ初期化は不要
  b0 = digitalRead(BUTTON);//最初のタクトスイッチの状態を保存する
  lastTime = millis();  // 開始時間をセット

  //ADB接続の開始  
  connection = ADB::addConnection("tcp:4567", true, adbEventHandler); 
  // 引数：TCPポート4567番,自動再接続設定,イベントが発生した際のコールバック関数名 
}

void loop()  //繰り返し実行される部分
{
  if ((millis() - lastTime) > 20){  //20ミリ秒以上経っていれば処理
    if(mb_status == false){
      mb_disconnect_loop();//AndroidとMicroBridge接続されていない時に実行されるスケッチ
    }else{
      mb_connect_loop();//AndroidとMicroBridge接続されている時に実行されるスケッチ
    }
    lastTime = millis();  // 現在時間をセット
  }
  //loop()内で定期的にAndroidに向けてポーリング
  ADB::poll();//Androidとの接続・切断・受信があるとコールバック関数を呼び出す
}

//Androidと接続されていない時のloop()処理部分
//Arduino単体で動作するときのloop()スケッチ部分を記述する
void mb_disconnect_loop()
{
  if(digitalRead(BUTTON)){//タクトスイッチが押されたら
    digitalWrite(LED,HIGH);//赤色LEDをオン
  }
  else{
    digitalWrite(LED,LOW);//赤色LEDをオフ
  }
  //CdSセルのアナログ値が0（真っ暗）なら最大輝度になるように
  //アナログ値が255（一番明るい）なら消えるように
  int a = 255-analogRead(SENSOR)/4;
  analogWrite(LED_PWM, a);
}

//AndroidとMicroBridge接続が行われている時のloop()処理部分
//主にデジタル・アナログ入力をAndroidへ送信する処理を記述する
void mb_connect_loop()
{
  uint8_t sendData[3];//Androidへ送信するデータ

  //デジタル入力処理（ArduinoのピンからreadしてAndroidにwriteする）
  uint8_t b = digitalRead(BUTTON);//タクトスイッチからデジタル入力を読み込む
  if(b != b0){ //ボタンのオン・オフが前の状態と異なるときだけ送信
    sendData[0] = 0x2;// タクトスイッチ・オン／オフ入力コマンド
    sendData[1] = b;  // オン／オフのデータ
    connection->write(3, sendData);
    b0 = b; //次の処理のために現在のオン／オフの状態を格納
    //タクトスイッチの現在の状態をシリアルモニターに出力
    Serial.println( "DI:data[]=" + String(sendData[0],HEX) + ", " + String(sendData[1],HEX) );
  }

  //アナログ入力処理（ArduinoのピンからreadしてAndroidにwriteする）
  switch(count++ % 0x10){ // 16(0x10)回に1回行うための処理
  case 0x3: //Cdsセル（光センサー）入力コマンド
    //アナログ値を符号なし16ビット整数値で入力
    uint16_t val = analogRead(A0);
    sendData[0] = 0x3;
    sendData[1] = val >> 8;    //上位8ビットを8ビット右シフトして格納
    sendData[2] = val & 0xff;  //下位8ビットのみビットマスクで取り出して格納
    connection->write(3, sendData); //符号なし8ビット整数値として2バイト送信
    Serial.println("AI:val=" + String(val) + " data[]=" + String(sendData[0],HEX) 
                  + ", " + String(sendData[1],HEX)+ ", " + String(sendData[2],HEX) );
  }
}

// ADB接続時のコールバック関数（イベントハンドラー）
// loop()内のADB::poll()から呼び出される
void adbEventHandler(Connection *connection, adb_eventType event, uint16_t length, uint8_t *recieveData)
{
  switch(event){
  case ADB_CONNECTION_OPEN: 
    Serial.println("event:ADB_CONNECTION_OPEN");
    if(mb_status == false){ //adb接続開始時の初期化部分
      digitalWrite(LED,LOW);
      analogWrite(LED_PWM, 0); 
    } 
    mb_status = true;  //ADB接続をした状態
    break;
  case ADB_CONNECTION_RECEIVE:
    Serial.println("event:ADB_CONNECTION_RECEIVE");
    mb_status = true;  //ADB接続をした状態
    //MicroBridge受信時の処理を呼び出す
    mb_recieve_loop(recieveData);
    break;
  case ADB_CONNECTION_FAILED:
    Serial.println("event:ADB_CONNECTION_FAILED");
    mb_status = false;  //ADB接続をしていない状態
    break;
  default:
    Serial.println("event="+String(event));
    mb_status = false;//ADB接続をしていない状態
    break;
  }
}

//MicroBridge受信時の処理
//主にAndroidから受信したデータをもとにArduinoのデジタル・アナログ出力を行う
void  mb_recieve_loop(uint8_t *recieveData)
{
  Serial.println( "recieveData[]=" + String(recieveData[0],HEX) + ", " + String(recieveData[1],HEX) );
  // 先頭0バイト目がコマンドに応じて処理を振り分ける
  switch(recieveData[0]){
  case 0x0: //LEDオンオフ（デジタル出力）コマンド
    digitalWrite(LED, recieveData[1]? HIGH: LOW);//データに応じてLEDを点灯/消灯する
    Serial.println( "DO:recieveData[]=" + String(recieveData[0],HEX) + ", " + String(recieveData[1],HEX) );
    break;
  case 0x1://LED PWM出力（アナログ出力）コマンド
    if (recieveData[1] >= 0x0 && recieveData[1] <= 0xff ) {//データが0～255ならば処理する
      analogWrite(LED_PWM, recieveData[1]);//LEDをPWM値（0:消～255:明るさ最大）で点灯
      Serial.println( "AO:recieveData[]=" + String(recieveData[0],HEX) + ", " + String(recieveData[1],HEX) );
    }
    break;
  case 0xff://バルス・コマンド
    //バルス成功
    if(recieveData[1] == 0x1) {
      //赤LEDを最初に点滅させる
      digitalWrite(LED, HIGH);
      delay(500);
      digitalWrite(LED, LOW);
      // 青色LEDを徐々に明るくさせる
      for(int i=0; i<256; i++){
        digitalWrite(LED_PWM, i);
        delay(20);
      }
      delay(100);
      //赤LEDを最後に点滅させる
      digitalWrite(LED, HIGH);
      delay(500);
      digitalWrite(LED, LOW);
      //青色LEDを消灯させる
      digitalWrite(LED_PWM, 0);
      uint8_t returnData[] = { 0x1, 0x1 };
      connection->write(2, returnData);
    } else {
      //バルス失敗の時は赤LEDと青LEDを交互に点滅させる
      digitalWrite(LED, HIGH);
      digitalWrite(LED_PWM, LOW);
      delay(500);
      digitalWrite(LED, LOW);
      digitalWrite(LED_PWM, HIGH);
      delay(500);
      digitalWrite(LED, HIGH);
      digitalWrite(LED_PWM, LOW);
      delay(500);
      digitalWrite(LED, LOW);
      digitalWrite(LED_PWM, HIGH);
      delay(500);
      digitalWrite(LED, HIGH);
      digitalWrite(LED_PWM, LOW);
      delay(500);
      digitalWrite(LED, LOW);
      digitalWrite(LED_PWM, LOW);
      uint8_t returnData[] = {
        0x1, 0x2              };
      connection->write(2, returnData);
    }
    break;
  }
}
