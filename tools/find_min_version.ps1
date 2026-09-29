# Finds the oldest Mekanism / Ex Deorum the addon actually compiles against.
#
# javap name checks are not enough: Mekanism changed method *signatures* between 10.7.x releases
# (for example TileEntityMekanism's block parameter became Holder<Block> instead of IBlockProvider),
# which only a real compile catches.
#
# Usage: pwsh -File tools/find_min_version.ps1 -Target mek|exd [-Versions a,b,c]
param(
    [Parameter(Mandatory = $true)][ValidateSet('mek', 'exd')][string]$Target,
    [string]$Versions = ''
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$props = Join-Path $root 'MekNihilo\gradle.properties'
$buildPs = Join-Path $root 'build.ps1'
$key = if ($Target -eq 'mek') { 'mekanism_version' } else { 'exdeorum_version' }

if ($Versions) {
    $list = $Versions.Split(',')
} elseif ($Target -eq 'mek') {
    # Whole 10.7.x line for 1.21.1, newest first.
    $list = @('1.21.1-10.7.19.85', '1.21.1-10.7.18.84', '1.21.1-10.7.17.83', '1.21.1-10.7.16.82',
        '1.21.1-10.7.15.81', '1.21.1-10.7.14.79', '1.21.1-10.7.13.78', '1.21.1-10.7.12.77',
        '1.21.1-10.7.11.76', '1.21.1-10.7.10.73', '1.21.1-10.7.9.72', '1.21.1-10.7.8.70',
        '1.21.1-10.7.7.64', '1.21.1-10.7.6.63', '1.21.1-10.7.5.62', '1.21.1-10.7.4.60',
        '1.21.1-10.7.3.59', '1.21.1-10.7.2.58', '1.21.1-10.7.1.57', '1.21.1-10.7.1.56',
        '1.21.1-10.7.0.55')
} else {
    $list = @('3.3', '3.4', '3.5', '3.6', '3.7', '3.8', '3.9', '3.10', '3.11', '3.12')
}

$results = @()
foreach ($v in $list) {
    $content = Get-Content $props -Raw
    $content = $content -replace "(?m)^$key=.*$", "$key=$v"
    Set-Content $props -Value $content -NoNewline

    $out = & $buildPs compileJava 2>&1 | Out-String
    $ok = $LASTEXITCODE -eq 0
    $firstError = @($out -split "`n" | Where-Object { $_ -match 'error:' } | Select-Object -First 1)
    $firstError = if ($firstError.Count -gt 0 -and $null -ne $firstError[0]) { "$($firstError[0])".Trim() } else { '' }
    Write-Host ("  {0,-22} {1}  {2}" -f $v, $(if ($ok) { 'COMPILES' } else { 'FAILS   ' }), $firstError)
    $results += [PSCustomObject]@{ Version = $v; Ok = $ok; Error = $firstError }
}

Write-Host ""
Write-Host "=== $Target results (newest first) ==="
$results | ForEach-Object { Write-Host ("  {0,-22} {1}" -f $_.Version, $(if ($_.Ok) { 'OK' } else { 'FAIL' })) }
# The list is newest-first, so the last OK entry is the oldest supported one.
$oldest = ($results | Where-Object { $_.Ok } | Select-Object -Last 1).Version
$newestBad = ($results | Where-Object { -not $_.Ok } | Select-Object -First 1).Version
Write-Host ""
Write-Host "  oldest compiling version: $oldest"
if ($newestBad) { Write-Host "  newest failing version : $newestBad" }
