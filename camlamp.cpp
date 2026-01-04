#include <Arduino.h>
#include <WiFi.h>
#include "esp_camera.h"
#include <WebServer.h>
#include <WiFiUdp.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#define CAMERA_MODEL_AI_THINKER
#include <camera_pins.h>
#include <WebSocketsClient.h>
#include <WebSocketsServer.h>
#include <base64.h>
#include <Wire.h>
#include <Adafruit_VL53L0X.h>
#include <ESP32Servo.h>

Servo armServo;

#define SERVO_PIN_1 4
#define SERVO_PIN_2 2

// 冷暖光 LED 引脚
#define LED_COLD_PIN 12
#define LED_WARM_PIN 13
#define BLUR_PIN 16

//tof引脚
#define ENABLE_TOF 1
#define TOF_SDA_PIN 15
#define TOF_SCL_PIN 14
Servo myservo1;  // 垂直方向舵机
Servo myservo2;

WebSocketsClient wsClient;
WebServer server(80);
WiFiUDP udp;
HTTPClient http;
WiFiClient client;

const char* ssid = "somebody的iPhone";
const char* password = "20040000";
const char* device_id = "camlamp2";
IPAddress broadcastIP(172,20,10,255);
const int udpPort = 4210;

const int PWM_FREQ=5000;
const int PWM_RES=10;
const int COLD_CH=0;
const int WARM_CH=1;

// 灯光控制参数
int brightness = 80;
int temp       = 4000;
bool autoMode  = true;
int recommendedBrightness = brightness;
int recommendedTemp       = temp;
char fabric[] ="unknown" ;
bool enableBroadcast = true;
bool enableAnnounce  = true;

// ToF 传感器
Adafruit_VL53L0X lox;
unsigned long personDetectedTime = 0;
bool personNearby   = false;
bool usingRecommended = false;
unsigned long transitionStart = 0;

// 定时器
unsigned long lastLightUpdate    = 0;
const unsigned long lightUpdateInterval = 150;
unsigned long lastUp = 0;
bool flowAutoUpload = false;
unsigned long lastFlowUpload = 0;
const unsigned long flowUploadInterval = 5000; // 5秒上传一次

//舵机
int angleV = 0;
int angleH = 180;

// 函数声明
void initCamera();
void handleStatus();
void handleSetLight();
void handleStopBroadcast();
void handleResumeBroadcast();
void handleStopAnnounce();
void handleResumeAnnounce();
void broadcastDevice();
void autoUploadImage();
void UploadImage();
void sendAnnounce();
void onWsEvent(WStype_t type, uint8_t* payload, size_t length);
void applyLightSettings(int br, int tp);
void updateLightingByToF();
void sendStayRecordToServer(unsigned long durationSec, const char* device_id);


void initCamera() {
  camera_config_t config;            

  config.ledc_channel = LEDC_CHANNEL_0;     
  config.ledc_timer   = LEDC_TIMER_0;

  config.pin_d0       = Y2_GPIO_NUM;
  config.pin_d1       = Y3_GPIO_NUM;
  config.pin_d2       = Y4_GPIO_NUM;
  config.pin_d3       = Y5_GPIO_NUM;
  config.pin_d4       = Y6_GPIO_NUM;
  config.pin_d5       = Y7_GPIO_NUM;
  config.pin_d6       = Y8_GPIO_NUM;
  config.pin_d7       = Y9_GPIO_NUM;
  config.pin_xclk     = XCLK_GPIO_NUM;
  config.pin_pclk     = PCLK_GPIO_NUM;
  config.pin_vsync    = VSYNC_GPIO_NUM;
  config.pin_href     = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn     = PWDN_GPIO_NUM;
  config.pin_reset    = RESET_GPIO_NUM;

  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size   = FRAMESIZE_UXGA;

  config.grab_mode    = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location  = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;
  config.fb_count     = 1;

  if (psramFound()) {
    config.jpeg_quality = 10;
    config.fb_count     = 2;
    config.grab_mode    = CAMERA_GRAB_LATEST;
  } else {
    config.frame_size  = FRAMESIZE_SVGA;
    config.fb_location = CAMERA_FB_IN_DRAM;
  }

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed: 0x%x\n", err);
    while (true) delay(1000);
  }
  sensor_t *s = esp_camera_sensor_get();
