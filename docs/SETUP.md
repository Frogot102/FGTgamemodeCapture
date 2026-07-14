# Установка карты и пресета

## 1. Мод

Скопируй `release/FGTgamemodeCapture-1.7.4.jar` в папку `mods` NeoForge-профиля.

## 2. Карта

Скопируй папку `map/MapForPack` в `saves/MapForPack` вашего профиля Modrinth/Prism.

Пример (Modrinth):
```
profiles/ZOV pack/saves/MapForPack/
```

## 3. Пресет

Создай папку `zovcapture/presets` в корне профиля (рядом с `saves`, `mods`):
```
profiles/ZOV pack/zovcapture/presets/test1.dat
```

Скопируй туда `presets/test1.dat` из репозитория.

## 4. Первый запуск

1. Открой мир **MapForPack**
2. В чате (OP):
   ```
   /FGTgmC preset load test1
   /FGTgmC shop reload
   /FGTgmC captain set Blue <ник_капитана_ВСРФ>
   /FGTgmC captain set Red <ник_капитана_NATO>
   /FGTgmC start
   ```

## 5. Сборка мода (разработчикам)

```powershell
cd mod
.\gradlew.bat build
```

JAR появится в `mod/build/libs/`.

---

By Frogot · https://t.me/frogot_1
