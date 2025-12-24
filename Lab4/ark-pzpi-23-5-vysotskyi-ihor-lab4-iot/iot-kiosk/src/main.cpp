// ============================================================================
// IoT РЕЙТИНГ-ГОЛОСУВАННЯ НА ESP32
// Система для збору оцінок користувачів через кнопки з математичним аналізом
// ============================================================================

#include <WiFi.h>           // Підключення до WiFi мережі
#include <HTTPClient.h>     // REST API запити до сервера
#include <ArduinoJson.h>    // Парсування JSON даних
#include <Preferences.h>    // Зберігання конфігурації у EEPROM
#include <time.h>           // Робота з часом

// ============================================================================
// ОГОЛОШЕННЯ ФУНКЦІЙ
// ============================================================================

void displayPoll();        // Показ опитування на дисплей
void handleRating();       // Обробка голосування (натискання кнопок)
void sendVote(int rating); // Відправка голосу на сервер
void syncWithServer();     // Синхронізація конфігурації з сервером
void getPollFromServer();  // Запит активного опитування
void connectWiFi();        // Підключення до WiFi
void loadSettings();       // Завантаження конфіги з EEPROM
void saveSettings();       // Збереження конфіги у EEPROM

// ============================================================================
// ЖОРСТКО ЗАКОДОВАНІ КОНСТАНТИ (початкові значення)
// ============================================================================

// WiFi параметри
const char* WIFI_SSID = "Wokwi-GUEST";
const char* WIFI_PASSWORD = "";

// Адреса REST API сервера
const char* SERVER_BASE_URL = "http://172.20.10.3:8080/api";

// Унікальний ID пристрою (UUID)
const char* DEVICE_ID = "aec29976-de35-472c-9d4d-5264c71e42be";

// ============================================================================
// КОНФІГУРАЦІЯ ПОРТІВ ESP32
// ============================================================================

// Пини кнопок для рейтингу (1-5 звезд)
const int BTN_RATING_1 = 14;  // Кнопка ⭐
const int BTN_RATING_2 = 15;  // Кнопка ⭐⭐
const int BTN_RATING_3 = 13;  // Кнопка ⭐⭐⭐
const int BTN_RATING_4 = 12;  // Кнопка ⭐⭐⭐⭐
const int BTN_RATING_5 = 2;   // Кнопка ⭐⭐⭐⭐⭐

// LED індикатори
const int LED_VOTE_OK = 4;    // Зелена LED (успішна відправка)
const int LED_ERROR = 25;     // Червона LED (помилка)

// ============================================================================
// СТРУКТУРИ ДАНИХ
// ============================================================================

// Конфігурація пристрою (зберігається у EEPROM, синхронізується з сервером)
struct DeviceConfig {
  String deviceId;              // UUID пристрою
  unsigned long pollIntervalMs;  // Інтервал запиту опитувань (мс)
  unsigned long displayTimeoutMs;// Таймаут показу опитування (мс)
  float confidenceThreshold;    // Поріг впевненості для валідації
  float anomalyThreshold;       // Поріг аномалії (Z-score)
  bool isEnabled;               // Активність пристрою
};

// Дані поточного опитування
struct CurrentPoll {
  String id;        // UUID опитування
  String title;     // Назва опитування
  String question;  // Текст запитання
  int maxRating;    // Максимум рейтингу (зазвичай 5)
  bool isActive;    // Чи активне опитування
};

// Розраховані метрики якості голосу
struct VoteMetrics {
  float confidence;       // Впевненість користувача (сигмоїдна функція часу)
  float anomalyScore;     // Оцінка аномалії (Z-score для виявлення ботів)
  float entropy;          // Ентропія Шеннона (невизначеність вибору)
  long votingTimeMs;      // Час голосування (мс)
  String validationStatus;// Статус: APPROVED/SUSPICIOUS/REJECTED
};

// ============================================================================
// ГЛОБАЛЬНІ ЗМІННІ
// ============================================================================

Preferences preferences;      // Об'єкт для роботи з EEPROM
DeviceConfig config;          // Поточна конфігурація пристрою
CurrentPoll currentPoll;      // Поточне активне опитування
VoteMetrics currentMetrics;   // Метрики останнього голосу

// Таймери для синхронізації
unsigned long lastSyncTime = 0;       // Час останньої синхронізації конфіги
unsigned long lastPollFetchTime = 0;  // Час останнього запиту опитування

// Інтервали синхронізації
const unsigned long SYNC_INTERVAL = 60000;       // Синхронізація кожні 60 сек
const unsigned long POLL_FETCH_INTERVAL = 30000; // Запит опитування кожні 30 сек

// ============================================================================
// ФУНКЦІЇ УПРАВЛІННЯ КОНФІГУРАЦІЄЮ
// ============================================================================