s->set_whitebal(s, 1);       // ✅ 开启自动白平衡
s->set_awb_gain(s, 1);       // ✅ 白平衡增益
// s->set_wb_mode(s, 1);    
s->set_vflip(s,1);           //上下翻转
s->set_gain_ctrl(s, 1);      // ✅ 自动增益
s->set_exposure_ctrl(s, 1);  // ✅ 自动曝光
s->set_brightness(s, 0);     // ✅ 默认亮度
s->set_contrast(s, 1);       // ✅ 增加对比度
}

// HTTP 接口实现
void handleStatus() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"status\":\"ok\"}");
}
/*
void handleSetLight() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "POST,OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
  if (server.method() == HTTP_OPTIONS) { server.send(204); return; }

  if (!server.hasArg("plain")) {
    server.send(400, "application/json", "{\"error\":\"missing body\"}");
    return;
  }

  DynamicJsonDocument doc(256);
  DeserializationError err = deserializeJson(doc, server.arg("plain"));
if (err) {
  server.send(400, "application/json", "{\"error\":\"invalid json\"}");
  return;
}
  brightness = doc["brightness"] | brightness;
  temp       = doc["temp"]       | temp;
  autoMode   = doc["auto"]       | autoMode;
  recommendedBrightness = doc["recommendedBrightness"] | recommendedBrightness;
  recommendedTemp       = doc["recommendedTemp"]       | recommendedTemp;
  const char* newFabric = doc["fabric"];
if (newFabric && strlen(newFabric) > 0) {
  strcpy(fabric, newFabric);
}
Serial.printf("收到 HTTP 控制: bri=%d, temp=%d, auto=%d, recB=%d, recT=%d, fabric%s \n",
                brightness, temp, autoMode,
                recommendedBrightness, recommendedTemp ,fabric );
  server.send(200, "application/json", "{\"result\":\"OK\"}");
}*/

void handleStopBroadcast() {
  enableBroadcast = false;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"broadcast\":\"stopped\"}");
}

void handleResumeBroadcast() {
  enableBroadcast = true;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"broadcast\":\"resumed\"}");
}

void handleStopAnnounce() {
  enableAnnounce = false;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"announce\":\"stopped\"}");
}

void handleResumeAnnounce() {
  enableAnnounce = true;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"announce\":\"resumed\"}");
}

void broadcastDevice() {
  static unsigned long lastUdp = 0;
  if (!enableBroadcast || millis() - lastUdp < 5000) return;
  lastUdp = millis();

  String msg = String("{\"type\":\"announce\",\"id\":\"") + device_id + "\",\"ip\":\"" + WiFi.localIP().toString() + "\"}";
  udp.beginPacket(broadcastIP, udpPort);
  udp.write((uint8_t*)msg.c_str(), msg.length());
  udp.endPacket();
}

void sendAnnounce() {
  if (!enableAnnounce || WiFi.status() != WL_CONNECTED) return;
  String json = String("{\"id\":\"") + device_id + "\",\"ip\":\"" + WiFi.localIP().toString() + "\"}";
  http.begin(client, "http://110.41.81.4:3000/announce");
  http.addHeader("Content-Type", "application/json");
  int code = http.POST(json);
  if (code > 0) {
    String payload = http.getString();
    if (payload.indexOf("\"added\":true") >= 0) {
      enableAnnounce  = false;
      enableBroadcast = false;
    }
  }
  http.end();
}

