$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = Join-Path $root ".tools"
$sdk = Join-Path $root ".android-sdk"
$cmdlineTools = Join-Path $sdk "cmdline-tools\latest"
$jdk = (Get-ChildItem -Path $tools -Directory -Filter "jdk-17*" | Select-Object -First 1).FullName

if (-not $jdk) {
    throw "OpenJDK 17 was not found in .tools. Run the initial tool setup again."
}

$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:GRADLE_USER_HOME = Join-Path $root ".gradle-home"
$env:PATH = "$jdk\bin;$tools\gradle-8.10.2\bin;$cmdlineTools\bin;$sdk\platform-tools;$env:PATH"

& (Join-Path $tools "gradle-8.10.2\bin\gradle.bat") --no-daemon assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE."
}

$bridgeApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$simulatorApk = Join-Path $root "powerapps-simulator\build\outputs\apk\debug\powerapps-simulator-debug.apk"
Write-Host ""
Write-Host "APKs built:"
Write-Host $bridgeApk
Write-Host $simulatorApk
