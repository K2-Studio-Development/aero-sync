$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $projectRoot 'gradlew.bat'
$releaseDir = Join-Path $projectRoot 'releases\0.0.9'
$java21 = 'C:\Program Files\Java\jdk-21'
$java25 = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'

$legacyMatrix = @(
    @{ Minecraft = '1.21.1'; Yarn = '1.21.1+build.3'; FabricApi = '0.116.15+1.21.1' },
    @{ Minecraft = '1.21.2'; Yarn = '1.21.2+build.1'; FabricApi = '0.106.1+1.21.2' },
    @{ Minecraft = '1.21.3'; Yarn = '1.21.3+build.2'; FabricApi = '0.114.1+1.21.3' },
    @{ Minecraft = '1.21.4'; Yarn = '1.21.4+build.8'; FabricApi = '0.119.4+1.21.4' },
    @{ Minecraft = '1.21.5'; Yarn = '1.21.5+build.1'; FabricApi = '0.128.2+1.21.5' },
    @{ Minecraft = '1.21.6'; Yarn = '1.21.6+build.1'; FabricApi = '0.128.2+1.21.6' },
    @{ Minecraft = '1.21.7'; Yarn = '1.21.7+build.8'; FabricApi = '0.129.0+1.21.7' },
    @{ Minecraft = '1.21.8'; Yarn = '1.21.8+build.1'; FabricApi = '0.136.1+1.21.8' },
    @{ Minecraft = '1.21.9'; Yarn = '1.21.9+build.1'; FabricApi = '0.134.1+1.21.9' },
    @{ Minecraft = '1.21.10'; Yarn = '1.21.10+build.3'; FabricApi = '0.138.4+1.21.10' },
    @{ Minecraft = '1.21.11'; Yarn = '1.21.11+build.6'; FabricApi = '0.141.6+1.21.11' }
)

$modernMatrix = @(
    @{ Minecraft = '26.1'; FabricApi = '0.145.1+26.1' },
    @{ Minecraft = '26.2'; FabricApi = '0.157.0+26.2' }
)

function Set-JavaHome([string] $path) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required JDK was not found: $path"
    }
    $env:JAVA_HOME = $path
    $env:Path = "$path\bin;$env:Path"
}

function Invoke-GradleBuild([string[]] $arguments) {
    & $gradle @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE"
    }
}

New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
Get-ChildItem -LiteralPath $releaseDir -Filter 'aerosync-fabric-*.jar' -File -ErrorAction SilentlyContinue | Remove-Item -Force

Set-JavaHome $java21
foreach ($target in $legacyMatrix) {
    Write-Host "Building AeroSync for Minecraft $($target.Minecraft)..."
    Invoke-GradleBuild @(
        'clean',
        'build',
        '--no-daemon',
        "-Pminecraft_version=$($target.Minecraft)",
        "-Pyarn_mappings=$($target.Yarn)",
        "-Pfabric_version=$($target.FabricApi)"
    )

    $jarName = "aerosync-fabric-mc$($target.Minecraft)-0.0.9.jar"
    Copy-Item -LiteralPath (Join-Path $projectRoot "build\libs\$jarName") -Destination (Join-Path $releaseDir $jarName)
}

Set-JavaHome $java25
foreach ($target in $modernMatrix) {
    Write-Host "Building AeroSync for Minecraft $($target.Minecraft)..."
    Invoke-GradleBuild @(
        '-p',
        (Join-Path $projectRoot 'versions\26.2'),
        'clean',
        'build',
        '--no-daemon',
        "-Pminecraft_version=$($target.Minecraft)",
        "-Pfabric_api_version=$($target.FabricApi)",
        "-Parchives_base_name=aerosync-fabric-mc$($target.Minecraft)"
    )

    $jarName = "aerosync-fabric-mc$($target.Minecraft)-0.0.9.jar"
    Copy-Item -LiteralPath (Join-Path $projectRoot "versions\26.2\build\libs\$jarName") -Destination (Join-Path $releaseDir $jarName)
}

& (Join-Path $PSScriptRoot 'build-universal.ps1')
if ($LASTEXITCODE -ne 0) {
    throw "Universal packaging failed with exit code $LASTEXITCODE"
}

Get-ChildItem -LiteralPath $releaseDir -Filter 'aerosync-fabric-mc*-0.0.9.jar' -File |
    Remove-Item -Force

Get-ChildItem -LiteralPath $releaseDir -Filter '*.jar' -File |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash)  $([System.IO.Path]::GetFileName($_.Path))" } |
    Set-Content -LiteralPath (Join-Path $releaseDir 'SHA256SUMS.txt') -Encoding UTF8

Write-Host "Release artifacts: $releaseDir"