// Завантаження конфігурації з EEPROM при старті
void loadSettings() {
  preferences.begin("kiosk-config", false);
  
  // Зчитуємо конфіг, якщо немає - використовуємо дефолтні значення
  config.deviceId = preferences.getString("deviceId", DEVICE_ID);
  config.pollIntervalMs = preferences.getULong("pollInterval", 30000);
  config.displayTimeoutMs = preferences.getULong("displayTimeout", 120000);
  config.confidenceThreshold = preferences.getFloat("confThreshold", 0.6);
  config.anomalyThreshold = preferences.getFloat("anomThreshold", 2.5);
  config.isEnabled = preferences.getBool("enabled", true);

  // Виводимо конфіг у серійний монітор
  Serial.println("\n========== КОНФІГУРАЦІЯ ==========");
  Serial.println("Device ID: " + config.deviceId);
  Serial.printf("Інтервал опитування: %lu ms\n", config.pollIntervalMs);
  Serial.printf("Час показу: %lu ms\n", config.displayTimeoutMs);
  Serial.printf("Поріг впевненості: %.2f\n", config.confidenceThreshold);
  Serial.printf("Поріг аномалії: %.2f\n", config.anomalyThreshold);
  Serial.printf("Статус: %s\n\n", config.isEnabled ? "УВІМКНЕНО" : "ВИМКНЕНО");
}

// Збереження конфігурації у EEPROM (викликається після синхронізації)
void saveSettings() {
  preferences.putString("deviceId", config.deviceId);
  preferences.putULong("pollInterval", config.pollIntervalMs);
  preferences.putULong("displayTimeout", config.displayTimeoutMs);
  preferences.putFloat("confThreshold", config.confidenceThreshold);
  preferences.putFloat("anomThreshold", config.anomalyThreshold);
  preferences.putBool("enabled", config.isEnabled);
  Serial.println("✅ Конфіг збережено у Flash\n");
}

// ============================================================================
// ФУНКЦІЇ МЕРЕЖЕВОЇ ВЗАЄМОДІЇ
// ============================================================================

// Підключення до WiFi мережі
void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return; // Якщо вже підключено - вихід
  
  Serial.print("🔌 Підключення до WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  // Чекаємо максимум 20 спроб по 500мс
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

// Синхронізація конфігурації з сервером (кожні 60 сек)
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
      
      // Парсуємо JSON та оновлюємо конфіг
      if (!error && doc["success"] == true) {
        JsonObject data = doc["data"];
        config.pollIntervalMs = data["config"]["pollIntervalMs"] | config.pollIntervalMs;
        config.displayTimeoutMs = data["config"]["displayTimeoutMs"] | config.displayTimeoutMs;
        config.confidenceThreshold = data["config"]["confidenceThreshold"] | config.confidenceThreshold;
        config.anomalyThreshold = data["config"]["anomalyThreshold"] | config.anomalyThreshold;
        
        Serial.println("✅ Синхронізація успішна\n");
        saveSettings(); // Зберігаємо нову конфіг
      } else {
        Serial.println("❌ Помилка сервера\n");
      }
    } else {
      Serial.printf("❌ HTTP помилка: %d\n\n", httpCode);
    }
    http.end();
  }
}

