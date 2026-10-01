param([string]$DataFile = 'data/books.csv')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build | Out-Null
    javac -encoding UTF-8 --release 17 -d build src/*.java
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    java '-Dfile.encoding=UTF-8' -cp build Main $DataFile
    if ($LASTEXITCODE -ne 0) { throw 'Application failed to start or run.' }
} finally {
    Pop-Location
}
