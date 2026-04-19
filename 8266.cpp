#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <WebSocketsClient.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>
#include <LittleFS.h>
#include <Wire.h>
#include <Adafruit_VL53L0X.h>
#include <BH1750.h>

// ===================== 传感器 / 外设 =====================
BH1750 lightMeter;
Adafruit_VL53L0X lox = Adafruit_VL53L0X();
WebSocketsClient webSocket;
ESP8266WebServer server(80);
WiFiUDP udp;

// ===================== 引脚定义 =====================
#define LED_COLD_PIN D2
#define LED_WARM_PIN D1
#define BLUR         D7
#define TOF_SDA_PIN  D5
#define TOF_SCL_PIN  D6

// ===================== 固件信息 =====================
#define FW_TYPE    "light8266"
#define FW_VERSION "1.0.0"

// ===================== 默认服务器配置 =====================
// 这里只是默认值，真正运行时会优先用保存的配置
const char* DEFAULT_SERVER_HOST = "192.168.31.171";
const uint16_t DEFAULT_HTTP_PORT = 3000;
const uint16_t DEFAULT_WS_PORT   = 3000;

// ===================== 定时参数 =====================
const unsigned long lightSendInterval    = 30000;    // 30秒上传一次光照
const unsigned long lightUpdateInterval  = 50;     // 最小灯光刷新间隔
const unsigned long wifiConnectTimeout   = 15000;  // 已保存WiFi连接超时
const unsigned long smartConfigTimeout   = 30000;  // SmartConfig超时
const unsigned long announceInterval     = 5000;   // 上报间隔
const unsigned long broadcastInterval    = 5000;   // UDP广播间隔
const unsigned long wsPingInterval       = 30000;  // WebSocket心跳间隔

const int udpPort = 4210;

// ===================== 运行状态 =====================
bool bh1750Ready = false;
bool tofReady = false;
bool enableBroadcast = true;
bool enableAnnounce = true;
bool portalMode = false;
bool otaInProgress = false;

unsigned long lastLightSend = 0;
unsigned long lastLightUpdate = 0;
unsigned long lastAnnounce = 0;
unsigned long lastBroadcast = 0;
unsigned long lastPing = 0;

// ===================== 灯光控制参数 =====================
int brightness = 80;
int temp = 4000;
bool autoMode = true;
int recommendedBrightness = 80;
int recommendedTemp = 4000;
char fabric[16] = "unknown";

// ===================== 设备配置 =====================
struct DeviceConfig {
  String ssid;
  String password;
  String serverHost;
  uint16_t httpPort;
  uint16_t wsPort;
};

DeviceConfig cfg;

// 设备唯一ID：默认用 lamp-芯片ID
String deviceId;

void beginWebSocketClient();
// ===================== 工具函数 =====================
String configPath() {
  return "/config.json";
}

String makeDeviceId() {
  String id = "lamp-";
  id += String(ESP.getChipId(), HEX);
  id.toUpperCase();
  return id;
}

String httpUrl(const String& path) {
  return "http://" + cfg.serverHost + ":" + String(cfg.httpPort) + path;
}

IPAddress calcBroadcastIP() {
  IPAddress ip = WiFi.localIP();
  IPAddress mask = WiFi.subnetMask();
  IPAddress broadcast;
  for (int i = 0; i < 4; i++) {
    broadcast[i] = ip[i] | (~mask[i]);
  }
  return broadcast;
}

void safeCopyFabric(const char* src) {
  if (!src || strlen(src) == 0) return;
  snprintf(fabric, sizeof(fabric), "%s", src);
}

int compareVersion(const String& a, const String& b) {
  int a1 = 0, a2 = 0, a3 = 0;
  int b1 = 0, b2 = 0, b3 = 0;
  sscanf(a.c_str(), "%d.%d.%d", &a1, &a2, &a3);
  sscanf(b.c_str(), "%d.%d.%d", &b1, &b2, &b3);

  if (a1 != b1) return (a1 > b1) ? 1 : -1;
  if (a2 != b2) return (a2 > b2) ? 1 : -1;
  if (a3 != b3) return (a3 > b3) ? 1 : -1;
  return 0;
}

