$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $projectRoot 'gradlew.bat'
$resultsDir = Join-Path $projectRoot '.compat\smoke-results'
$java21 = 'C:\Program Files\Java\jdk-21'
$java25 = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
$timeoutSeconds = 240

$targets = @(
    @{ Minecraft = '1.21.1'; Yarn = '1.21.1+build.3'; FabricApi = '0.116.15+1.21.1'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v01' },
    @{ Minecraft = '1.21.2'; Yarn = '1.21.2+build.1'; FabricApi = '0.106.1+1.21.2'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v02' },
    @{ Minecraft = '1.21.3'; Yarn = '1.21.3+build.2'; FabricApi = '0.114.1+1.21.3'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v03' },
    @{ Minecraft = '1.21.4'; Yarn = '1.21.4+build.8'; FabricApi = '0.119.4+1.21.4'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v04' },
    @{ Minecraft = '1.21.5'; Yarn = '1.21.5+build.1'; FabricApi = '0.128.2+1.21.5'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v05' },
    @{ Minecraft = '1.21.6'; Yarn = '1.21.6+build.1'; FabricApi = '0.128.2+1.21.6'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v06' },
    @{ Minecraft = '1.21.7'; Yarn = '1.21.7+build.8'; FabricApi = '0.129.0+1.21.7'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v07' },
    @{ Minecraft = '1.21.8'; Yarn = '1.21.8+build.1'; FabricApi = '0.136.1+1.21.8'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v08' },
    @{ Minecraft = '1.21.9'; Yarn = '1.21.9+build.1'; FabricApi = '0.134.1+1.21.9'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v09' },
    @{ Minecraft = '1.21.10'; Yarn = '1.21.10+build.3'; FabricApi = '0.138.4+1.21.10'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v10' },
    @{ Minecraft = '1.21.11'; Yarn = '1.21.11+build.6'; FabricApi = '0.141.6+1.21.11'; Java = $java21; Profile = 'versions\smoke-1.21.1'; Layer = 'v11' },
    @{ Minecraft = '26.1'; FabricApi = '0.145.1+26.1'; Java = $java25; Profile = 'versions\smoke-26.2'; Layer = 'v12' },
    @{ Minecraft = '26.2'; FabricApi = '0.157.0+26.2'; Java = $java25; Profile = 'versions\smoke-26.2'; Layer = 'v13' }
)

New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
$results = @()

foreach ($target in $targets) {
    $minecraft = $target.Minecraft
    $stdout = Join-Path $resultsDir "$minecraft.stdout.log"
    $stderr = Join-Path $resultsDir "$minecraft.stderr.log"
    Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue

    $env:JAVA_HOME = $target.Java
    $env:Path = "$($target.Java)\bin;$env:Path"
    $arguments = @(
        '-p', $target.Profile,
        'runClient', '--no-daemon',
        "-Psmoke_minecraft=$minecraft",
        "-Psmoke_fabric_api=$($target.FabricApi)"
    )
    if ($target.Yarn) {
        $arguments += "-Psmoke_yarn=$($target.Yarn)"
    }

    Write-Host "Smoke testing Minecraft $minecraft ($($target.Layer))..."
    $process = Start-Process -FilePath $gradle -ArgumentList $arguments -WorkingDirectory $projectRoot `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru -WindowStyle Hidden

    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    $initialized = $false
    $reachedResources = $false
    $fatal = $false
    while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
        Start-Sleep -Seconds 1
        $text = ((Get-Content -LiteralPath $stdout, $stderr -Raw -ErrorAction SilentlyContinue) -join "`n")
        $initialized = $text.Contains("Loaded AeroSync compatibility layer for Minecraft $minecraft")
        $reachedResources = $text.Contains('Reloading ResourceManager')
        $fatal = $text -match 'Uncaught exception in thread "main"|Could not execute entrypoint|BUILD FAILED'
        if ($fatal -or ($initialized -and $reachedResources)) {
            break
        }
        $process.Refresh()
    }

    if (-not $process.HasExited) {
        & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        $process.WaitForExit()
    }

    $passed = $initialized -and $reachedResources -and -not $fatal
    $results += [PSCustomObject]@{
        Minecraft = $minecraft
        Layer = $target.Layer
        Initialized = $initialized
        MainMenuResources = $reachedResources
        FatalError = $fatal
        Passed = $passed
    }
    if ($passed) {
        Write-Host "PASS $minecraft"
    } else {
        Write-Host "FAIL $minecraft"
    }
}

$results | Export-Csv -LiteralPath (Join-Path $resultsDir 'summary.csv') -NoTypeInformation -Encoding UTF8
$results | Format-Table -AutoSize
if ($results.Where({ -not $_.Passed }).Count -gt 0) {
    exit 1
}
