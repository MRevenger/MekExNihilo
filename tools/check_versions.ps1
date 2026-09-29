# Checks the Ex Deorum / Mekanism API surface this addon depends on against every candidate jar,
# using javap. Much faster than booting a server per version, and it tells us exactly which member
# is missing when a version is unsupported.
#
# Usage: pwsh -File tools/check_versions.ps1 -Target exd|mek
param(
    [Parameter(Mandatory = $true)][ValidateSet('exd', 'mek')][string]$Target
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$cache = Join-Path $root 'libs\versions'
New-Item -ItemType Directory -Force -Path $cache | Out-Null

# Every class/member pair the addon actually compiles against.
$checks = @{
    exd = @(
        @{ Class = 'thedarkcolour.exdeorum.registry.EItems'; Members = @('STRING_MESH', 'FLINT_MESH', 'IRON_MESH', 'GOLDEN_MESH', 'DIAMOND_MESH', 'NETHERITE_MESH') },
        @{ Class = 'thedarkcolour.exdeorum.tag.EItemTags'; Members = @('SIEVE_MESHES') },
        @{ Class = 'thedarkcolour.exdeorum.registry.ERecipeTypes'; Members = @('SIEVE') },
        @{ Class = 'thedarkcolour.exdeorum.recipe.sieve.SieveRecipe'; Members = @('mesh', 'byHandOnly') },
        @{ Class = 'thedarkcolour.exdeorum.recipe.SingleIngredientRecipe'; Members = @('ingredient') },
        @{ Class = 'thedarkcolour.exdeorum.recipe.ProbabilityRecipe'; Members = @('result', 'resultAmount') }
    )
    mek = @(
        @{ Class = 'mekanism.common.tile.prefab.TileEntityConfigurableMachine'; Members = @() },
        @{ Class = 'mekanism.common.content.blocktype.Machine'; Members = @() },
        @{ Class = 'mekanism.common.content.blocktype.Machine$MachineBuilder'; Members = @('createMachine') },
        @{ Class = 'mekanism.common.block.attribute.AttributeUpgradeable'; Members = @('upgradeResult') },
        @{ Class = 'mekanism.common.block.attribute.AttributeTier'; Members = @() },
        @{ Class = 'mekanism.common.block.attribute.AttributeSideConfig'; Members = @('ELECTRIC_MACHINE') },
        @{ Class = 'mekanism.common.capabilities.energy.MachineEnergyContainer'; Members = @('input') },
        @{ Class = 'mekanism.common.inventory.slot.InputInventorySlot'; Members = @('at') },
        @{ Class = 'mekanism.common.inventory.slot.OutputInventorySlot'; Members = @('at') },
        @{ Class = 'mekanism.common.inventory.slot.EnergyInventorySlot'; Members = @('fillOrConvert') },
        @{ Class = 'mekanism.common.inventory.slot.BasicInventorySlot'; Members = @('setStackUnchecked') },
        @{ Class = 'mekanism.common.tile.component.TileComponentEjector'; Members = @() },
        @{ Class = 'mekanism.common.tile.component.config.ConfigInfo'; Members = @('addSlotInfo') },
        @{ Class = 'mekanism.common.tile.component.config.slot.InventorySlotInfo'; Members = @() },
        @{ Class = 'mekanism.common.upgrade.IUpgradeData'; Members = @() },
        @{ Class = 'mekanism.common.util.MekanismUtils'; Members = @('getTicks') },
        @{ Class = 'mekanism.common.util.WorldUtils'; Members = @('dismantleBlock') },
        @{ Class = 'mekanism.common.tile.base.WrenchResult'; Members = @('DISMANTLED', 'PASS') },
        @{ Class = 'mekanism.common.tier.FactoryTier'; Members = @('BASIC', 'ULTIMATE', 'processes') },
        @{ Class = 'mekanism.common.registries.MekanismDataComponents'; Members = @('EJECTOR', 'SIDE_CONFIG') },
        @{ Class = 'mekanism.common.registration.impl.BlockDeferredRegister'; Members = @('register') },
        @{ Class = 'mekanism.common.registration.impl.ContainerTypeDeferredRegister'; Members = @('register') },
        @{ Class = 'mekanism.common.registration.impl.TileEntityTypeDeferredRegister'; Members = @('mekBuilder') },
        @{ Class = 'mekanism.common.registration.impl.CreativeTabDeferredRegister'; Members = @('addToDisplay') },
        @{ Class = 'mekanism.client.gui.GuiConfigurableTile'; Members = @() },
        @{ Class = 'mekanism.client.gui.element.bar.GuiVerticalPowerBar'; Members = @() },
        @{ Class = 'mekanism.client.gui.element.progress.GuiProgress'; Members = @() },
        @{ Class = 'mekanism.client.gui.element.tab.GuiEnergyTab'; Members = @() },
        @{ Class = 'mekanism.common.tile.base.TileEntityMekanism'; Members = @('tryWrench', 'getComponent', 'getSupportedUpgrade') }
    )
}

function Get-Versions {
    if ($Target -eq 'exd') {
        $json = Get-Content (Join-Path $root 'tools\exd-versions.json') -Raw | ConvertFrom-Json
        return $json | Sort-Object { [version]$_.version_number } | ForEach-Object {
            $file = $_.files | Where-Object { $_.filename -like '*.jar' -and $_.filename -notlike '*sources*' } | Select-Object -First 1
            [PSCustomObject]@{ Version = $_.version_number; Url = $file.url; Name = $file.filename }
        }
    }
    # Mekanism: only the ends of the line plus a couple of milestones, since it is a stable branch.
    $all = @('1.21.1-10.7.0.55', '1.21.1-10.7.5.62', '1.21.1-10.7.10.73', '1.21.1-10.7.15.81', '1.21.1-10.7.19.85')
    return $all | ForEach-Object {
        [PSCustomObject]@{
            Version = $_
            Url     = "https://modmaven.dev/mekanism/Mekanism/$_/Mekanism-$_.jar"
            Name    = "Mekanism-$_.jar"
        }
    }
}

function Test-Jar($jar, $list) {
    $missing = @()
    foreach ($check in $list) {
        $out = & javap -classpath $jar $check.Class 2>&1 | Out-String
        if ($LASTEXITCODE -ne 0 -or $out -match 'Error:|not found') {
            $missing += "$($check.Class) (class missing)"
            continue
        }
        foreach ($member in $check.Members) {
            if ($out -notmatch [regex]::Escape($member)) {
                $missing += "$($check.Class)#$member"
            }
        }
    }
    return $missing
}

$results = @()
foreach ($v in Get-Versions) {
    $jar = Join-Path $cache $v.Name
    if (-not (Test-Path $jar)) {
        try {
            Invoke-WebRequest -Uri $v.Url -OutFile $jar -TimeoutSec 180 -UseBasicParsing
        } catch {
            Write-Host ("  {0,-22} DOWNLOAD FAILED: {1}" -f $v.Version, $_.Exception.Message)
            $results += [PSCustomObject]@{ Version = $v.Version; Ok = $false; Note = 'download failed' }
            continue
        }
    }
    $missing = Test-Jar $jar $checks[$Target]
    $ok = $missing.Count -eq 0
    $note = if ($ok) { 'all required members present' } else { $missing -join ', ' }
    Write-Host ("  {0,-22} {1}  {2}" -f $v.Version, $(if ($ok) { 'OK  ' } else { 'FAIL' }), $note)
    $results += [PSCustomObject]@{ Version = $v.Version; Ok = $ok; Note = $note }
}

$okVersions = $results | Where-Object { $_.Ok } | ForEach-Object { $_.Version }
Write-Host ""
Write-Host "=== $Target summary ==="
Write-Host "  compatible: $($okVersions -join ', ')"
Write-Host "  incompatible: $(($results | Where-Object { -not $_.Ok } | ForEach-Object { $_.Version }) -join ', ')"
