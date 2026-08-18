$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$version = '0.0.9'
$inputDir = Join-Path $projectRoot "releases\$version"
$outputJar = Join-Path $inputDir "aerosync-neoforge-mc1.21.1-26.2-v$version.jar"
$fabricJar = Join-Path $inputDir "aerosync-fabric-mc1.21.1-26.2-v$version.jar"
if (-not (Test-Path -LiteralPath $fabricJar)) {
    $fabricJar = Join-Path $inputDir "aerosync-fabric-$version.jar"
}
$targets = @(
    @{ Minecraft = '1.21.1'; Prefix = 'n01' }, @{ Minecraft = '1.21.2'; Prefix = 'n02' },
    @{ Minecraft = '1.21.3'; Prefix = 'n03' }, @{ Minecraft = '1.21.4'; Prefix = 'n04' },
    @{ Minecraft = '1.21.5'; Prefix = 'n05' }, @{ Minecraft = '1.21.6'; Prefix = 'n06' },
    @{ Minecraft = '1.21.7'; Prefix = 'n07' }, @{ Minecraft = '1.21.8'; Prefix = 'n08' },
    @{ Minecraft = '1.21.9'; Prefix = 'n09' }, @{ Minecraft = '1.21.10'; Prefix = 'n10' },
    @{ Minecraft = '1.21.11'; Prefix = 'n11' }, @{ Minecraft = '26.1'; Prefix = 'n12' },
    @{ Minecraft = '26.2'; Prefix = 'n13' }
)

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-Bytes([IO.Compression.ZipArchiveEntry] $entry) {
    $stream = $entry.Open()
    try {
        $memory = [IO.MemoryStream]::new()
        try { $stream.CopyTo($memory); return $memory.ToArray() } finally { $memory.Dispose() }
    } finally { $stream.Dispose() }
}

function Replace-Ascii([byte[]] $bytes, [string] $oldValue, [string] $newValue) {
    $old = [Text.Encoding]::ASCII.GetBytes($oldValue)
    $new = [Text.Encoding]::ASCII.GetBytes($newValue)
    if ($old.Length -ne $new.Length) { throw 'Binary replacements must have equal lengths' }
    $result = [byte[]]$bytes.Clone()
    for ($offset = 0; $offset -le $result.Length - $old.Length; $offset++) {
        $matches = $true
        for ($index = 0; $index -lt $old.Length; $index++) {
            if ($result[$offset + $index] -ne $old[$index]) { $matches = $false; break }
        }
        if ($matches) {
            [Array]::Copy($new, 0, $result, $offset, $new.Length)
            $offset += $old.Length - 1
        }
    }
    return $result
}

function Add-Bytes($archive, $names, [string] $name, [byte[]] $bytes) {
    $name = $name.Replace('\', '/')
    if (-not $names.Add($name)) { return }
    $entry = $archive.CreateEntry($name, [IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    try { $stream.Write($bytes, 0, $bytes.Length) } finally { $stream.Dispose() }
}

function Add-Text($archive, $names, [string] $name, [string] $value) {
    Add-Bytes $archive $names $name ([Text.UTF8Encoding]::new($false).GetBytes($value))
}

if (-not (Test-Path -LiteralPath $fabricJar)) { throw "Missing Fabric dependency source: $fabricJar" }
foreach ($target in $targets) {
    $target.Jar = Join-Path $inputDir "aerosync-neoforge-mc$($target.Minecraft)-$version.jar"
    if (-not (Test-Path -LiteralPath $target.Jar)) { throw "Missing compatibility build: $($target.Jar)" }
}
if (Test-Path -LiteralPath $outputJar) { Remove-Item -LiteralPath $outputJar -Force }

$file = [IO.File]::Open($outputJar, [IO.FileMode]::CreateNew)
$output = [IO.Compression.ZipArchive]::new($file, [IO.Compression.ZipArchiveMode]::Create)
$names = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
try {
    $dependencies = [IO.Compression.ZipFile]::OpenRead($fabricJar)
    try {
        foreach ($entry in $dependencies.Entries) {
            $name = $entry.FullName
            if ($name.EndsWith('/') -or $name -eq 'fabric.mod.json' -or
                $name -eq 'META-INF/MANIFEST.MF' -or $name -like 'aerosync-v*.mixins.json' -or
                $name.StartsWith('com/aerosync/') -or $name -match '^v\d\d/aerosync/' -or
                $name.StartsWith('assets/aerosync/') -or $name -match '^META-INF/[^/]+\.(SF|RSA|DSA)$') { continue }
            if ($name.StartsWith('com/sun/jna/') -or
                $name -match '^META-INF/versions/\d+/com/sun/jna/' -or
                $name.StartsWith('META-INF/native-image/com.sun.jna/')) { continue }
            Add-Bytes $output $names $name (Read-Bytes $entry)
        }
    } finally { $dependencies.Dispose() }

    $base = [IO.Compression.ZipFile]::OpenRead($targets[0].Jar)
    try {
        foreach ($entry in $base.Entries) {
            if (($entry.FullName.StartsWith('assets/aerosync/') -and -not $entry.FullName.EndsWith('/')) -or
                $entry.FullName -eq 'com/aerosync/neoforge/AeroSyncNeoForgeMod.class') {
                Add-Bytes $output $names $entry.FullName (Read-Bytes $entry)
            }
        }
    } finally { $base.Dispose() }

    foreach ($target in $targets) {
        $zip = [IO.Compression.ZipFile]::OpenRead($target.Jar)
        try {
            foreach ($entry in $zip.Entries) {
                if (-not $entry.FullName.StartsWith('com/aerosync/') -or
                    -not $entry.FullName.EndsWith('.class') -or
                    $entry.FullName.StartsWith('com/aerosync/universal/') -or
                    $entry.FullName -eq 'com/aerosync/neoforge/AeroSyncNeoForgeMod.class') { continue }
                $destination = $entry.FullName.Replace('com/aerosync/', "$($target.Prefix)/aerosync/")
                $bytes = Replace-Ascii (Read-Bytes $entry) 'com/aerosync' "$($target.Prefix)/aerosync"
                Add-Bytes $output $names $destination $bytes
            }
        } finally { $zip.Dispose() }
    }

    $metadata = @"
modLoader="javafml"
loaderVersion="[4,)"
license="All Rights Reserved"

[[mods]]
modId="aerosync"
version="$version"
displayName="AeroSync"
displayTest="IGNORE_ALL_VERSION"
logoFile="assets/aerosync/icon.png"
authors="StandoffKitty75, K2 Studio"
description='''Share Minecraft worlds, configs, resource packs and other selected game data through a single P2P address.'''

[[dependencies.aerosync]]
modId="neoforge"
type="required"
versionRange="[21.1,27)"
ordering="NONE"
side="CLIENT"

[[dependencies.aerosync]]
modId="minecraft"
type="required"
versionRange="[1.21.1,26.2]"
ordering="NONE"
side="CLIENT"
"@
    Add-Text $output $names 'META-INF/neoforge.mods.toml' $metadata
    Add-Text $output $names 'META-INF/MANIFEST.MF' "Manifest-Version: 1.0`r`nImplementation-Title: AeroSync`r`nImplementation-Version: $version`r`n`r`n"
} finally {
    $output.Dispose()
    $file.Dispose()
}

Write-Host "Universal AeroSync NeoForge JAR: $outputJar"
