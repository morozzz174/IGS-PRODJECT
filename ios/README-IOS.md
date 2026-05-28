# ИЖС-Проектировщик — iOS (Compose Multiplatform)

Перенос Android-приложения на iOS через **Compose Multiplatform**.

## Структура проекта

```
ios/
├── composeApp/                  # Общий Kotlin код (Compose UI + логика)
│   ├── src/
│   │   ├── commonMain/          # Код для обеих платформ
│   │   │   └── kotlin/ru/company/izhs_planner/
│   │   │       ├── ai/          # AIManager, LLMInference (expect), agents
│   │   │       ├── domain/      # Модели данных
│   │   │       └── App.kt       # Главный Composable
│   │   ├── androidMain/         # Android-специфичный код
│   │   │   └── kotlin/ru/company/izhs_planner/
│   │   │       ├── ai/LLMInference.android.kt  # llama.cpp через JNI
│   │   │       └── MainActivity.kt
│   │   └── iosMain/             # iOS-специфичный код
│   │       └── kotlin/ru/company/izhs_planner/
│   │           ├── ai/LLMInference.ios.kt      # llama.cpp через cinterop
│   │           └── MainViewController.kt
│   └── build.gradle.kts
├── iosApp/                      # Xcode проект
│   ├── iosApp.xcodeproj/
│   └── iosApp/
│       ├── iosAppApp.swift      # SwiftUI entry point
│       ├── ContentView.swift    # Обёртка для ComposeUIViewController
│       └── Info.plist
├── build.gradle.kts             # Корневой Gradle
├── settings.gradle.kts
├── gradle.properties
└── gradle/
```

## Требования

- **macOS** Sonoma 14+ (обязательно для сборки iOS)
- **Xcode** 15.3+
- **Android Studio** (для Kotlin кода)
- **JDK** 17+

## Сборка и запуск

### 1. Настроить Gradle

```bash
cd ios
chmod +x gradlew
```

### 2. Собрать Kotlin framework

```bash
./gradlew :composeApp:assembleDebug
```

### 3. Открыть Xcode проект

```bash
open iosApp/iosApp.xcodeproj
```

### 4. В Xcode

1. Выберите симулятор iOS 17+
2. `Product → Build` (или Cmd+B) — Xcode сам запустит Gradle таск `embedAndSignAppleFrameworkForXcode`
3. `Product → Run` (или Cmd+R)

## Интеграция llama.cpp для iOS

Текущая реализация `LLMInference.ios.kt` содержит заглушку.
Для реального инференса:

1. Постройте llama.cpp для iOS:
```bash
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
mkdir build-ios && cd build-ios
cmake -G Xcode \
  -DCMAKE_TOOLCHAIN_FILE=../cmake/ios.toolchain.cmake \
  -DPLATFORM=OS64 \
  -DCMAKE_BUILD_TYPE=Release ..
cmake --build . --config Release
```

2. Настройте cinterop в `composeApp/build.gradle.kts`:
```kotlin
iosArm64().compilations.getByName("main") {
    cinterops {
        create("llama") {
            defFile(project.file("src/nativeInterop/cinterop/llama.def"))
            includeDirs("path/to/llama.cpp/includes")
        }
    }
}
```

3. Создайте `composeApp/src/nativeInterop/cinterop/llama.def`
4. Реализуйте native-функции в LLMInference.ios.kt

## Публикация в App Store

1. В Xcode: `Product → Archive`
2. App Store Connect → загрузите билд
3. Заполните карточку приложения
4. Отправьте на ревью

## Что уже перенесено с Android

| Компонент | Статус |
|-----------|--------|
| Domain модели (Models.kt, ChatModels.kt) | ✅ Общие |
| AI агенты (Agents.kt) | ✅ Общие |
| AIManager | ✅ Общий (с expect LLMInference) |
| UI (Compose экраны) | ✅ В процессе (App.kt) |
| 3D-конструктор | 🔜 Планируется |
| Room (SQLDelight) | 🔜 Планируется |
| Экспорт PDF | 🔜 Планируется |
