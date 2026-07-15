# Modpack — ZOV pack (Modrinth profile)

Готовая сборка для **NeoForge 1.21.1**: все моды, конфиги, ресурспаки и пресет.

## Установка из репозитория (после clone)

### Вариант A — одним ZIP (рекомендуется)

1. Скачай **`release/FGT-ZOV-pack-playable.zip`** из [Releases](https://github.com/Frogot102/FGTgamemodeCapture/releases) на GitHub.
2. Распакуй в:
   ```
   C:\Users\<ты>\AppData\Roaming\ModrinthApp\profiles\FGT-ZOV-pack
   ```
3. В **Modrinth App** → Add existing profile → выбери папку `FGT-ZOV-pack`.
4. Запусти NeoForge **1.21.1**, открой мир **MapForPack**.

### Вариант B — собрать ZIP самому

```powershell
cd FGTgamemodeCapture
.\tools\build-playable-pack.ps1
```

Архив появится в `release/FGT-ZOV-pack-playable.zip`.

### Вариант C — вручную из папок репо

Скопируй в новую папку профиля `FGT-ZOV-pack`:

| Из репо | В профиль |
|---------|-----------|
| `modpack/profile/mods/` | `mods/` |
| `modpack/profile/config/` | `config/` |
| `modpack/profile/resourcepacks/` | `resourcepacks/` |
| `modpack/profile/options.txt` | `options.txt` |
| `modpack/profile/zovcapture/` | `zovcapture/` |
| `map/MapForPack/` | `saves/MapForPack/` |

## Первый запуск в мире

```
/FGTgmC preset load test1
/FGTgmC shop reload
/FGTgmC captain set Blue <ник>
/FGTgmC captain set Red <ник>
/FGTgmC start
```

## Состав

- **FGTgamemodeCapture** — наш gamemode мод
- **Superb Warfare** + **AshVehicle** + **Wararmbise** и др.
- Карта **MapForPack** с настроенными точками (пресет test1)

---

By Frogot · Mods belong to their respective authors.
