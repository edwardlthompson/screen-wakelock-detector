#Requires -Version 7.0
param(
  [switch]$Once,
  [switch]$Autofix,
  [switch]$NoAutofix,
  [string]$Step = 'gate'
)
$args = @()
if ($Once) { $args += '--once' }
if ($Autofix) { $args += '--autofix' }
if ($NoAutofix) { $args += '--no-autofix' }
if ($Step) { $args += '--step'; $args += $Step }
& bash (Join-Path $PSScriptRoot 'watch-agent-gates.sh') @args
exit $LASTEXITCODE
