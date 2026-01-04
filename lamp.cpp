#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <WebSocketsServer.h>
#include <WebSocketsClient.h>
#include <ESP8266HTTPClient.h>
#include <Wire.h>
#include <Adafruit_VL53L0X.h>
#include <BH1750.h> 
BH1750 lightMeter; 
Adafruit_VL53L0X lox = Adafruit_VL53L0X();
WebSocketsClient webSocket;

unsigned long lastLightSend = 0;
const unsigned long lightSendInterval = 300;  

#define LED_COLD_PIN D2  // 冷光接在 D1(GPIO5)
#define LED_WARM_PIN D1  // 暖光接在 D2(GPIO4)
#define ENABLE_TOF 1
#define BLUR D7

//tof引脚
#define TOF_SDA_PIN D5
#define TOF_SCL_PIN D6

bool bh1750Ready = false;
bool tofReady = false;

unsigned long personDetectedTime = 0;
bool personNearby = false;
bool usingRecommended = false;
const char* ssid = "somebody的iPhone";
const char* password = "20040000";
const char* device_id = "lamp5";  // 每个灯设备设置唯一ID
IPAddress broadcastIP(192, 168, 110, 255); 

unsigned long lastLightUpdate = 0;  // 上次灯光应用时间
const unsigned long lightUpdateInterval = 50;  // 最小灯光刷新间隔（毫秒）

const int udpPort = 4210;
bool enableBroadcast = true;
bool enableAnnounce = true;
ESP8266WebServer server(80);
WiFiUDP udp;

int brightness = 80;
int temp = 4000;
bool autoMode = true;
int recommendedBrightness = brightness;
int recommendedTemp       = temp;
char fabric[16] = "unknown";


// 设备广播
void broadcastDevice() {
  if(!enableBroadcast) return;
  static unsigned long lastBroadcast = 0;
  if (millis() - lastBroadcast > 5000) {
    lastBroadcast = millis();
    String msg = "{\"type\":\"announce\",\"device\":\"lamp\",\"id\":\"" + String(device_id) +
                 "\",\"ip\":\"" + WiFi.localIP().toString() + "\"}";
    udp.beginPacket(broadcastIP, udpPort);
    udp.write(msg.c_str());
    Serial.println("广播："+ msg);
    udp.endPacket();
  }
}

void handleStatus() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"status\":\"已配网\"}");
}

void handleSetLight() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "POST,OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");

  if (server.method() == HTTP_OPTIONS) {
    server.send(204);
    return;
  }

  if (!server.hasArg("plain")) {
    server.send(400, "application/json", "{\"error\":\"缺少 body\"}");
    return;
  }

  String body = server.arg("plain");
  DynamicJsonDocument doc(512);
  auto err = deserializeJson(doc, body);
  if (err) {
    server.send(400, "application/json", "{\"error\":\"JSON 解析失败\"}");
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
  Serial.printf("收到 HTTP 控制: bri=%d, temp=%d, auto=%d, recB=%d, recT=%d, fabric=%s\n",
                brightness, temp, autoMode,
                recommendedBrightness, recommendedTemp, fabric);
  server.send(200, "application/json", "{\"result\":\"OK\"}");

}


void handleResumeBroadcast() {
  enableBroadcast = true;
  enableAnnounce = true;
  server.send(200, "application/json", "{\"status\":\"resumed\"}");
  Serial.println("接收到网页指令：恢复广播");
}
void handleStopBroadcast() {
  enableBroadcast = false;
  Serial.println("接收到网页指令：停止广播");
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"result\":\"Broadcast stopped\"}");
}
void handleStopAnnounce() {
  enableAnnounce = false;
  Serial.println("接收到网页指令：停止上报");
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"result\":\"Announce stopped\"}");
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  if (type == WStype_TEXT) {
    StaticJsonDocument<512> doc;
    DeserializationError err = deserializeJson(doc, payload, length);
    if (err) return;

    if (doc["type"] == "state" && doc["id"] == device_id) {
      brightness = doc["brightness"] | brightness;
      temp       = doc["temp"]       | temp;
      recommendedBrightness = doc["recommendedBrightness"] | recommendedBrightness;
      recommendedTemp       = doc["recommendedTemp"]       | recommendedTemp;
      autoMode   = doc["auto"]       | autoMode;
const char* newFabric = doc["fabric"];
if (newFabric && strlen(newFabric) > 0) {
  strcpy(fabric, newFabric);
}
      Serial.printf("从WebSocket收到控制指令：亮度=%d 色温=%d 自动=%d 推荐色温=%d 推荐亮度=%d 面料%s\n", 
        brightness, temp, autoMode, recommendedTemp, recommendedBrightness, fabric);
    }
  } 
}

