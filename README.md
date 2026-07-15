# FGTgamemodeCapture

Командный режим захвата точек для **Minecraft 1.21.1 (NeoForge)** с экономикой, классами, магазином Superb Warfare и пресетами карт.

**By Frogot** · [Telegram](https://t.me/frogot_1) · [Канал Rubickhouse](https://t.me/Rubickhouse1)

---

## Что внутри репозитория

| Папка | Описание |
|-------|----------|
| [`mod/`](mod/) | Исходники NeoForge-мода (Gradle) |
| [`map/MapForPack/`](map/MapForPack/) | Готовая карта для матчей |
| [`presets/`](presets/) | Пресет `test1` — точки, базы, экономика, аirdrop |
| [`release/`](release/) | Собранный JAR + **ZIP со всей сборкой** для Modrinth |
| [`modpack/`](modpack/) | Моды, конфиги, ресурспаки профиля ZOV pack |
| [`docs/GUIDE.ru.md`](docs/GUIDE.ru.md) | Подробный гайд на русском |

---

## Быстрый старт (игрок / админ)

### Скачать и играть (вся сборка)

1. Открой **[Releases](https://github.com/Frogot102/FGTgamemodeCapture/releases)** на GitHub.
2. Скачай **`FGT-ZOV-pack-playable.zip`**.
3. Распакуй в `AppData\Roaming\ModrinthApp\profiles\FGT-ZOV-pack`.
4. В Modrinth App добавь профиль **FGT-ZOV-pack** (NeoForge 1.21.1).
5. Запусти мир **MapForPack** и введи команды ниже.

Подробнее: [modpack/README.md](modpack/README.md)

### Ручная установка (только мод + карта)

1. Установи **NeoForge 1.21.1** и модпак с **Superb Warfare**, **AshVehicle** и зависимостями.
2. Скопируй `release/FGTgamemodeCapture-*.jar` в папку `mods` профиля.
3. Положи карту `map/MapForPack` в `saves/MapForPack`.
4. Скопируй `presets/test1.dat` в `<профиль>/zovcapture/presets/test1.dat`.
5. Запусти мир и выполни:

   ```
   /FGTgmC preset load test1
   /FGTgmC shop reload
   /FGTgmC start
   ```

### Клавиши

| Клавиша | Действие |
|---------|----------|
| **J** | Выбор команды |
| **K** | Выбор класса |
| **B** | Магазин (только на базе) |
| **M** | Меню матча + фидбек в Telegram |
| **N** | Навигация к точкам |

---

## Сборка мода из исходников

```bash
cd mod
./gradlew build
```

JAR: `mod/build/libs/FGTgamemodeCapture-<version>.jar`

> На Windows, если `gradlew` не работает, используй локальный Gradle или:
> `%USERPROFILE%\.gradle\wrapper\dists\gradle-9.1.0-bin\...\gradle.bat build`

---

## Пресеты

Сохранить текущую конфигурацию мира:
```
/FGTgmC preset save mymap
```

Загрузить:
```
/FGTgmC preset load test1
```

Файлы пресетов: `<instance>/zovcapture/presets/*.dat`

Читаемый дамп `test1_dump.txt` — для просмотра точек и баз без NBT-редактора.

---

## Основные команды

Префикс: **`/FGTgmC`**

| Команда | Описание |
|---------|----------|
| `start` / `start now` | Старт матча (с отсчётом / сразу) |
| `stop` | Остановить матч |
| `create <имя> <радиус>` | Создать точку захвата |
| `base create <имя> <команда> <радиус>` | Создать базу команды |
| `captain set <команда> <игрок>` | Назначить капитана |
| `preset save/load <имя>` | Пресеты карты |
| `shop reload` | Обновить магазин из JAR |

Полный список — в [docs/GUIDE.ru.md](docs/GUIDE.ru.md).

---

## Механики (кратко)

- Захват точек через ванильный scoreboard (ВСРФ / NATO)
- Личная и командная экономика; командный магазин — только **капитан**
- Классы: штурмовик, медик, техник, разведчик
- Техника в контейнерах SBW (T-90, BMP-2, Mi-28 / Little Bird, Abrams, Bradley)
- Friendly fire между союзниками отключён
- Магазин и покупки — только на своей базе

---

## Зависимости модпака

- NeoForge **21.1+**
- [Superb Warfare](https://modrinth.com/mod/superb-warfare)
- [AshVehicle](https://modrinth.com/mod/ashvehicle) (Abrams, T-90, Bradley)
- Рекомендуется: Wararmbise, NVG, GeckoLib и др. из вашего ZOV pack

---

## Лицензия

All Rights Reserved — **By Frogot**.  
Не выкладывайте сборку как свою без указания автора.

---

*FGTgamemodeCapture · v1.7.4*
