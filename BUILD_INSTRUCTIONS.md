# Инструкция по сборке и публикации приложения «ИЖС-Проектировщик»

## Требования к окружению

- **Android Studio**: Jellyfish (2024.2.1) или Iguana (2024.1.2)
- **Java**: JDK 17 (OpenJDK 17.x)
- **Gradle**: 8.2.2 (поставляется с AGP 8.2.2)
- **Android SDK**: API 35 (target), API 26 (min)
- **RAM**: минимум 8 ГБ, рекомендуется 16 ГБ
- **Место на диске**: минимум 10 ГБ свободного

## Этапы сборки

### 1. Подготовка проекта

```bash
# Клонировать репозиторий
git clone https://github.com/ваш-репозиторий/izhs_planner.git
cd izhs_planner

# Открыть в Android Studio
# File -> Open -> izhs_planner
```

### 2. Настройка нативного движка llama.cpp

Приложение использует **llama.cpp** для запуска LLM-модели офлайн на устройстве.

**Шаг 1: Клонирование llama.cpp**

```bash
# Из корня проекта
git clone --depth 1 https://github.com/ggerganov/llama.cpp.git app/src/main/cpp/llama.cpp
```

Или через PowerShell:
```powershell
.\setup_llama.ps1
```

**Шаг 2: Проверьте Android NDK**

Убедитесь, что установлен NDK версии 26.1.10909125 или выше:
- В Android Studio: SDK Manager → SDK Tools → NDK
- Или установите через sdkmanager

**Шаг 3: Сборка**

Откройте проект в Android Studio и сделайте Build → Make Project.
CMake автоматически соберёт llama_jni.so вместе с llama.cpp для выбранных ABI (arm64-v8a, armeabi-v7a).

### 3. Загрузка ИИ-модели

Модель скачивается **автоматически** при первом запуске приложения с HuggingFace:

- Модель: **Qwen2.5-3B-Instruct-Q4_K_M** (GGUF)
- Размер: ~1.8 ГБ
- URL: https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF
- Файл: qwen2.5-3b-instruct-q4_0.bin
- Сохраняется в: `context.filesDir/models/`

> **Важно:** Для первого запуска требуется интернет для загрузки модели. После загрузки ИИ работает полностью офлайн.

**Альтернативные модели:**
- **Llama-3.2-3B-Instruct-Q4** (~1.8 ГБ) — замените URL в AIManager.kt:240

### 4. Настройка signing config

Создайте файл `keystore.properties` в корне проекта:

```properties
storeFile=keystore/izhs-planner.jks
storePassword=ВАШ_ПАРОЛЬ_ОТ_ХРАНИЛИЩА
keyAlias=izhs_planner
keyPassword=ВАШ_ПАРОЛЬ_КЛЮЧА
```

### 5. Сборка debug-версии

```bash
# В терминале Android Studio
./gradlew assembleDebug

# Или через Android Studio:
# Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

### 6. Сборка release-версии

```bash
./gradlew assembleRelease
```

### 7. Проверка APK

После сборки APK будет расположен:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Настройка интеграций

### Яндекс Мобильная Реклама

1. Зарегистрируйтесь в Яндекс Рекламной сети: https://partner.yandex.ru
2. Создайте рекламный блок для banner и interstitial
3. Получите ad Unit ID
4. Замените в `MobileAdsManager.kt`:
```kotlin
companion object {
    private const val BANNER_AD_UNIT_ID = "ваш-banner-id"
    private const val INTERSTITIAL_AD_UNIT_ID = "ваш-interstitial-id"
}
```

### RuStore (для In-App Purchases)

1. Зарегистрируйтесь в RuStore: https:// seller.rustore.ru
2. Создайте приложение
3. Настройте IAP (внутренние покупки)
4. Добавьте товар "premium_full" с ценой 399 ₽
5. Получите App Key и настройте в `PremiumManager.kt`

### Согласование с операторами связи (ЕАЭС)

Для продажи в Казахстане, Беларуси, Кыргызстане:

1. Зарегистрируйтесь в Минсвязи каждой страны
2. Получите локальные сертификаты
3. Добавьте требования в Play Console

## Публикация

### Google Play Console

1. **Создайте аккаунт**: https://play.google/console
2. **Создайте приложение**:
   - Название: «ИЖС-Проектировщик»
   - Язык: Русский
   - Категория: Строительство
3. **Загрузите APK**: Release
4. **Заполните карточку**:
   - Описание ( минимум 400 символов)
   - Скриншоты ( минимум 2 шт.)
   - Политика конфиденциальности
5. **Отправьте на проверку**

### RuStore

1. **Создайте аккаунт**: https://console.rustore.ru
2. **Загрузите APK** в разделе «Приложения»
3. **Настройте IAP** для Премиум
4. **Опубликуйте**

## Структура продукта

### Бесплатная версия (по умолчанию)
- 1 сохранённый проект
- 50 сообщений ИИ в день
- Реклама (баннер + межстраничная)
- Водяной знак при экспорте

### Премиум (разовая покупка 399 ₽)
- Безлимитные проекты
- Безлимитный ИИ
- Экспорт без водяного знака
- Без рекламы
- Приоритетная поддержка

### Debug-сборка
- Без рекламы
- Укороченные логи
- Тестовые покупки

## Частые ошибки

### Ошибка: «Unable to start service»

**Решение**: Убедитесь, что minSdk >= 26

### Ошибка: «Model not found»

**Решение**: Проверьте наличие файла модели в assets/models/

### Ошибка: «Plugin not found»

**Решение**: Запустите `./gradlew wrapper`

### Ошибка: «OutOfMemoryError»

**Решение**: Увеличьте RAM в настройках Gradle:
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m
```

## Контакты поддержки

- Email: support@izhs-planner.ru
- Телефон: +7 (495) 123-45-67
- Сайт: https://izhs-planner.ru/support