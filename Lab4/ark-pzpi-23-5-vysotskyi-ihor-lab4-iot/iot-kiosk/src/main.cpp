#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <time.h>

void displayPoll();
void handleRating();
void sendVote(int rating);
void syncWithServer();
void getPollFromServer();
void connectWiFi();
void loadSettings();
void saveSettings();

const char* WIFI_SSID = "Wokwi-GUEST";
const char* WIFI_PASSWORD = "";
const char* SERVER_BASE_URL = "http://172.20.10.3:8080/api";
const char* DEVICE_ID = "aec29976-de35-472c-9d4d-5264c71e42be";

const int BTN_RATING_1 = 14;
const int BTN_RATING_2 = 15;
const int BTN_RATING_3 = 13;
const int BTN_RATING_4 = 12;
const int BTN_RATING_5 = 2;

const int LED_VOTE_OK = 4;
const int LED_ERROR = 25;

struct DeviceConfig {
  String deviceId;
  unsigned long pollIntervalMs;
  unsigned long displayTimeoutMs;
  float confidenceThreshold;
  float anomalyThreshold;
  bool isEnabled;
};

struct CurrentPoll {
  String id;
  String title;
  String question;
  int maxRating;
  bool isActive;
};

struct VoteMetrics {
  float confidence;
  float anomalyScore;
  float entropy;
  long votingTimeMs;
  String validationStatus;
};

Preferences preferences;
DeviceConfig config;
CurrentPoll currentPoll;
VoteMetrics currentMetrics;

unsigned long lastSyncTime = 0;
unsigned long lastPollFetchTime = 0;
const unsigned long SYNC_INTERVAL = 60000;
const unsigned long POLL_FETCH_INTERVAL = 30000;

void loadSettings() {
  preferences.begin("kiosk-config", false);
  config.deviceId = preferences.getString("deviceId", DEVICE_ID);
  config.pollIntervalMs = preferences.getULong("pollInterval", 30000);
  config.displayTimeoutMs = preferences.getULong("displayTimeout", 120000);
  config.confidenceThreshold = preferences.getFloat("confThreshold", 0.6);
  config.anomalyThreshold = preferences.getFloat("anomThreshold", 2.5);
  config.isEnabled = preferences.getBool("enabled", true);

  Serial.println("\n========== КОНФІГУРАЦІЯ ==========");
  Serial.println("Device ID: " + config.deviceId);
  Serial.printf("Інтервал опитування: %lu ms\n", config.pollIntervalMs);
  Serial.printf("Час показу: %lu ms\n", config.displayTimeoutMs);
  Serial.printf("Поріг впевненості: %.2f\n", config.confidenceThreshold);
  Serial.printf("Поріг аномалії: %.2f\n", config.anomalyThreshold);
  Serial.printf("Статус: %s\n\n", config.isEnabled ? "УВІМКНЕНО" : "ВИМКНЕНО");
}

void saveSettings() {
  preferences.putString("deviceId", config.deviceId);
  preferences.putULong("pollInterval", config.pollIntervalMs);
  preferences.putULong("displayTimeout", config.displayTimeoutMs);
  preferences.putFloat("confThreshold", config.confidenceThreshold);
  preferences.putFloat("anomThreshold", config.anomalyThreshold);
  preferences.putBool("enabled", config.isEnabled);
  Serial.println("✅ Конфіг збережено у Flash\n");
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;
  
  Serial.print("🔌 Підключення до WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n✅ WiFi підключено!");
    Serial.println("IP: " + WiFi.localIP().toString() + "\n");
  } else {
    Serial.println("\n❌ Не вдалось підключитися\n");
  }
}

void syncWithServer() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("⚠️ Немає WiFi\n");
    return;
  }

  WiFiClient client;
  HTTPClient http;
  String url = String(SERVER_BASE_URL) + "/iot/sync/" + config.deviceId;
  
  Serial.println("🔄 Синхронізація з сервером...");
  
  if (http.begin(client, url)) {
    int httpCode = http.GET();
    
    if (httpCode == 200) {
      String payload = http.getString();
      DynamicJsonDocument doc(2048);
      DeserializationError error = deserializeJson(doc, payload);
      
      if (!error && doc["success"] == true) {
        JsonObject data = doc["data"];
        config.pollIntervalMs = data["config"]["pollIntervalMs"] | config.pollIntervalMs;
        config.displayTimeoutMs = data["config"]["displayTimeoutMs"] | config.displayTimeoutMs;
        config.confidenceThreshold = data["config"]["confidenceThreshold"] | config.confidenceThreshold;
        config.anomalyThreshold = data["config"]["anomalyThreshold"] | config.anomalyThreshold;
        
        Serial.println("✅ Синхронізація успішна\n");
        saveSettings();
      } else {
        Serial.println("❌ Помилка сервера\n");
      }
    } else {
      Serial.printf("❌ HTTP помилка: %d\n\n", httpCode);
    }
    http.end();
  }
}

