#Requires -Version 7.0
param([switch]$Quick)
$args = @()
if ($Quick) { $args += '--quick' }
& bash (Join-Path $PSScriptRoot 'validate-bootstrap.sh') @args
exit $LASTEXITCODE
