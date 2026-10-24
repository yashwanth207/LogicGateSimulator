# LogicGrid - Digital Circuit Simulator PowerShell Runner
Set-Location $PSScriptRoot

$java = "java"
$javac = "javac"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $found = Get-ChildItem -Path "$env:USERPROFILE\.antigravity-ide\extensions\redhat.java-*\jre\*\bin\java.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $java = $found.FullName
        $javac = Join-Path (Split-Path $java) "javac.exe"
    }
}

if (-not (Test-Path dist\LogicGateSimulator.jar)) {
    if (-not (Test-Path out\logicsim\LogicGateSimulator.class)) {
        & $javac -encoding UTF-8 -sourcepath src -d out src\logicsim\LogicGateSimulator.java
    }
    & $java -cp out logicsim.LogicGateSimulator
} else {
    & $java -jar dist\LogicGateSimulator.jar
}
