# Публикация на GitHub

Репозиторий уже инициализирован и закоммичен локально.

**Путь:** `ZOV pack/FGTgamemodeCapture`

## Шаг 1 — войти в GitHub

```powershell
gh auth login
```

Выбери: GitHub.com → HTTPS → Login with a web browser.

## Шаг 2 — создать репозиторий и запушить

```powershell
cd "c:\Users\alfia\AppData\Roaming\ModrinthApp\profiles\ZOV pack\FGTgamemodeCapture"

gh repo create FGTgamemodeCapture --public --source=. --remote=origin --push --description "Team capture gamemode for Minecraft 1.21.1 NeoForge with SBW shop"
```

Или приватный:

```powershell
gh repo create FGTgamemodeCapture --private --source=. --remote=origin --push
```

## Без gh (через сайт)

1. На https://github.com/new создай репозиторий **FGTgamemodeCapture** (без README).
2. Выполни:

```powershell
cd "c:\Users\alfia\AppData\Roaming\ModrinthApp\profiles\ZOV pack\FGTgamemodeCapture"
git remote add origin https://github.com/ТВОЙ_НИК/FGTgamemodeCapture.git
git push -u origin main
```

---

By Frogot
