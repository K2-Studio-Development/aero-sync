$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $projectRoot 'gradlew.bat'
$releaseDir = Join-Path $projectRoot 'releases\0.0.9'
$java21 = 'C:\Program Files\Java\jdk-21'
$java25 = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
$targets = @(
    @{ Minecraft = '1.21.1'; NeoForge = '21.1.248'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.2'; NeoForge = '21.2.1-beta'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.3'; NeoForge = '21.3.97'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.4'; NeoForge = '21.4.157'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.5'; NeoForge = '21.5.98'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.6'; NeoForge = '21.6.20-beta'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.7'; NeoForge = '21.7.25-beta'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.8'; NeoForge = '21.8.54'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.9'; NeoForge = '21.9.16-beta'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.10'; NeoForge = '21.10.64'; Java = $java21; Target = 21 },
    @{ Minecraft = '1.21.11'; NeoForge = '21.11.45'; Java = $java21; Target = 21 },
    @{ Minecraft = '26.1'; NeoForge = '26.1.0.19-beta'; Java = $java25; Target = 25 },
    @{ Minecraft = '26.2'; NeoForge = '26.2.0.59'; Java = $java25; Target = 25 }
)

New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
foreach ($target in $targets) {
    $jarName = "aerosync-neoforge-mc$($target.Minecraft)-0.0.9.jar"
    $releaseJar = Join-Path $releaseDir $jarName
    if (Test-Path -LiteralPath $releaseJar) {
        Write-Host "Using existing AeroSync NeoForge build for Minecraft $($target.Minecraft)."
        continue
    }
    $builtJar = Join-Path $projectRoot ".compat\neoforge-build\$($target.Minecraft)\libs\$jarName"
    if (Test-Path -LiteralPath $builtJar) {
        Write-Host "Reusing cached AeroSync NeoForge build for Minecraft $($target.Minecraft)."
        Copy-Item -LiteralPath $builtJar -Destination $releaseJar -Force
        continue
    }
    if (-not (Test-Path -LiteralPath $target.Java)) {
        throw "Required JDK was not found: $($target.Java)"
    }
    $env:JAVA_HOME = $target.Java
    $env:Path = "$($target.Java)\bin;$env:Path"
    Write-Host "Building AeroSync NeoForge for Minecraft $($target.Minecraft)..."
    & $gradle -p (Join-Path $projectRoot 'versions\neoforge') build --no-daemon `
        "-Pneoforge_version=$($target.NeoForge)" `
        "-Pminecraft_version=$($target.Minecraft)" `
        "-Pjava_version=$($target.Target)"
    if ($LASTEXITCODE -ne 0) {
        throw "NeoForge build failed for Minecraft $($target.Minecraft)"
    }
    Copy-Item -LiteralPath $builtJar -Destination $releaseJar -Force
}

& (Join-Path $PSScriptRoot 'build-neoforge-universal.ps1')
if (-not $?) {
    throw "Universal NeoForge packaging failed"
}

Get-ChildItem -LiteralPath $releaseDir -Filter 'aerosync-neoforge-mc*-0.0.9.jar' -File |
    Remove-Item -Force

Get-ChildItem -LiteralPath $releaseDir -Filter '*.jar' -File |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash)  $([System.IO.Path]::GetFileName($_.Path))" } |
    Set-Content -LiteralPath (Join-Path $releaseDir 'SHA256SUMS.txt') -Encoding UTF8

Write-Host "NeoForge release artifact: $releaseDir"