void applyLightSettings(int br, int tp) {
  tp = constrain(tp, 2700, 6500);

  int tempVal = map(tp, 2700, 6500, 0, 1024);
  int briVal  = map(br, 0, 100, 0, 1024);

  int pwmCold = (long)tempVal * briVal / 1024;
  int pwmWarm = (long)(1024 - tempVal) * briVal / 1024;

  analogWrite(LED_COLD_PIN,1024- pwmCold);
  analogWrite(LED_WARM_PIN,1024- pwmWarm);
  Serial.printf("PWM Cold=%d, Warm=%d\n", pwmCold, pwmWarm);
}

void sendStayRecordToServer(unsigned long duration, const char* device_id) {
  if (WiFi.status() == WL_CONNECTED) {
    WiFiClient client;
    HTTPClient http;

    http.begin(client, "http://110.41.81.4:3000/duration");
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"id\":\"" + String(device_id) +
                 "\",\"duration\":" + duration + "}";

    int httpCode = http.POST(payload);
    Serial.printf("[HTTP] POST 返回码：%d\n", httpCode);

    http.end();
  } else {
    Serial.println("WiFi 未连接，无法发送数据。");
  }
}


void updateLightingByToF() {
  if (!tofReady) {
    int br = autoMode ? recommendedBrightness : brightness;
    int tp = autoMode ? recommendedTemp : temp;
    unsigned long now = millis();

    if (autoMode && strcmp(fabric, "polyester") == 0) {
      digitalWrite(BLUR, LOW);
    } else {
      digitalWrite(BLUR, HIGH);
    }

    int level = digitalRead(BLUR);
   // Serial.printf("BLUR电平 = %d (%s)\n", level, level ? "HIGH" : "LOW");

    if (now - lastLightUpdate > lightUpdateInterval) {
      applyLightSettings(br, tp);
      lastLightUpdate = now;
    }
    return;
  }

  VL53L0X_RangingMeasurementData_t measure;
  lox.rangingTest(&measure, false);
  if (measure.RangeMilliMeter>8200)
  return;
  static bool wasNearby = false;
  static unsigned long transitionStart = 0;

  static unsigned long detectedStart  = 0; 
  static unsigned long leftStart     = 0; 

  bool currentNearby = (measure.RangeMilliMeter < 2000);
  unsigned long now = millis();

  Serial.printf("测距: %d mm\n", measure.RangeMilliMeter);

  if (autoMode) {
    if (currentNearby && !wasNearby) {
      if (detectedStart == 0) {
        detectedStart = now; 
      } else if (now - detectedStart >= 1000) {
        transitionStart = now;
        wasNearby = true;
        leftStart = 0; 
      }
    } else if (!currentNearby && wasNearby) {
      if (leftStart == 0) {
        leftStart = now; 
      } else if (now - leftStart >= 1000) {
        transitionStart = now;
        wasNearby = false;
        unsigned long stayDurationSeconds = (now - detectedStart) / 1000;
        sendStayRecordToServer(stayDurationSeconds, device_id);

        detectedStart = 0; 
      }
    } else {
      if (currentNearby) leftStart = 0;
      else detectedStart = 0;
    }
  }

  float ratio = float(now - transitionStart) / 2000.0f;
  if (ratio > 1.0f) ratio = 1.0f;

  int br = brightness;
  int tp = temp;

  if (autoMode) {
    if (wasNearby) {
      br = brightness + int((recommendedBrightness - brightness) * ratio);
      tp = temp       + int((recommendedTemp       - temp)       * ratio);
       if (strcmp(fabric, "polyester") == 0) {
  digitalWrite(BLUR, LOW);
  int level = digitalRead(BLUR);
  Serial.printf("BLUR电平 = %d (%s)\n", level, level ? "HIGH" : "LOW");
}
else  {digitalWrite(BLUR, HIGH);
  int level = digitalRead(BLUR);
  Serial.printf("BLUR_PIN 电平 = %d (%s)\n", level, level ? "HIGH" : "LOW");}
    } else {
      br = recommendedBrightness + int((brightness - recommendedBrightness) * ratio);
      tp = recommendedTemp       + int((temp       - recommendedTemp)       * ratio);
      digitalWrite(BLUR, HIGH);
    }
  }

  if (now - lastLightUpdate > lightUpdateInterval) {
    applyLightSettings(br, tp);
    lastLightUpdate = now;
  }
}