// ===================== 配置读写 =====================
bool saveConfig(const DeviceConfig& c) {
  StaticJsonDocument<256> doc;
  doc["ssid"] = c.ssid;
  doc["password"] = c.password;
  doc["serverHost"] = c.serverHost;
  doc["httpPort"] = c.httpPort;
  doc["wsPort"] = c.wsPort;

  File f = LittleFS.open(configPath(), "w");
  if (!f) return false;

  if (serializeJson(doc, f) == 0) {
    f.close();
    return false;
  }
  f.close();
  return true;
}

bool loadConfig() {
  cfg.ssid = "";
  cfg.password = "";
  cfg.serverHost = DEFAULT_SERVER_HOST;
  cfg.httpPort = DEFAULT_HTTP_PORT;
  cfg.wsPort = DEFAULT_WS_PORT;

  if (!LittleFS.exists(configPath())) return false;

  File f = LittleFS.open(configPath(), "r");
  if (!f) return false;

  StaticJsonDocument<256> doc;
  DeserializationError err = deserializeJson(doc, f);
  f.close();
  if (err) return false;

  cfg.ssid = doc["ssid"] | "";
  cfg.password = doc["password"] | "";
  cfg.serverHost = doc["serverHost"] | DEFAULT_SERVER_HOST;
  cfg.httpPort = doc["httpPort"] | DEFAULT_HTTP_PORT;
  cfg.wsPort = doc["wsPort"] | DEFAULT_WS_PORT;

  return cfg.ssid.length() > 0;
}

void clearConfig() {
  if (LittleFS.exists(configPath())) {
    LittleFS.remove(configPath());
  }
}

// ===================== 配网页面 =====================
String getPortalHtml() {
  String html = R"rawliteral(
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>ESP8266 配网</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>
    body{font-family:Arial;padding:20px;max-width:560px;margin:auto;}
    input{width:100%;padding:10px;margin:8px 0;box-sizing:border-box;}
    button{padding:12px 18px;margin-top:10px;}
    .box{border:1px solid #ddd;border-radius:12px;padding:16px;}
    .tip{color:#666;font-size:14px;}
  </style>
</head>
<body>
  <div class="box">
    <h2>灯节点配网</h2>
    <p>设备ID：__DEVICE_ID__</p>
    <p class="tip">先尝试 SmartConfig。失败后可手动填 Wi-Fi 和服务器信息。</p>

    <form action="/saveWifi" method="POST">
      <label>Wi-Fi 名称</label>
      <input name="ssid" placeholder="请输入 Wi-Fi 名称">

      <label>Wi-Fi 密码</label>
      <input name="password" type="password" placeholder="请输入 Wi-Fi 密码">

      <label>服务器 Host/IP</label>
      <input name="serverHost" value="__SERVER_HOST__">

      <label>HTTP 端口</label>
      <input name="httpPort" value="__HTTP_PORT__">

      <label>WebSocket 端口</label>
      <input name="wsPort" value="__WS_PORT__">

      <button type="submit">保存并重启</button>
    </form>

    <form action="/resetWifi" method="POST">
      <button type="submit">清除配置并重启</button>
    </form>
  </div>
</body>
</html>
)rawliteral";

  html.replace("__DEVICE_ID__", deviceId);
  html.replace("__SERVER_HOST__", cfg.serverHost.length() ? cfg.serverHost : String(DEFAULT_SERVER_HOST));
  html.replace("__HTTP_PORT__", String(cfg.httpPort ? cfg.httpPort : DEFAULT_HTTP_PORT));
  html.replace("__WS_PORT__", String(cfg.wsPort ? cfg.wsPort : DEFAULT_WS_PORT));
  return html;
}

// ===================== WiFi =====================
bool connectWiFi(const String& ssid, const String& password, unsigned long timeoutMs) {
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid.c_str(), password.c_str());

  Serial.println("\n[WiFi] 正在连接: " + ssid);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < timeoutMs) {
    delay(500);
    Serial.print(".");
    yield();
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("[WiFi] 连接成功: " + WiFi.localIP().toString());
    return true;
  }

  Serial.println("[WiFi] 连接失败");
  return false;
}

