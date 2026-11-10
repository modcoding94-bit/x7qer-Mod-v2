$ErrorActionPreference = 'Stop'
$toolDir = Join-Path $PSScriptRoot '.tools'
$extractDir = Join-Path $toolDir 'jdk-download'
$archive = Join-Path $toolDir 'jdk25.zip'
$homeFile = Join-Path $toolDir 'jdk-home.txt'

if (Test-Path -LiteralPath $homeFile) {
    $existing = (Get-Content -LiteralPath $homeFile -Raw).Trim()
    if (Test-Path -LiteralPath (Join-Path $existing 'bin\java.exe')) { exit 0 }
}

New-Item -ItemType Directory -Force -Path $toolDir, $extractDir | Out-Null
Write-Host 'Lade ein portables JDK 25 von Adoptium herunter (ca. 200 MB)...'
Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $archive
Write-Host 'Entpacke JDK...'
Expand-Archive -LiteralPath $archive -DestinationPath $extractDir -Force
$jdk = Get-ChildItem -LiteralPath $extractDir -Directory | Select-Object -First 1
if (-not $jdk -or -not (Test-Path -LiteralPath (Join-Path $jdk.FullName 'bin\java.exe'))) {
    throw 'Im heruntergeladenen Archiv wurde kein gültiges JDK gefunden.'
}
Set-Content -LiteralPath $homeFile -Value $jdk.FullName -NoNewline
Write-Host 'JDK 25 ist bereit.'
