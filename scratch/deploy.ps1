$sdk = "C:\Users\Khalil\AppData\Local\Android\Sdk"
$emulator = "$sdk\emulator\emulator.exe"
$adb = "$sdk\platform-tools\adb.exe"
$apk = "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "Starting emulator..."
Start-Process -FilePath $emulator -ArgumentList "-avd Pixel_8 -no-snapshot-load"

Write-Host "Waiting for emulator to register..."
$attached = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 2
    $devices = & $adb devices
    if ($devices -match "emulator-5554\s+device") {
        Write-Host "Device detected!"
        $attached = $true
        break
    }
    Write-Host "Still waiting for device..."
}

if (-not $attached) {
    # If match failed, let's check any device
    if ((& $adb devices) -match "device\b") {
        Write-Host "Some device detected!"
        $attached = $true
    } else {
        Write-Error "Emulator did not start in time."
        exit 1
    }
}

Write-Host "Waiting for boot completion..."
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 2
    $booted = & $adb shell getprop sys.boot_completed
    if ($booted.Trim() -eq "1") {
        Write-Host "Boot completed!"
        break
    }
    Write-Host "Still booting..."
}

Write-Host "Installing APK..."
& $adb install -r $apk

Write-Host "Launching app..."
& $adb shell monkey -p com.aistudio.fintrackdz.agkdlm -c android.intent.category.LAUNCHER 1

Write-Host "Deployment completed successfully!"
