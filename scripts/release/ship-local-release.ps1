# Build signed release locally and upload to GitHub Releases.
# Usage: .\scripts\release\ship-local-release.ps1
#        .\scripts\release\ship-local-release.ps1 -UseCi
param(
    [string]$Tag = "",
    [switch]$UseCi,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $Root

$bash = "C:\Program Files\Git\bin\bash.exe"
if (-not (Test-Path $bash)) { throw "Git Bash required at $bash" }

$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }

if ($UseCi) { $env:USE_CI = "1" }
if ($SkipBuild) { $env:SKIP_BUILD = "1" }

$args = @("-lc", "cd '$($Root -replace '\\','/')' && bash scripts/release/ship-local-release.sh $Tag")
& $bash @args
