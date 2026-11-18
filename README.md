# Срок годности (ExpiryDate)

Простое Android-приложение для отслеживания срока годности продуктов:
вводите дату покупки и срок годности в днях — приложение показывает,
когда продукт истекает и сколько дней осталось.

## Возможности
- Дата покупки (по умолчанию — сегодняшняя, можно изменить через календарь)
- Срок годности в днях (кнопки −/+ или ввод числа)
- Автоматический расчёт даты истечения
- Список продуктов с цветовой индикацией: зелёный (>3 дней), оранжевый (≤3 дней), красный (просрочено)
- Удаление продукта свайпом
- Данные сохраняются на устройстве (файл products.json)

## Готовый APK
`expiry-app.apk` — подписанная release-сборка, готова к установке на телефон
(Android 7.0 / API 24 и новее).

### Как установить на телефон
1. Скопируйте `expiry-app.apk` на телефон (USB, Google Drive, Telegram и т.п.).
2. Откройте файл на телефоне → разрешите «Установка из неизвестных источников».
3. Установите и запустите.

Либо через ADB (телефон в режиме разработчика + USB-отладка):
```
adb install expiry-app.apk
```

## Сборка из исходников
Требуются JDK 17 и Android SDK (platforms;android-34, build-tools;34.0.0).

```
echo "sdk.dir=/путь/к/android-sdk" > local.properties
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # релизный APK (нужна подпись, см. ниже)
```

Подпись релизного APK (ключ уже создан — release.keystore, пароль expiry123):
```
$ANDROID_HOME/build-tools/34.0.0/apksigner sign \
  --ks release.keystore --ks-key-alias expiry \
  --ks-pass pass:expiry123 --key-pass pass:expiry123 \
  --out expiry-app.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Важно: храните release.keystore — без него нельзя обновлять приложение поверх установленной версии.
