$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$version = '0.0.9'
$inputDir = Join-Path $projectRoot "releases\$version"
$outputJar = Join-Path $inputDir "aerosync-fabric-mc1.21.1-26.2-v$version.jar"

$targets = @(
    @{ Minecraft = '1.21.1'; Prefix = 'v01' },
    @{ Minecraft = '1.21.2'; Prefix = 'v02' },
    @{ Minecraft = '1.21.3'; Prefix = 'v03' },
    @{ Minecraft = '1.21.4'; Prefix = 'v04' },
    @{ Minecraft = '1.21.5'; Prefix = 'v05' },
    @{ Minecraft = '1.21.6'; Prefix = 'v06' },
    @{ Minecraft = '1.21.7'; Prefix = 'v07' },
    @{ Minecraft = '1.21.8'; Prefix = 'v08' },
    @{ Minecraft = '1.21.9'; Prefix = 'v09' },
    @{ Minecraft = '1.21.10'; Prefix = 'v10' },
    @{ Minecraft = '1.21.11'; Prefix = 'v11' },
    @{ Minecraft = '26.1'; Prefix = 'v12' },
    @{ Minecraft = '26.2'; Prefix = 'v13' }
)

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-ZipEntryBytes([System.IO.Compression.ZipArchiveEntry] $entry) {
    $stream = $entry.Open()
    try {
        $memory = [System.IO.MemoryStream]::new()
        try {
            $stream.CopyTo($memory)
            return $memory.ToArray()
        } finally {
            $memory.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

function Replace-AsciiBytes([byte[]] $bytes, [string] $oldValue, [string] $newValue) {
    $oldBytes = [Text.Encoding]::ASCII.GetBytes($oldValue)
    $newBytes = [Text.Encoding]::ASCII.GetBytes($newValue)
    if ($oldBytes.Length -ne $newBytes.Length) {
        throw "Binary replacements must have equal lengths: $oldValue -> $newValue"
    }

    $result = [byte[]]$bytes.Clone()
    for ($offset = 0; $offset -le $result.Length - $oldBytes.Length; $offset++) {
        $matches = $true
        for ($index = 0; $index -lt $oldBytes.Length; $index++) {
            if ($result[$offset + $index] -ne $oldBytes[$index]) {
                $matches = $false
                break
            }
        }
        if ($matches) {
            [Array]::Copy($newBytes, 0, $result, $offset, $newBytes.Length)
            $offset += $oldBytes.Length - 1
        }
    }
    return $result
}

function Add-ZipBytes(
    [System.IO.Compression.ZipArchive] $archive,
    [System.Collections.Generic.HashSet[string]] $names,
    [string] $name,
    [byte[]] $bytes
) {
    $normalizedName = $name.Replace('\', '/')
    if (-not $names.Add($normalizedName)) {
        return
    }
    $entry = $archive.CreateEntry($normalizedName, [System.IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    try {
        $stream.Write($bytes, 0, $bytes.Length)
    } finally {
        $stream.Dispose()
    }
}

function Add-ZipText(
    [System.IO.Compression.ZipArchive] $archive,
    [System.Collections.Generic.HashSet[string]] $names,
    [string] $name,
    [string] $text
) {
    Add-ZipBytes $archive $names $name ([Text.UTF8Encoding]::new($false).GetBytes($text))
}

foreach ($target in $targets) {
    $target.Jar = Join-Path $inputDir "aerosync-fabric-mc$($target.Minecraft)-$version.jar"
    if (-not (Test-Path -LiteralPath $target.Jar)) {
        throw "Missing compatibility build: $($target.Jar)"
    }
}

if (Test-Path -LiteralPath $outputJar) {
    Remove-Item -LiteralPath $outputJar -Force
}

$outputStream = [IO.File]::Open($outputJar, [IO.FileMode]::CreateNew)
$output = [IO.Compression.ZipArchive]::new($outputStream, [IO.Compression.ZipArchiveMode]::Create)
$entryNames = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)

try {
    $baseZip = [IO.Compression.ZipFile]::OpenRead($targets[0].Jar)
    try {
        foreach ($entry in $baseZip.Entries) {
            if ($entry.FullName.StartsWith('assets/aerosync/') -and -not $entry.FullName.EndsWith('/')) {
                Add-ZipBytes $output $entryNames $entry.FullName (Read-ZipEntryBytes $entry)
            }
            if ($entry.FullName.StartsWith('com/aerosync/universal/') -and $entry.FullName.EndsWith('.class')) {
                Add-ZipBytes $output $entryNames $entry.FullName (Read-ZipEntryBytes $entry)
            }
        }

        foreach ($nestedEntry in $baseZip.Entries | Where-Object { $_.FullName -like 'META-INF/jars/*.jar' }) {
            $nestedBytes = Read-ZipEntryBytes $nestedEntry
            $nestedStream = [IO.MemoryStream]::new($nestedBytes)
            $nestedZip = [IO.Compression.ZipArchive]::new($nestedStream, [IO.Compression.ZipArchiveMode]::Read)
            try {
                foreach ($entry in $nestedZip.Entries) {
                    $name = $entry.FullName
                    if ($name.EndsWith('/') -or
                        $name -eq 'fabric.mod.json' -or
                        $name -eq 'META-INF/MANIFEST.MF' -or
                        $name -match '^META-INF/[^/]+\.(SF|RSA|DSA)$') {
                        continue
                    }
                    Add-ZipBytes $output $entryNames $name (Read-ZipEntryBytes $entry)
                }
            } finally {
                $nestedZip.Dispose()
                $nestedStream.Dispose()
            }
        }
    } finally {
        $baseZip.Dispose()
    }

    foreach ($target in $targets) {
        $zip = [IO.Compression.ZipFile]::OpenRead($target.Jar)
        try {
            foreach ($entry in $zip.Entries) {
                if (-not $entry.FullName.StartsWith('com/aerosync/') -or
                    -not $entry.FullName.EndsWith('.class') -or
                    $entry.FullName.StartsWith('com/aerosync/universal/')) {
                    continue
                }

                $destination = $entry.FullName.Replace('com/aerosync/', "$($target.Prefix)/aerosync/")
                $bytes = Replace-AsciiBytes (Read-ZipEntryBytes $entry) 'com/aerosync' "$($target.Prefix)/aerosync"
                Add-ZipBytes $output $entryNames $destination $bytes
            }
        } finally {
            $zip.Dispose()
        }

        $mixinConfig = [ordered]@{
            required = $false
            minVersion = '0.8'
            package = "$($target.Prefix).aerosync.mixin"
            compatibilityLevel = 'JAVA_21'
            plugin = 'com.aerosync.universal.AeroSyncVersionMixinPlugin'
            client = @('TitleScreenMixin', 'GameMenuScreenMixin')
            injectors = [ordered]@{ defaultRequire = 1 }
        } | ConvertTo-Json -Depth 5
        Add-ZipText $output $entryNames "aerosync-$($target.Prefix).mixins.json" $mixinConfig
    }

    $metadata = [ordered]@{
        schemaVersion = 1
        id = 'aerosync'
        version = $version
        name = 'AeroSync'
        description = 'Share Minecraft worlds, configs, resource packs and other selected game data through a single P2P address.'
        authors = @('StandoffKitty75', 'K2 Studio')
        contact = [ordered]@{}
        environment = 'client'
        entrypoints = [ordered]@{
            client = @('com.aerosync.universal.AeroSyncUniversalClientMod')
        }
        mixins = @($targets | ForEach-Object { "aerosync-$($_.Prefix).mixins.json" })
        depends = [ordered]@{
            fabricloader = '>=0.19.3'
            'fabric-api' = '*'
            minecraft = '>=1.21.1 <=26.2'
            java = '>=21'
        }
        icon = 'assets/aerosync/icon.png'
    } | ConvertTo-Json -Depth 8
    Add-ZipText $output $entryNames 'fabric.mod.json' $metadata
    Add-ZipText $output $entryNames 'META-INF/MANIFEST.MF' "Manifest-Version: 1.0`r`nImplementation-Title: AeroSync`r`nImplementation-Version: $version`r`n`r`n"
} finally {
    $output.Dispose()
    $outputStream.Dispose()
}

Write-Host "Universal AeroSync JAR: $outputJar"
