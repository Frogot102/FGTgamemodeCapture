# Builds a ready-to-play Modrinth profile zip from repo contents.
# Output: release/FGT-ZOV-pack-playable.zip

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$profileName = "FGT-ZOV-pack"
$staging = Join-Path $env:TEMP "fgt-pack-staging-$profileName"
$outZip = Join-Path $repoRoot "release\FGT-ZOV-pack-playable.zip"

if (Test-Path $staging) {
    Remove-Item $staging -Recurse -Force
}

$dest = Join-Path $staging $profileName
New-Item -ItemType Directory -Force -Path $dest | Out-Null

function Copy-Tree($source, $target) {
    if (Test-Path $source) {
        New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
        Copy-Item -Path $source -Destination $target -Recurse -Force
    }
}

Copy-Tree (Join-Path $repoRoot "modpack\profile\mods") (Join-Path $dest "mods")
Copy-Tree (Join-Path $repoRoot "modpack\profile\config") (Join-Path $dest "config")
Copy-Tree (Join-Path $repoRoot "modpack\profile\resourcepacks") (Join-Path $dest "resourcepacks")
Copy-Item (Join-Path $repoRoot "modpack\profile\options.txt") (Join-Path $dest "options.txt") -Force -ErrorAction SilentlyContinue
Copy-Tree (Join-Path $repoRoot "modpack\profile\zovcapture") (Join-Path $dest "zovcapture")
Copy-Tree (Join-Path $repoRoot "map\MapForPack") (Join-Path $dest "saves\MapForPack")

$readme = @'
FGT-ZOV-pack - ready Modrinth profile
By Frogot

1. Extract this folder to:
   %APPDATA%\ModrinthApp\profiles\FGT-ZOV-pack

2. In Modrinth App select profile FGT-ZOV-pack (NeoForge 1.21.1).

3. Open world MapForPack and run:
   /FGTgmC preset load test1
   /FGTgmC shop reload
   /FGTgmC start

Telegram: @frogot_1 | t.me/Rubickhouse1
'@
Set-Content -Path (Join-Path $dest "HOW_TO_PLAY.txt") -Value $readme -Encoding UTF8

New-Item -ItemType Directory -Force -Path (Split-Path $outZip -Parent) | Out-Null
if (Test-Path $outZip) {
    Remove-Item $outZip -Force
}

Compress-Archive -Path (Join-Path $staging $profileName) -DestinationPath $outZip -CompressionLevel Optimal
$sizeMb = [math]::Round((Get-Item $outZip).Length / 1MB, 1)
Write-Host "Created zip: $outZip size: $sizeMb megabytes"
Remove-Item $staging -Recurse -Force
