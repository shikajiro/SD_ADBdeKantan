===
このソースコードは吉田研一氏の許可を得て、公開しています。
http://act-yoshiken.blogspot.jp/p/microbridge.html
===

「ADB接続でかんたん フィジカルコンピューティング」掲載のソースコード配布

目次

0.はじめに
1.使い方
 1.1.解凍後の「src_SD_kantan」フォルダのディレクトリ一覧
 1.2.Androidプロジェクト
 1.3.Arduinoスケッチ
 1.4.必要なハードウェア
2.各プロジェクト・フォルダの説明

本文

0.はじめに
このフォルダは、技術評論社「Software Design」2012年10・11月号掲載の「ADB接続でかんたん フィジカルコンピューティング」のソースコード配布のためのフォルダです。本記事に掲載されたAndroidアプリのソース5個とArduinoスケッチ1個が入っています。


1.使い方

1.1.解凍後の「src_SD_kantan」フォルダのディレクトリ一覧

まずは、以下(1)〜(6)のフォルダの有無を確認してください（アルファベット順です）。
(1)MbBalusProj
(2)MbCdsDtmfProj
(3)MbDaioProj
(4)MbDaioSketchWithBalus
(5)MbShakeLedProj
(6)MbTalkClockProj

1.2.Androidプロジェクト

(4)以外の(1)から(3),(5)と(6)は、Androidプロジェクトです。
Eclipseからインポートしてください。

1.3.Arduinoスケッチ
(4)はArduino 1.0.1プロジェクトです。
Arduino 1.0.1から「MbDaioSketchWithBalus.ino」ファイルを開いてください。

1.4.必要なハードウェア
上記アプリは、デジタル・アナログ入出力のための接続をすませたブレッドボード基板とLEDなどの電子部品が必要です。「Software Design」2012年11月号の「ADB接続でかんたん フィジカルコンピューティング[後編]」に電子部品の入手リストと配線図＆手順が掲載されていますので、そちらを参考にしてください。
なお、なるべくAndroid単体でもアプリの動作確認は可能なように作っています。ご確認下さい。


2.各プロジェクト・フォルダの説明

各プロジェクトの説明は以下のとおりです（「」内はアプリ名です）。各アプリのもう少し詳しい紹介は「Software Design」2012年11月号の「ADB接続でかんたん フィジカルコンピューティング[前編]」をご覧ください。

(1)MbBalusProj「Mbバルス」
…Google音声認識を使って「バルス」と叫ぶとLEDが光るアプリ

(2)MbCdsDtmfProj「Mb光DTMFテルミン」
…光センサ（CdSセル）の光量に応じてプッシュトーンが鳴るテルミンのようなアプリ

(3)MbDaioProj「MbDaio」
…デジタル・アナログ入出力確認用アプリ

(4)MbDaioSketchWithBalus
…Arduino側スケッチです。
Android側の(1)から(3),(5)(6)のアプリすべてに対応するよう１つにまとめています。

(5)MbShakeLedProj「MbシェイクLチカ」
…Android本体をシェイクすることでLEDの輝度が変化するアプリ

(6)MbTalkClockProj「Mbトーククロック」
…タクトスイッチを押すと、現在時間を読み上げるアプリです。日本語で読み上げる場合はあらかじめGoogle Playから「N2 TTS」をインストールしておいてください。
