# Idempotent GitHub repo setup (Windows wrapper).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$bash = "C:\Program Files\Git\bin\bash.exe"
if (-not (Test-Path $bash)) {
    Write-Error "Git Bash required at $bash"
}
& $bash -lc "cd '$($Root -replace '\\','/')' && bash scripts/setup-github-repo.sh @args"