void getPollFromServer() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("⚠️ Немає WiFi\n");
    return;
  }

  WiFiClient client;
  HTTPClient http;
  String url = String(SERVER_BASE_URL) + "/polls?status=ACTIVE";
  
  Serial.println("🗳️ Отримання опитування...");
  
  if (http.begin(client, url)) {
    int httpCode = http.GET();
    
    if (httpCode == 200) {
      String payload = http.getString();
      DynamicJsonDocument doc(4096);
      DeserializationError error = deserializeJson(doc, payload);
      
      if (!error && doc.is<JsonArray>()) {
        JsonArray arr = doc.as<JsonArray>();
        
        if (arr.size() > 0) {
          JsonObject poll = arr[0];
          
          currentPoll.id = poll["id"] | "";
          currentPoll.title = poll["title"] | "Без назви";
          currentPoll.question = poll["question"] | "Немає запитання";
          currentPoll.maxRating = poll["rating_max_scale"] | 5;
          currentPoll.isActive = (poll["status"] == "ACTIVE");
          
          Serial.printf("✅ Опитування: %s\n", currentPoll.title.c_str());
          Serial.printf("   Рейтинг: 1-%d\n\n", currentPoll.maxRating);
        } else {
          Serial.println("⚠️ Немає активних опитувань\n");
          currentPoll.isActive = false;
        }
      }
    } else {
      Serial.printf("❌ HTTP помилка: %d\n\n", httpCode);
    }
    http.end();
  }
}

float calculateConfidence(long votingTimeMs) {
  double timeSec = votingTimeMs / 1000.0;
  double k = 0.1;
  double midpoint = 15.0;
  double confidence = 1.0 / (1.0 + exp(-k * (timeSec - midpoint)));
  return (float)confidence;
}

float calculateAnomalyScore(long votingTimeMs) {
  double expectedTime = 15000.0;
  double stdDev = 5000.0;
  double zScore = abs((votingTimeMs - expectedTime) / stdDev);
  return (float)zScore;
}

float calculateEntropy(long votingTimeMs) {
  double normalized = min(votingTimeMs / 30000.0, 1.0);
  double p = normalized;
  if (p <= 0.0 || p >= 1.0) return 0.0;
  double entropy = -(p * (log(p) / log(2.0)) + (1 - p) * (log(1 - p) / log(2.0)));
  return (float)entropy;
}

void computeVoteMetrics(long votingTimeMs) {
  currentMetrics.votingTimeMs = votingTimeMs;
  currentMetrics.confidence = calculateConfidence(votingTimeMs);
  currentMetrics.anomalyScore = calculateAnomalyScore(votingTimeMs);
  currentMetrics.entropy = calculateEntropy(votingTimeMs);
  
  if (currentMetrics.anomalyScore > config.anomalyThreshold * 2) {
    currentMetrics.validationStatus = "REJECTED";
  } else if (currentMetrics.confidence < 0.3 || 
             currentMetrics.anomalyScore > config.anomalyThreshold) {
    currentMetrics.validationStatus = "SUSPICIOUS";
  } else {
    currentMetrics.validationStatus = "APPROVED";
  }
  
  Serial.printf("📊 Метрики: conf=%.2f, anom=%.2f, entr=%.2f, статус=%s\n",
                currentMetrics.confidence, currentMetrics.anomalyScore,
                currentMetrics.entropy, currentMetrics.validationStatus.c_str());
}

void displayPoll() {
  Serial.println("\n─────────────────────────────────");
  Serial.printf("РЕЙТИНГ: %s\n", currentPoll.title.c_str());
  Serial.println("─────────────────────────────────");
  Serial.printf("❓ %s\n", currentPoll.question.c_str());
  Serial.println("─────────────────────────────────");
  Serial.println("⭐ Натисніть кнопку (1-5):");
  Serial.println("  [BTN1] ⭐");
  Serial.println("  [BTN2] ⭐⭐");
  Serial.println("  [BTN3] ⭐⭐⭐");
  Serial.println("  [BTN4] ⭐⭐⭐⭐");
  Serial.println("  [BTN5] ⭐⭐⭐⭐⭐");
  Serial.println("─────────────────────────────────\n");
}