// WebSocket 事件
void onWsEvent(WStype_t type, uint8_t* payload, size_t length) {
  if (type == WStype_TEXT) {
    StaticJsonDocument<512> doc;
    if (deserializeJson(doc, payload, length)) return;
    if (doc["type"] == "state" && doc["id"] == device_id) {
      brightness = doc["brightness"] | brightness;
      temp       = doc["temp"]       | temp;
      recommendedBrightness = doc["recommendedBrightness"] | recommendedBrightness;
      recommendedTemp       = doc["recommendedTemp"]       | recommendedTemp;
      autoMode = doc["auto"].as<bool>();
       const char* newFabric = doc["fabric"];
if (newFabric && strlen(newFabric) > 0) {
  strcpy(fabric, newFabric);
}  Serial.printf("从WebSocket收到控制指令：亮度=%d 色温=%d 自动=%d 推荐色温=%d 推荐亮度=%d 面料%s\n", brightness, temp, autoMode, recommendedTemp, recommendedBrightness, fabric);
    }
    else if (doc["type"] == "command") {
    String cmd = doc["cmd"].as<String>();  
    if (cmd == "upload_cloth") {
        digitalWrite(BLUR_PIN, LOW);
        applyLightSettings(50, 4500);
        angleH = 180;
        myservo2.write(angleH);
        angleV = 0;
        myservo1.write(angleV);
        delay(2000);
        Serial.println("收到服装图像上传指令");
        autoUploadImage();
        digitalWrite(BLUR_PIN, HIGH);
    } 
    else if (cmd == "flow_upload") {

        bool enable = doc["enabled"] | false;  
        flowAutoUpload = enable;
        
        Serial.printf("人流自动上传 %s\n", enable ? "开启" : "关闭");
        if (enable) {angleH = 0;
        myservo2.write(angleH);
        angleV = 50;
        myservo1.write(angleV);
        delay(2000);
            lastFlowUpload = 0;  
            UploadImage();
        }
    }
}
else if (doc["type"] == "arm") {  
    String direction = doc["direction"].as<String>();
    Serial.printf("收到机械臂控制指令: %s\n", direction.c_str());

     if (direction == "up") {
        angleV = constrain(angleV - 10, 0, 90);
        myservo1.write(angleV);
      } 
      else if (direction == "down") {
        angleV = constrain(angleV + 10, 0, 90);
        myservo1.write(angleV);
      } 
      else if (direction == "left") {
        angleH = 0;
        myservo2.write(angleH);
      } 
      else if (direction == "right") {
        angleH = 180;
        myservo2.write(angleH);
      }
}
  }
}

// 灯光应用
void applyLightSettings(int br, int tp) {
  tp = constrain(tp, 2700, 6500);
  int tempRatio    = map(tp, 2700, 6500, 0, 255);
  int pwmBrightness = map(br, 0, 100, 0, 255);
  
  int pwmCold      = (tempRatio * pwmBrightness) / 255;
  int pwmWarm      = ((255 - tempRatio) * pwmBrightness) / 255;
  analogWrite(LED_COLD_PIN,255- pwmCold);
  analogWrite(LED_WARM_PIN,255- pwmWarm);
  //Serial.printf("PWM Cold=%d, Warm=%d\n", pwmCold, pwmWarm);
}

//人流
void UploadImage() {
  camera_fb_t* fb = NULL;
for (int i = 0; i < 2; i++) {   // 丢 1 帧，再取 1 帧
  fb = esp_camera_fb_get();
  if (fb) esp_camera_fb_return(fb);
  delay(60);
}
fb = esp_camera_fb_get();
if (!fb) return;
  String meta = String("{\"type\":\"image/jpeg\",\"id\":\"") + device_id + "\"}";
  //wsClient.sendTXT(meta);
  //wsClient.sendBIN(fb->buf, fb->len);
    String url = String("http://110.41.81.4:3000/uploadImageDetect?device=") + device_id;
  http.begin(client, url);
  http.addHeader("Content-Type", "image/jpeg");
  // 直接把 fb->buf 作为 body 发出去
  int httpCode = http.POST(fb->buf, fb->len);
  if (httpCode > 0) {
    Serial.printf("HTTP 上传响应：%d\n", httpCode);
  } else {
    Serial.printf("HTTP 上传失败：%s\n", http.errorToString(httpCode).c_str());
  }
  http.end();
  esp_camera_fb_return(fb);
}