void sendAnnounce() {
  if (!enableAnnounce || WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  WiFiClient client;
  http.begin(client, "http://110.41.81.4:3000/announce");

  http.addHeader("Content-Type", "application/json");

  String json = "{\"id\":\"" + String(device_id) +
                "\",\"ip\":\"" + WiFi.localIP().toString() + "\"}";

  int httpCode = http.POST(json);
  if (httpCode > 0) {
    String payload = http.getString();
    Serial.println("服务器回应: " + payload);

    if (payload.indexOf("\"added\":true") >= 0) {
      enableAnnounce = false;
      enableBroadcast = false;
      Serial.println("✅ 成功上报且已添加，停止上报和广播");
    }

  } else {
    Serial.printf("上报失败: %s\n", http.errorToString(httpCode).c_str());
  }
  http.end();
}

void sendLightLevelToServer() {
  if (!bh1750Ready) return;  
  float lux = lightMeter.readLightLevel();  
  Serial.printf("📡 当前光照值：%.2f lux\n", lux);

  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    WiFiClient client;

    http.begin(client, "http://110.41.81.4:3000/lux"); 
    http.addHeader("Content-Type", "application/json");

    String json = "{\"id\":\"" + String(device_id) + "\",\"lux\":" + String(lux, 2) + "}";

    int httpCode = http.POST(json);
    if (httpCode > 0) {
      Serial.printf("✅ 光照上传成功，返回码: %d\n", httpCode);
    } else {
      Serial.printf("❌ 光照上传失败: %s\n", http.errorToString(httpCode).c_str());
    }
    http.end();
  } else {
    Serial.println("WiFi 未连接，无法上传光照值。");
  }
}

void setup() {
  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
WiFi.begin(ssid, password);

while (WiFi.status() != WL_CONNECTED) {
  delay(500);
  Serial.print(".");
}
Serial.println("WiFi 已连接，IP地址: " + WiFi.localIP().toString());
  webSocket.begin("110.41.81.4", 8081, "/");
  webSocket.onEvent(webSocketEvent);
  pinMode(LED_COLD_PIN, OUTPUT);
  pinMode(LED_WARM_PIN, OUTPUT);
  pinMode(BLUR, OUTPUT);
  digitalWrite(BLUR, HIGH);
  analogWriteRange(1024);
 Wire.begin(TOF_SDA_PIN, TOF_SCL_PIN);
 Wire.setClock(400000);
  if (!lox.begin()) {
    Serial.println("❌ 无法初始化 VL53L0X！");
   
  } else {
    Serial.println("✅ VL53L0X 初始化成功");
    tofReady = true;
  }

 if (lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
   Serial.println("✅ BH1750 初始化成功");
   bh1750Ready = true;
 } else {
   Serial.println("❌ BH1750 初始化失败！");
 }



  udp.begin(udpPort);
  server.onNotFound([](){
    if (server.method() == HTTP_OPTIONS) {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      server.sendHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
      server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
      server.send(204);
    } else {
      server.send(404);
    }
  });
  server.on("/status", handleStatus);
  server.on("/setLight", handleSetLight);
  server.on("/stopBroadcast",handleStopBroadcast);
  server.on("/resumeBroadcast",handleResumeBroadcast);
  server.on("/stopAnnounce", handleStopAnnounce);
  server.begin();
}

void loop() {
  server.handleClient();
  webSocket.loop();
  broadcastDevice();
  static unsigned long lastPing = 0;
if (millis() - lastPing > 30000) {  
  lastPing = millis();
  StaticJsonDocument<64> doc;
  doc["type"] = "ping";
  doc["id"]   = device_id;
  String pingMsg;
  serializeJson(doc, pingMsg);
  webSocket.sendTXT(pingMsg);
  Serial.println("发送 WebSocket 心跳：" + pingMsg);
}static unsigned long lastAnnounce = 0;
  if (millis() - lastAnnounce > 5000) {
    lastAnnounce = millis();
    sendAnnounce();  
  }
updateLightingByToF();

  static unsigned long lastLightSend = 0;
  if (millis() - lastLightSend > lightSendInterval) {
    lastLightSend = millis();
    sendLightLevelToServer();
    //applyLightSettings(brightness,temp);
  }

}

