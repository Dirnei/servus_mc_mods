# Builds the Servus mods, collects every jar the play test needs into server/mods
# and writes the player packs plus a download page to server/dist.
#   .\server\pack.ps1            build and pack
#   .\server\pack.ps1 -NoBuild   pack the jars that are already built
param([switch]$NoBuild)
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$server = $PSScriptRoot
$mods = Join-Path $server 'mods'
$dist = Join-Path $server 'dist'

$props = @{}
Get-Content (Join-Path $root 'gradle.properties') | Where-Object { $_ -match '^\s*([\w.]+)\s*=\s*(.*)$' } |
    ForEach-Object { $props[$Matches[1]] = $Matches[2].Trim() }
$mc = $props['minecraft_version']
$neo = $props['neo_version']

if (-not $NoBuild) {
    Push-Location $root
    try {
        & .\gradlew.bat build -x test --quiet
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
    } finally { Pop-Location }
}

# Servus jars
$jars = @()
foreach ($p in 'foundation', 'alchemy', 'shuttles', 'delicacies') {
    $jar = Get-ChildItem (Join-Path $root "$p\build\libs") -Filter "servus_$p-*.jar" |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { throw "No jar for $p, run without -NoBuild" }
    $jars += $jar
}

# Third party jars, taken from the Gradle cache in the versions gradle.properties pins
$cache = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'
$deps = @(
    @{ g = 'com.klikli_dev'; a = "modonomicon-$mc-neoforge"; v = $props['modonomicon_version'] },
    @{ g = 'mezz.jei'; a = "jei-$mc-neoforge"; v = $props['jei_version'] },
    @{ g = 'maven.modrinth'; a = 'jade'; v = $props['jade_version'] },
    @{ g = 'dev.ftb.mods'; a = 'ftb-library-neoforge'; v = $props['ftb_library_version'] },
    @{ g = 'dev.ftb.mods'; a = 'ftb-ultimine-neoforge'; v = $props['ftb_ultimine_version'] },
    @{ g = 'dev.ftb.mods'; a = 'ftb-teams-neoforge'; v = $props['ftb_teams_version'] },
    @{ g = 'dev.ftb.mods'; a = 'ftb-chunks-neoforge'; v = $props['ftb_chunks_version'] }
)
foreach ($d in $deps) {
    $dir = Join-Path $cache "$($d.g)\$($d.a)"
    if ($d.v) { $dir = Join-Path $dir $d.v }
    $jar = Get-ChildItem $dir -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jar) { throw "Missing $($d.a) $($d.v) in the Gradle cache, run a build first" }
    $jars += $jar
}

# server/mods, mounted into the container
if (Test-Path $mods) { Remove-Item $mods -Recurse -Force }
New-Item -ItemType Directory $mods | Out-Null
$jars | ForEach-Object { Copy-Item $_.FullName $mods }
[IO.File]::WriteAllText((Join-Path $server 'versions.env'), "VERSION=$mc`nNEOFORGE_VERSION=$neo`n", [Text.UTF8Encoding]::new($false))

# Player packs. Every jar goes in as an override, so no download links are needed.
$sha = (git -C $root rev-parse --short HEAD).Trim()
$version = "$($props['pack_version'])-$sha"
New-Item -ItemType Directory $dist -Force | Out-Null
Get-ChildItem $dist -File | Remove-Item
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem

# Writes a zip with the given text files and the jars under $jarDir. Entries are added
# one by one so their names use forward slashes; Windows PowerShell zips with
# backslashes otherwise and launchers reject the pack.
function Write-Pack([string]$path, [hashtable]$texts, [string]$jarDir) {
    $archive = [IO.Compression.ZipFile]::Open($path, 'Create')
    try {
        foreach ($name in $texts.Keys) {
            $writer = [IO.StreamWriter]::new($archive.CreateEntry($name).Open(), [Text.UTF8Encoding]::new($false))
            try { $writer.Write($texts[$name]) } finally { $writer.Dispose() }
        }
        foreach ($jar in $jars) {
            [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $jar.FullName, "$jarDir/$($jar.Name)")
        }
    } finally { $archive.Dispose() }
}

# Modrinth pack for Prism Launcher and the Modrinth App
$mrIndex = [ordered]@{
    formatVersion = 1
    game = 'minecraft'
    versionId = $version
    name = 'Servus Playtest'
    summary = "Servus mods $version"
    files = @()
    dependencies = [ordered]@{ minecraft = $mc; neoforge = $neo }
}
$mrpack = Join-Path $dist "servus-playtest-$version.mrpack"
Write-Pack $mrpack @{ 'modrinth.index.json' = ($mrIndex | ConvertTo-Json -Depth 5) } 'overrides/mods'

# CurseForge pack for the FTB App (Curse Import) and the CurseForge app
$cfManifest = [ordered]@{
    minecraft = [ordered]@{ version = $mc; modLoaders = @([ordered]@{ id = "neoforge-$neo"; primary = $true }) }
    manifestType = 'minecraftModpack'
    manifestVersion = 1
    name = 'Servus Playtest'
    version = $version
    author = $props['mod_authors']
    files = @()
    overrides = 'overrides'
}
$cfpack = Join-Path $dist "servus-playtest-$version-curseforge.zip"
Write-Pack $cfpack @{ 'manifest.json' = ($cfManifest | ConvertTo-Json -Depth 5) } 'overrides/mods'

# Bare jars, for a hand made NeoForge $neo instance when an import fails
$modsZip = Join-Path $dist "servus-playtest-$version-mods.zip"
Write-Pack $modsZip @{} 'mods'

# Download page served by the web sidecar in docker-compose.yml
$page = (Get-Content (Join-Path $server 'site\index.html') -Raw -Encoding utf8).
    Replace('{{VERSION}}', $version).Replace('{{MC}}', $mc).Replace('{{NEO}}', $neo).
    Replace('{{MRPACK}}', (Split-Path $mrpack -Leaf)).
    Replace('{{CFZIP}}', (Split-Path $cfpack -Leaf)).
    Replace('{{MODSZIP}}', (Split-Path $modsZip -Leaf))
[IO.File]::WriteAllText((Join-Path $dist 'index.html'), $page, [Text.UTF8Encoding]::new($false))

Write-Host "Mods for the server: $mods"
$jars | ForEach-Object { Write-Host "  $($_.Name)" }
Write-Host "Packs for the players in $dist"
Get-ChildItem $dist -File | ForEach-Object { Write-Host "  $($_.Name)" }