//服装色温
void autoUploadImage() {
  camera_fb_t* fb = NULL;
for (int i = 0; i < 2; i++) {   // 丢 1 帧，再取 1 帧
  fb = esp_camera_fb_get();
  if (fb) esp_camera_fb_return(fb);
  delay(60);
}
fb = esp_camera_fb_get();
if (!fb) return;
  String meta = String("{\"type\":\"image/jpeg\",\"id\":\"") + device_id + "\"}";
  //wsClient.sendTXT(meta);
  //wsClient.sendBIN(fb->buf, fb->len);
    String url = String("http://110.41.81.4:3000/uploadImage?device=") + device_id;
  http.begin(client, url);
  http.addHeader("Content-Type", "image/jpeg");
  // 直接把 fb->buf 作为 body 发出去
  int httpCode = http.POST(fb->buf, fb->len);
  if (httpCode > 0) {
    Serial.printf("HTTP 上传响应：%d\n", httpCode);
  } else {
    Serial.printf("HTTP 上传失败：%s\n", http.errorToString(httpCode).c_str());
  }
  http.end();
  esp_camera_fb_return(fb);
}

void updateLightingByToF() {
  VL53L0X_RangingMeasurementData_t measure;
  lox.rangingTest(&measure, false);

  bool currentNearby = (measure.RangeStatus == 0 && measure.RangeMilliMeter < 2000);
  unsigned long now = millis();
  Serial.printf("测距: %d mm\n", measure.RangeMilliMeter);

  static unsigned long leaveStartTime = 0;  // 记录离开开始时间
  static unsigned long transitionStart = 0;

  if (autoMode) {
    if (currentNearby && !personNearby) {
      personDetectedTime = now;    // 记录靠近开始时间
      personNearby = true;
      transitionStart = now;
      leaveStartTime = 0;
    } 
    else if (!currentNearby && personNearby) {
      if (leaveStartTime == 0) leaveStartTime = now;  // 记录离开开始时间
      if (now - leaveStartTime >= 1000) {             // 离开确认，1秒防抖
        personNearby = false;
        transitionStart = now;

        unsigned long stayDurationMs = now - personDetectedTime;
        unsigned long stayDurationSec = stayDurationMs / 1000;

        Serial.printf("停留时长：%lu 秒\n", stayDurationSec);
        sendStayRecordToServer(stayDurationSec, device_id);

        personDetectedTime = 0;
        leaveStartTime = 0;
      }
    } 
    else {
      // 状态稳定，重置防抖计时器
      if (currentNearby) leaveStartTime = 0;
      else personDetectedTime = 0;
    }
  }

  float ratio = float(now - transitionStart) / 2000.0f;
  if (ratio > 1.0f) ratio = 1.0f;

  int br = brightness;
  int tp = temp;

  if (autoMode) {
    if (personNearby) {
      br = brightness + int((recommendedBrightness - brightness) * ratio);
      tp = temp + int((recommendedTemp - temp) * ratio);
      if (strcmp(fabric, "polyester") == 0) {
      digitalWrite(BLUR_PIN, LOW);}
    } else {
      br = recommendedBrightness + int((brightness - recommendedBrightness) * ratio);
      tp = recommendedTemp + int((temp - recommendedTemp) * ratio);
      digitalWrite(BLUR_PIN, HIGH);
    }
  }

  if (now - lastLightUpdate > lightUpdateInterval) {
    applyLightSettings(br, tp);
    lastLightUpdate = now;
  }
}