// Запит активного опитування з сервера (кожні 30 сек)
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
      
      // Парсуємо JSON масив опитувань
      if (!error && doc.is<JsonArray>()) {
        JsonArray arr = doc.as<JsonArray>();
        
        if (arr.size() > 0) {
          JsonObject poll = arr[0]; // Беремо перше активне опитування
          
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

// ============================================================================
// МАТЕМАТИЧНІ ФУНКЦІЇ РОЗРАХУНКУ МЕТРИК
// ============================================================================

// МЕТРИКА 1: Впевненість користувача через сигмоїдну функцію часу
// Формула: confidence = 1 / (1 + e^(-0.1 * (timeSec - 15)))
// При t=15s → confidence=0.5, при t<15s → низька, при t>15s → висока
float calculateConfidence(long votingTimeMs) {
  double timeSec = votingTimeMs / 1000.0;
  double k = 0.1;          // Крутизна кривої
  double midpoint = 15.0;  // Точка перегину
  double confidence = 1.0 / (1.0 + exp(-k * (timeSec - midpoint)));
  return (float)confidence;
}

// МЕТРИКА 2: Аномалія активності через Z-score
// Формула: zScore = |votingTime - expectedTime| / stdDev
// z<1.0 → нормально, z=1-3 → підозріло, z>3 → дуже дивно
float calculateAnomalyScore(long votingTimeMs) {
  double expectedTime = 15000.0; // Очікуємо ~15 сек
  double stdDev = 5000.0;        // Стандартне відхилення 5 сек
  double zScore = abs((votingTimeMs - expectedTime) / stdDev);
  return (float)zScore;
}

// МЕТРИКА 3: Ентропія Шеннона (невизначеність вибору)
// Формула: H = -(p*log2(p) + (1-p)*log2(1-p))
// Низька ентропія → впевнений вибір, висока → коливання
float calculateEntropy(long votingTimeMs) {
  double normalized = min(votingTimeMs / 30000.0, 1.0);
  double p = normalized;
  if (p <= 0.0 || p >= 1.0) return 0.0;
  double entropy = -(p * (log(p) / log(2.0)) + (1 - p) * (log(1 - p) / log(2.0)));
  return (float)entropy;
}

// ============================================================================
// КОМПОЗИЦІЯ МЕТРИК ТА ВИЗНАЧЕННЯ СТАТУСУ ВАЛІДАЦІЇ
// ============================================================================

// Розраховує всі три метрики та визначає статус голосу
// APPROVED: все в межах норми
// SUSPICIOUS: низька впевненість або підвищена аномалія
// REJECTED: дуже висока аномалія
void computeVoteMetrics(long votingTimeMs) {
  currentMetrics.votingTimeMs = votingTimeMs;
  currentMetrics.confidence = calculateConfidence(votingTimeMs);
  currentMetrics.anomalyScore = calculateAnomalyScore(votingTimeMs);
  currentMetrics.entropy = calculateEntropy(votingTimeMs);
  
  // Логіка визначення статусу валідації
  if (currentMetrics.anomalyScore > config.anomalyThreshold * 2) {
    currentMetrics.validationStatus = "REJECTED";      // Дуже дивне
  } else if (currentMetrics.confidence < 0.3 || 
             currentMetrics.anomalyScore > config.anomalyThreshold) {
    currentMetrics.validationStatus = "SUSPICIOUS";    // Підозріле
  } else {
    currentMetrics.validationStatus = "APPROVED";      // Нормальне
  }
  
  // Виводимо метрики у серійний монітор
  Serial.printf("📊 Метрики: conf=%.2f, anom=%.2f, entr=%.2f, статус=%s\n",
                currentMetrics.confidence, currentMetrics.anomalyScore,
                currentMetrics.entropy, currentMetrics.validationStatus.c_str());
}

// ============================================================================
// ФУНКЦІЇ ОБРОБКИ ГОЛОСУВАННЯ
// ============================================================================

// Показ опитування у серійному порту
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

// Обробка рейтингу: читання кнопок, розрахунок метрик, відправка
void handleRating() {
  displayPoll();
  
  unsigned long startTime = millis();
  int rating = 0;
  
  // Цикл: чекаємо натискання кнопки або таймаут
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
    delay(50); // Debounce для уникнення дрижання контактів
  }
  
  // Якщо вибір був - розраховуємо метрики та відправляємо
  if (rating > 0) {
    long votingTime = millis() - startTime;
    computeVoteMetrics(votingTime);
    sendVote(rating);
  } else {
    Serial.println("⏱️ Час вичерпаний\n");
  }
}

// Відправка голосу на сервер з усіма метриками
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
  
  // Формуємо JSON з рейтингом та метриками
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
  
  // POST запит на сервер
  int httpCode = http.POST(jsonData);
  
  // Обробка результату з LED сигналізацією
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

// ============================================================================
// ІНІЦІАЛІЗАЦІЯ ТА ОСНОВНИЙ ЦИКЛ
// ============================================================================

// Ініціалізація при включенні (виконується один раз)
void setup() {
  Serial.begin(115200);
  delay(1000);
  
  // Налаштування пінів кнопок як входи з pull-up
  pinMode(BTN_RATING_1, INPUT_PULLUP);
  pinMode(BTN_RATING_2, INPUT_PULLUP);
  pinMode(BTN_RATING_3, INPUT_PULLUP);
  pinMode(BTN_RATING_4, INPUT_PULLUP);
  pinMode(BTN_RATING_5, INPUT_PULLUP);
  
  // Налаштування пінів LED як виходи
  pinMode(LED_VOTE_OK, OUTPUT);
  pinMode(LED_ERROR, OUTPUT);
  digitalWrite(LED_VOTE_OK, LOW);
  digitalWrite(LED_ERROR, LOW);
  
  Serial.println("\n🎯 IoT Рейтинг-голосування");
  Serial.println("════════════════════════════════\n");
  
  // Послідовність ініціалізації
  loadSettings();        // Завантажуємо конфіг з EEPROM
  connectWiFi();         // Підключаємось до WiFi
  syncWithServer();      // Синхронізуємо конфіг з сервером
  getPollFromServer();   // Запитуємо перше опитування
  
  Serial.println("✅ Система готова!\n");
}

// Основний цикл (крутиться нескінченно)
void loop() {
  // Перевіряємо WiFi на кожній ітерації
  connectWiFi();
  
  unsigned long currentTime = millis();
  
  // Синхронізація конфіги кожні 60 сек
  if (currentTime - lastSyncTime >= SYNC_INTERVAL) {
    syncWithServer();
    lastSyncTime = currentTime;
  }
  
  // Запит опитування кожні 30 сек
  if (currentTime - lastPollFetchTime >= POLL_FETCH_INTERVAL) {
    getPollFromServer();
    lastPollFetchTime = currentTime;
  }
  
  // Обробка голосування якщо опитування активне
  if (currentPoll.isActive) {
    handleRating();          // Показуємо опитування та читаємо кнопки
    delay(2000);             // Пауза після голосування
    getPollFromServer();     // Запитуємо наступне опитування
  } else {
    Serial.println("⏳ Очікування опитування...");
    delay(5000);             // Чекаємо перед наступною спробою
  }
}