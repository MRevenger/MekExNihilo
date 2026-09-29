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

$gradle = Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.8-bin\4u0rgm4geyrm56fyhoco0c9in\gradle-8.8\bin\gradle.bat'
if (-not (Test-Path $gradle)) {
    throw "Cached Gradle 8.8 not found at $gradle"
}

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