bool connectSavedWiFi() {
  if (cfg.ssid.length() == 0) return false;
  return connectWiFi(cfg.ssid, cfg.password, wifiConnectTimeout);
}

bool smartConfigProvision(unsigned long timeoutMs) {
  Serial.println("[SmartConfig] 等待批量配网...");
  WiFi.mode(WIFI_STA);
  WiFi.beginSmartConfig();

  unsigned long start = millis();
  while (!WiFi.smartConfigDone() && millis() - start < timeoutMs) {
    delay(500);
    Serial.print("#");
    yield();
  }
  Serial.println();

  if (!WiFi.smartConfigDone()) {
    Serial.println("[SmartConfig] 超时");

    // 关键：停止 SmartConfig，否则后面切 AP 可能失败
    WiFi.stopSmartConfig();
    delay(300);
    WiFi.disconnect(true);
    delay(300);

    return false;
  }

  Serial.println("[SmartConfig] 已收到配网信息，等待联网...");
  start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 20000) {
    delay(500);
    Serial.print(".");
    yield();
  }
  Serial.println();

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[SmartConfig] 联网失败");

    // 这里也要停
    WiFi.stopSmartConfig();
    WiFi.disconnect(true);
    delay(300);

    return false;
  }

  DeviceConfig newCfg;
  newCfg.ssid = WiFi.SSID();
  newCfg.password = WiFi.psk();
  newCfg.serverHost = cfg.serverHost.length() ? cfg.serverHost : String(DEFAULT_SERVER_HOST);
  newCfg.httpPort = cfg.httpPort ? cfg.httpPort : DEFAULT_HTTP_PORT;
  newCfg.wsPort = cfg.wsPort ? cfg.wsPort : DEFAULT_WS_PORT;

  cfg = newCfg;
  saveConfig(cfg);

  WiFi.stopSmartConfig();

  Serial.println("[SmartConfig] 成功，已保存配置");
  return true;
}

void startConfigPortal() {
  portalMode = true;

  WiFi.persistent(false);

  WiFi.disconnect(true);
  delay(500);

  WiFi.mode(WIFI_OFF);
  delay(500);

  WiFi.mode(WIFI_AP);
  delay(300);

  IPAddress apIP(192, 168, 4, 1);
  IPAddress gateway(192, 168, 4, 1);
  IPAddress subnet(255, 255, 255, 0);

  bool configOk = WiFi.softAPConfig(apIP, gateway, subnet);
  Serial.println(configOk ? "[AP] IP配置成功" : "[AP] IP配置失败");

  String apName = "LightConfig_" + deviceId;

  bool apOk = WiFi.softAP(apName.c_str(), "12345678", 1, false, 4);

  Serial.println("[AP] 进入网页配网模式");
  Serial.println("[AP] 热点名称: " + apName);
  Serial.println("[AP] 密码: 12345678");

  if (!apOk) {
    Serial.println("[AP] 热点启动失败！");
    return;
  }

  delay(500);

  Serial.println("[AP] IP: " + WiFi.softAPIP().toString());
  Serial.println("[AP] 打开: http://" + WiFi.softAPIP().toString());

  server.on("/", HTTP_GET, []() {
    server.send(200, "text/html; charset=utf-8", getPortalHtml());
  });

  server.on("/saveWifi", HTTP_POST, []() {
    DeviceConfig newCfg;
    newCfg.ssid = server.arg("ssid");
    newCfg.password = server.arg("password");
    newCfg.serverHost = server.arg("serverHost");
    newCfg.httpPort = (uint16_t)server.arg("httpPort").toInt();
    newCfg.wsPort = (uint16_t)server.arg("wsPort").toInt();

    if (newCfg.ssid.length() == 0) {
      server.send(400, "text/plain; charset=utf-8", "Wi-Fi 名称不能为空");
      return;
    }

    if (newCfg.serverHost.length() == 0) newCfg.serverHost = DEFAULT_SERVER_HOST;
    if (newCfg.httpPort == 0) newCfg.httpPort = DEFAULT_HTTP_PORT;
    if (newCfg.wsPort == 0) newCfg.wsPort = DEFAULT_WS_PORT;

    if (!saveConfig(newCfg)) {
      server.send(500, "text/plain; charset=utf-8", "保存失败");
      return;
    }

    server.send(200, "text/html; charset=utf-8", "<h3>保存成功，设备即将重启...</h3>");
    delay(1200);
    ESP.restart();
  });

  server.on("/resetWifi", HTTP_POST, []() {
    clearConfig();
    server.send(200, "text/html; charset=utf-8", "<h3>已清除配置，设备即将重启...</h3>");
    delay(1200);
    ESP.restart();
  });

  server.begin();
}

