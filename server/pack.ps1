# Builds the Servus mods, collects every jar the play test needs into server/mods
# and writes server/dist/servus-playtest-<version>.mrpack for the players' launcher.
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
foreach ($p in 'foundation', 'manufacturing', 'storage') {
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
    @{ g = 'dev.architectury'; a = 'architectury-neoforge'; v = $null }
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

# Modrinth pack: every jar as an override, NeoForge version in the index
$sha = (git -C $root rev-parse --short HEAD).Trim()
$version = "$($props['mod_version'])-$sha"
$stage = Join-Path $env:TEMP "servus-mrpack-$sha"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory (Join-Path $stage 'overrides\mods') | Out-Null
$jars | ForEach-Object { Copy-Item $_.FullName (Join-Path $stage 'overrides\mods') }
$index = [ordered]@{
    formatVersion = 1
    game = 'minecraft'
    versionId = $version
    name = 'Servus Playtest'
    summary = "Servus mods $version"
    files = @()
    dependencies = [ordered]@{ minecraft = $mc; neoforge = $neo }
}
[IO.File]::WriteAllText((Join-Path $stage 'modrinth.index.json'), ($index | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))

New-Item -ItemType Directory $dist -Force | Out-Null
$pack = Join-Path $dist "servus-playtest-$version.mrpack"
Get-ChildItem $dist -Filter '*.mrpack' | Remove-Item
# Entries are added one by one so their names use forward slashes; Windows PowerShell
# zips with backslashes otherwise and launchers reject the pack.
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open($pack, 'Create')
try {
    Get-ChildItem $stage -Recurse -File | ForEach-Object {
        $name = $_.FullName.Substring($stage.Length + 1).Replace([char]92, [char]47)
        [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $name)
    }
} finally { $archive.Dispose() }
Remove-Item $stage -Recurse -Force

Write-Host "Mods for the server: $mods"
$jars | ForEach-Object { Write-Host "  $($_.Name)" }
Write-Host "Pack for the players: $pack"