void handleRating() {
  displayPoll();
  
  unsigned long startTime = millis();
  int rating = 0;
  
  while (millis() - startTime < config.displayTimeoutMs) {
    if (digitalRead(BTN_RATING_1) == LOW) {
      rating = 1;
      Serial.println("✅ Оцінка: ⭐");
      break;
    }
    if (digitalRead(BTN_RATING_2) == LOW) {
      rating = 2;
      Serial.println("✅ Оцінка: ⭐⭐");
      break;
    }
    if (digitalRead(BTN_RATING_3) == LOW) {
      rating = 3;
      Serial.println("✅ Оцінка: ⭐⭐⭐");
      break;
    }
    if (digitalRead(BTN_RATING_4) == LOW) {
      rating = 4;
      Serial.println("✅ Оцінка: ⭐⭐⭐⭐");
      break;
    }
    if (digitalRead(BTN_RATING_5) == LOW) {
      rating = 5;
      Serial.println("✅ Оцінка: ⭐⭐⭐⭐⭐");
      break;
    }
    delay(50);
  }
  
  if (rating > 0) {
    long votingTime = millis() - startTime;
    computeVoteMetrics(votingTime);
    sendVote(rating);
  } else {
    Serial.println("⏱️ Час вичерпаний\n");
  }
}

void sendVote(int rating) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("❌ Немає WiFi");
    digitalWrite(LED_ERROR, HIGH);
    delay(500);
    digitalWrite(LED_ERROR, LOW);
    return;
  }

  WiFiClient client;
  HTTPClient http;
  String url = String(SERVER_BASE_URL) + "/iot/votes";
  
  DynamicJsonDocument doc(512);
  doc["iotDeviceId"] = config.deviceId;
  doc["pollId"] = currentPoll.id;
  doc["rating"] = rating;
  doc["votingTimeMs"] = currentMetrics.votingTimeMs;
  doc["confidence"] = currentMetrics.confidence;
  doc["anomalyScore"] = currentMetrics.anomalyScore;
  doc["entropy"] = currentMetrics.entropy;
  doc["validationStatus"] = currentMetrics.validationStatus;
  
  String jsonData;
  serializeJson(doc, jsonData);
  
  http.begin(client, url);
  http.addHeader("Content-Type", "application/json");
  
  Serial.println("\n📤 Відправка голосу...");
  
  int httpCode = http.POST(jsonData);
  
  if (httpCode == 201 || httpCode == 200) {
    Serial.println("✅ Голос відправлено успішно!\n");
    digitalWrite(LED_VOTE_OK, HIGH);
    delay(1000);
    digitalWrite(LED_VOTE_OK, LOW);
  } else {
    Serial.printf("❌ Помилка: HTTP %d\n\n", httpCode);
    digitalWrite(LED_ERROR, HIGH);
    delay(500);
    digitalWrite(LED_ERROR, LOW);
  }
  
  http.end();
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  pinMode(BTN_RATING_1, INPUT_PULLUP);
  pinMode(BTN_RATING_2, INPUT_PULLUP);
  pinMode(BTN_RATING_3, INPUT_PULLUP);
  pinMode(BTN_RATING_4, INPUT_PULLUP);
  pinMode(BTN_RATING_5, INPUT_PULLUP);
  
  pinMode(LED_VOTE_OK, OUTPUT);
  pinMode(LED_ERROR, OUTPUT);
  digitalWrite(LED_VOTE_OK, LOW);
  digitalWrite(LED_ERROR, LOW);
  
  Serial.println("\n🎯 IoT Рейтинг-голосування");
  Serial.println("════════════════════════════════\n");
  
  loadSettings();
  connectWiFi();
  syncWithServer();
  getPollFromServer();
  
  Serial.println("✅ Система готова!\n");
}

void loop() {
  connectWiFi();
  
  unsigned long currentTime = millis();
  
  if (currentTime - lastSyncTime >= SYNC_INTERVAL) {
    syncWithServer();
    lastSyncTime = currentTime;
  }
  
  if (currentTime - lastPollFetchTime >= POLL_FETCH_INTERVAL) {
    getPollFromServer();
    lastPollFetchTime = currentTime;
  }
  
  if (currentPoll.isActive) {
    handleRating();
    delay(2000);
    getPollFromServer();
  } else {
    Serial.println("⏳ Очікування опитування...");
    delay(5000);
  }
}