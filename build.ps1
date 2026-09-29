# Build helper for the MekNihilo NeoForge project.
# Works around two environment quirks:
#  1. JAVA_HOME is not set globally -> point at the local JDK 21.
#  2. Gradle's native library dir defaults to ~/.gradle, which is outside the
#     writable workspace sandbox -> relocate it into the workspace.
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs = @('build')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$env:JAVA_HOME = 'C:\Program Files\Java\dragonwell-21.0.10.0.10+7-GA'
$nativeDir = Join-Path $root '.gradle-home\native'
New-Item -ItemType Directory -Force -Path $nativeDir | Out-Null

# Heap must be given to the *launcher* JVM (via GRADLE_OPTS) rather than through
# org.gradle.jvmargs, otherwise Gradle forks a build daemon and the sandbox
# denies the required child-process pipe.
$env:GRADLE_OPTS = "-Xmx4G -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8 -Dorg.gradle.native.dir=$nativeDir"

# Pick a Gradle from the shared wrapper cache. Preference order: any 8.x (the version this project
# was developed against), newest first, then anything else.
$dists = Join-Path $env:USERPROFILE '.gradle\wrapper\dists'
$best = Get-ChildItem $dists -Recurse -Filter 'gradle.bat' -ErrorAction SilentlyContinue | ForEach-Object {
    if ($_.FullName -match 'gradle-(\d+\.\d+(?:\.\d+)?)-bin') {
        [PSCustomObject]@{ Path = $_.FullName; Major = [int]($Matches[1].Split('.')[0]); Version = [version]$Matches[1] }
    }
} | Sort-Object @{ Expression = { if ($_.Major -eq 8) { 0 } else { 1 } } }, @{ Expression = { $_.Version }; Descending = $true } |
    Select-Object -First 1
if (-not $best) {
    throw "No usable Gradle found under $dists. Install one or add a wrapper to the project."
}
$gradle = $best.Path
Write-Host "==> using Gradle $($best.Version) at $gradle" -ForegroundColor DarkGray

# Gradle writes caches and lock files under GRADLE_USER_HOME. The default
# ~/.gradle is outside the writable workspace sandbox (lock creation is denied),
# so the user home lives inside the workspace and is pre-seeded from ~/.gradle.
$gh = Join-Path $root '.gradle-home\user'
New-Item -ItemType Directory -Force -Path $gh | Out-Null
$env:GRADLE_USER_HOME = $gh

$projectDir = Join-Path $root 'MekNihilo'
Write-Host "==> gradle $($GradleArgs -join ' ') (in $projectDir)" -ForegroundColor Cyan
& $gradle @GradleArgs --no-daemon -p $projectDir
exit $LASTEXITCODE