// ===================== OTA =====================
void otaStarted() {
  Serial.println("[OTA] 开始升级");
}
void otaFinished() {
  Serial.println("[OTA] 升级完成");
}
void otaProgress(int cur, int total) {
  Serial.printf("[OTA] 进度: %d / %d\n", cur, total);
}
void otaError(int err) {
  Serial.printf("[OTA] 错误码: %d\n", err);
}

void doOtaUpdate(const String& url, const String& version) {
  if (otaInProgress) return;
  otaInProgress = true;

  Serial.println("[OTA] 收到升级通知");
  Serial.println("[OTA] 当前版本: " + String(FW_VERSION));
  Serial.println("[OTA] 目标版本: " + version);
  Serial.println("[OTA] 下载地址: " + url);

  webSocket.disconnect();
  delay(200);

  WiFiClient client;
  ESPhttpUpdate.setClientTimeout(12000);
  ESPhttpUpdate.onStart(otaStarted);
  ESPhttpUpdate.onEnd(otaFinished);
  ESPhttpUpdate.onProgress(otaProgress);
  ESPhttpUpdate.onError(otaError);

  t_httpUpdate_return ret = ESPhttpUpdate.update(client, url);

  switch (ret) {
    case HTTP_UPDATE_FAILED:
      Serial.printf("[OTA] 升级失败 (%d): %s\n",
                    ESPhttpUpdate.getLastError(),
                    ESPhttpUpdate.getLastErrorString().c_str());
      otaInProgress = false;
      beginWebSocketClient();
      break;

    case HTTP_UPDATE_NO_UPDATES:
      Serial.println("[OTA] 没有更新");
      otaInProgress = false;
      beginWebSocketClient();
      break;

    case HTTP_UPDATE_OK:
      Serial.println("[OTA] 升级成功，设备将自动重启");
      break;
  }
}

// ===================== HTTP 接口 =====================
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
  temp = doc["temp"] | temp;
  autoMode = doc["auto"] | autoMode;
  recommendedBrightness = doc["recommendedBrightness"] | recommendedBrightness;
  recommendedTemp = doc["recommendedTemp"] | recommendedTemp;
  safeCopyFabric(doc["fabric"]);

  Serial.printf("收到 HTTP 控制: bri=%d temp=%d auto=%d recB=%d recT=%d fabric=%s\n",
                brightness, temp, autoMode,
                recommendedBrightness, recommendedTemp, fabric);

  server.send(200, "application/json", "{\"result\":\"OK\"}");
}

void handleResumeBroadcast() {
  enableBroadcast = true;
  enableAnnounce = true;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"status\":\"resumed\"}");
  Serial.println("接收到网页指令：恢复广播");
}

void handleStopBroadcast() {
  enableBroadcast = false;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"result\":\"Broadcast stopped\"}");
  Serial.println("接收到网页指令：停止广播");
}

void handleStopAnnounce() {
  enableAnnounce = false;
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"result\":\"Announce stopped\"}");
  Serial.println("接收到网页指令：停止上报");
}

void handleResetWifi() {
  clearConfig();
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"result\":\"WiFi config cleared, restarting\"}");
  delay(800);
  ESP.restart();
}