void sendStayRecordToServer(unsigned long durationSec, const char* device_id) {
  if (WiFi.status() == WL_CONNECTED) {
    WiFiClient client;
    HTTPClient http;

    http.begin(client, "http://110.41.81.4:3000/duration");
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"id\":\"" + String(device_id) + "\",\"duration\":" + String(durationSec) + "}";

    int httpCode = http.POST(payload);
    Serial.printf("[HTTP] POST 返回码：%d\n", httpCode);

    http.end();
  } else {
    Serial.println("WiFi 未连接，无法发送数据。");
  }
}


/*void onWebSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  if (type == WStype_TEXT) {
    String msg = (const char *)payload;
    Serial.printf("📥 收到控制命令: %s\n", msg.c_str());

    // 解析 JSON 消息
    DynamicJsonDocument doc(128);
    DeserializationError err = deserializeJson(doc, msg);

    if (!err && doc["type"] == "arm") {
      String cmd = doc["command"];

      if (cmd == "up") {
        angleV = constrain(angleV - 10, 0, 180);
        myservo1.write(angleV);
      } 
      else if (cmd == "down") {
        angleV = constrain(angleV + 10, 0, 180);
        myservo1.write(angleV);
      } 
      else if (cmd == "left") {
        angleH = constrain(angleH - 10, 0, 180);
        myservo2.write(angleH);
      } 
      else if (cmd == "right") {
        angleH = constrain(angleH + 10, 0, 180);
        myservo2.write(angleH);
      }

      Serial.printf("垂直角度: %d°, 水平角度: %d°\n", angleV, angleH);
    }
  }
}*/

void setup() {

  pinMode(LED_COLD_PIN, OUTPUT);
  pinMode(LED_WARM_PIN, OUTPUT);
  pinMode(BLUR_PIN, OUTPUT);
  digitalWrite(BLUR_PIN, HIGH);
  Serial.begin(115200);
  while(!Serial) delay(10);
  Serial.println(">>> setup start <<<");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) delay(500);
  Serial.println("WiFi connected");
  
  myservo1.setPeriodHertz(50);  // 标准 50Hz servo
  myservo2.setPeriodHertz(50);

  // 连接舵机引脚（替换为你实际接线的 GPIO）
  myservo1.attach(SERVO_PIN_1);
  myservo2.attach(SERVO_PIN_2); 
  
  // 初始角度
  myservo1.write(angleV);
  myservo2.write(angleH);
  
  wsClient.begin("110.41.81.4", 8081, "/");
  wsClient.onEvent(onWsEvent);

  initCamera();
  server.on("/status",          HTTP_GET,  handleStatus);
  server.on("/setLight",        HTTP_POST, handleSetLight);
  server.on("/stopBroadcast",   HTTP_GET,  handleStopBroadcast);
  server.on("/resumeBroadcast", HTTP_GET,  handleResumeBroadcast);
  server.on("/stopAnnounce",    HTTP_GET,  handleStopAnnounce);
  server.on("/resumeAnnounce",  HTTP_GET,  handleResumeAnnounce);
  server.begin();

#if ENABLE_TOF
  Wire.begin(TOF_SDA_PIN, TOF_SCL_PIN);  
  if (!lox.begin()) {
    Serial.println("❌ ToF 初始化失败！");
  } else {
    Serial.println("✅ ToF 初始化成功");
  }
#endif
}


void loop() {
  server.handleClient();
  wsClient.loop();
  broadcastDevice();
  // 心跳
  static unsigned long lastPing = 0;
  if (millis() - lastPing > 30000) {
    lastPing = millis();
    DynamicJsonDocument doc(64);
    doc["type"] = "ping";
    doc["id"]   = device_id;
    String pingMsg;
    serializeJson(doc, pingMsg);
    wsClient.sendTXT(pingMsg);
  }


static unsigned long lastAnnounce = 0;
  if (millis() - lastAnnounce > 5000) {
    lastAnnounce = millis();
    sendAnnounce();  
  }

  #if ENABLE_TOF
  updateLightingByToF();
#endif
}