void setupDeviceHttpServer() {
  server.onNotFound([]() {
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
  server.on("/stopBroadcast", handleStopBroadcast);
  server.on("/resumeBroadcast", handleResumeBroadcast);
  server.on("/stopAnnounce", handleStopAnnounce);
  server.on("/resetWifi", HTTP_POST, handleResetWifi);

  server.begin();
}

// ===================== WebSocket =====================
void sendWsRegister() {
  StaticJsonDocument<256> doc;
  doc["type"] = "register";
  doc["id"] = deviceId;
  doc["chipId"] = deviceId;
  doc["deviceType"] = "lamp";
  doc["fwType"] = FW_TYPE;
  doc["fwVersion"] = FW_VERSION;
  doc["ip"] = WiFi.localIP().toString();
  doc["mac"] = WiFi.macAddress();

  String msg;
  serializeJson(doc, msg);
  webSocket.sendTXT(msg);
  Serial.println("[WS] register: " + msg);
}

void handleWsMessage(const String& text) {
  Serial.println("[WS] 收到消息: " + text);

  StaticJsonDocument<768> doc;
  DeserializationError err = deserializeJson(doc, text);
  if (err) {
    Serial.println("[WS] JSON解析失败");
    return;
  }

  JsonObject root = doc.as<JsonObject>();
  JsonObject payload = root;

  if (root["data"].is<JsonObject>()) {
    payload = root["data"].as<JsonObject>();
  }

  String type = root["type"] | payload["type"] | "";

  if (type == "state") {
    String targetId = payload["id"] | "";
    String chipId = payload["chipId"] | "";

    if (targetId != deviceId && chipId != deviceId) {
      Serial.println("[WS] state 不是发给本设备，忽略");
      return;
    }

    brightness = payload["brightness"] | brightness;
    temp = payload["temp"] | temp;
    recommendedBrightness = payload["recommendedBrightness"] | recommendedBrightness;
    recommendedTemp = payload["recommendedTemp"] | recommendedTemp;

    if (payload.containsKey("autoMode")) {
      autoMode = payload["autoMode"];
    } else {
      autoMode = payload["auto"] | autoMode;
    }

    safeCopyFabric(payload["fabric"]);

    Serial.printf("WS控制：亮度=%d 色温=%d 自动=%d 推荐亮度=%d 推荐色温=%d 面料=%s\n",
                  brightness, temp, autoMode,
                  recommendedBrightness, recommendedTemp, fabric);
    return;
  }

  if (type == "ota:update") {
    String fwType = root["fwType"] | payload["fwType"] | "";
    String version = root["version"] | payload["version"] | "";
    String url = root["url"] | payload["url"] | "";

    if (fwType != FW_TYPE) {
      Serial.println("[OTA] 固件类型不匹配，忽略");
      return;
    }

    if (version.length() == 0 || url.length() == 0) {
      Serial.println("[OTA] OTA消息缺少 version/url");
      return;
    }

    if (compareVersion(version, FW_VERSION) <= 0) {
      Serial.println("[OTA] 当前版本已不低于目标版本，忽略");
      return;
    }

    doOtaUpdate(url, version);
    return;
  }

  Serial.println("[WS] 未处理消息类型: " + type);
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.println("[WS] 已断开");
      break;

    case WStype_CONNECTED:
      Serial.printf("[WS] 已连接: %s\n", payload);
      sendWsRegister();
      break;

    case WStype_TEXT:
      handleWsMessage(String((char*)payload));
      break;

    case WStype_PONG:
      Serial.println("[WS] PONG");
      break;

    default:
      break;
  }
}

void beginWebSocketClient() {
  webSocket.disconnect();
  delay(100);

  Serial.println("[WS] 准备连接:");
  Serial.println("host = " + cfg.serverHost);
  Serial.println("port = " + String(cfg.wsPort));
  Serial.println("path = /ws/device");
  Serial.println("url  = ws://" + cfg.serverHost + ":" + String(cfg.wsPort) + "/ws/device");

  webSocket.begin(cfg.serverHost.c_str(), cfg.wsPort, "/ws/device");
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);
  webSocket.enableHeartbeat(15000, 3000, 2);
}

// ===================== 灯控逻辑 =====================
void applyLightSettings(int br, int tp) {
  tp = constrain(tp, 2700, 6500);

  int tempVal = map(tp, 2700, 6500, 0, 1024);
  int briVal  = map(br, 0, 100, 0, 1024);

  int pwmCold = (long)tempVal * briVal / 1024;
  int pwmWarm = (long)(1024 - tempVal) * briVal / 1024;

  analogWrite(LED_COLD_PIN, 1024 - pwmCold);
  analogWrite(LED_WARM_PIN, 1024 - pwmWarm);
  Serial.printf("PWM Cold=%d, Warm=%d\n", pwmCold, pwmWarm);
}

void sendStayRecordToServer(unsigned long durationSeconds) {
  if (WiFi.status() != WL_CONNECTED) return;

  WiFiClient client;
  HTTPClient http;
  http.begin(client, httpUrl("/admin/duration/create"));
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<192> doc;
  doc["chipId"] = deviceId;
  doc["durationValue"] = durationSeconds * 1000UL;

  String payload;
  serializeJson(doc, payload);

  int httpCode = http.POST(payload);
  Serial.printf("[HTTP] /admin/duration/create -> %d\n", httpCode);
  if (httpCode > 0) {
    Serial.println(http.getString());
  }

  http.end();
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
    //Serial.printf("BLUR电平 = %d (%s)\n", level, level ? "HIGH" : "LOW");

    if (now - lastLightUpdate > lightUpdateInterval) {
      applyLightSettings(br, tp);
      lastLightUpdate = now;
    }
    return;
  }

  VL53L0X_RangingMeasurementData_t measure;
  lox.rangingTest(&measure, false);

  if (measure.RangeMilliMeter > 8200) return;

  static bool wasNearby = false;
  static unsigned long transitionStart = 0;
  static unsigned long detectedStart = 0;
  static unsigned long leftStart = 0;

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
        sendStayRecordToServer(stayDurationSeconds);
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
      tp = temp + int((recommendedTemp - temp) * ratio);

      if (strcmp(fabric, "polyester") == 0) {
        digitalWrite(BLUR, LOW);
      } else {
        digitalWrite(BLUR, HIGH);
      }
    } else {
      br = recommendedBrightness + int((brightness - recommendedBrightness) * ratio);
      tp = recommendedTemp + int((temp - recommendedTemp) * ratio);
      digitalWrite(BLUR, HIGH);
    }
  }

  if (now - lastLightUpdate > lightUpdateInterval) {
    applyLightSettings(br, tp);
    lastLightUpdate = now;
  }
}

// ===================== 设备上报 =====================
void broadcastDevice() {
  if (!enableBroadcast || WiFi.status() != WL_CONNECTED) return;

  if (millis() - lastBroadcast > broadcastInterval) {
    lastBroadcast = millis();

    IPAddress broadcastIP = calcBroadcastIP();
    String msg = "{\"type\":\"announce\",\"device\":\"lamp\",\"id\":\"" + deviceId +
                 "\",\"ip\":\"" + WiFi.localIP().toString() + "\"}";

    udp.beginPacket(broadcastIP, udpPort);
    udp.write((const uint8_t*)msg.c_str(), msg.length());
    udp.endPacket();

    Serial.println("广播: " + msg);
  }
}

void sendAnnounce() {
  if (!enableAnnounce || WiFi.status() != WL_CONNECTED) return;

  WiFiClient client;
  HTTPClient http;
  http.begin(client, httpUrl("/admin/device/announce"));
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<256> doc;
  doc["chipId"] = deviceId;
  doc["ip"] = WiFi.localIP().toString();
  doc["deviceType"] = "lamp";

  String json;
  serializeJson(doc, json);

  int httpCode = http.POST(json);
  if (httpCode > 0) {
    String payload = http.getString();
    Serial.println("服务器回应: " + payload);

    if (payload.indexOf("\"added\":true") >= 0) {
      enableAnnounce = false;
      enableBroadcast = false;
      Serial.println("成功上报且已添加，停止上报和广播");
    }
  } else {
    Serial.printf("上报失败: %s\n", http.errorToString(httpCode).c_str());
  }

  http.end();
}

void sendLightLevelToServer() {
  if (!bh1750Ready || WiFi.status() != WL_CONNECTED) return;

  float lux = lightMeter.readLightLevel();
  Serial.printf("当前光照值：%.2f lux\n", lux);

  WiFiClient client;
  HTTPClient http;
  http.begin(client, httpUrl("/admin/lux/create"));
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<192> doc;
  doc["chipId"] = deviceId;
  doc["luxValue"] = lux;

  String json;
  serializeJson(doc, json);

  int httpCode = http.POST(json);

  if (httpCode > 0) {
    Serial.printf("光照上传成功，返回码: %d\n", httpCode);
    Serial.println(http.getString());
  } else {
    Serial.printf("光照上传失败: %s\n", http.errorToString(httpCode).c_str());
  }

  http.end();
}

// ===================== 初始化 =====================
void setupHardwareAndSensors() {
  pinMode(LED_COLD_PIN, OUTPUT);
  pinMode(LED_WARM_PIN, OUTPUT);
  pinMode(BLUR, OUTPUT);
  digitalWrite(BLUR, HIGH);
  analogWriteRange(1024);

  Wire.begin(TOF_SDA_PIN, TOF_SCL_PIN);
  Wire.setClock(400000);

  if (!lox.begin()) {
    Serial.println("VL53L0X 初始化失败");
  } else {
    Serial.println("VL53L0X 初始化成功");
    tofReady = true;
  }

  if (lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
    Serial.println("BH1750 初始化成功");
    bh1750Ready = true;
  } else {
    Serial.println("BH1750 初始化失败");
  }

  udp.begin(udpPort);
}

bool ensureWiFiReady() {
  if (WiFi.status() == WL_CONNECTED) return true;

  Serial.println("[WiFi] 已断开，尝试重连已保存网络...");
  if (connectSavedWiFi()) return true;

  Serial.println("[WiFi] 已保存网络重连失败，尝试 SmartConfig...");
  if (smartConfigProvision(smartConfigTimeout)) return true;

  Serial.println("[WiFi] 进入 AP 配网模式");
  startConfigPortal();
  return false;
}

// ===================== setup / loop =====================
void setup() {
  Serial.begin(115200);
  delay(200);

  deviceId = makeDeviceId();

  Serial.println("\n========================");
  Serial.println("设备启动");
  Serial.println("ID = " + deviceId);
  Serial.println("FW = " + String(FW_VERSION));
  Serial.println("========================");

  if (!LittleFS.begin()) {
    Serial.println("[FS] LittleFS 挂载失败");
  }

  setupHardwareAndSensors();

  bool hasConfig = loadConfig();
  bool wifiOk = false;

  if (hasConfig) {
    Serial.println("[BOOT] 检测到本地配置，尝试直连");
    wifiOk = connectSavedWiFi();
  } else {
    Serial.println("[BOOT] 没有本地配置");
  }

  if (!wifiOk) {
    wifiOk = smartConfigProvision(smartConfigTimeout);
  }

  if (!wifiOk) {
    startConfigPortal();
    return;
  }

  setupDeviceHttpServer();
  beginWebSocketClient();
  sendAnnounce();
}

void loop() {
  server.handleClient();

  if (portalMode) {
    return;
  }

  if (otaInProgress) {
    return;
  }

  if (!ensureWiFiReady()) {
    return;
  }

  webSocket.loop();
  broadcastDevice();
  updateLightingByToF();

  unsigned long now = millis();

  if (now - lastPing > wsPingInterval) {
    lastPing = now;
    StaticJsonDocument<64> doc;
    doc["type"] = "ping";
    doc["id"] = deviceId;
    String pingMsg;
    serializeJson(doc, pingMsg);
    webSocket.sendTXT(pingMsg);
    Serial.println("发送 WebSocket 心跳: " + pingMsg);
  }

  if (now - lastAnnounce > announceInterval) {
    lastAnnounce = now;
    sendAnnounce();
  }

  if (now - lastLightSend > lightSendInterval) {
    lastLightSend = now;
    sendLightLevelToServer();
  }
}