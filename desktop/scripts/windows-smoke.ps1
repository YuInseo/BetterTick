$ErrorActionPreference = 'Stop'
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class BetterTickWindow {
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hwnd);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr hwnd, uint msg, IntPtr wParam, IntPtr lParam);
  [DllImport("user32.dll")] public static extern void keybd_event(byte key, byte scan, uint flags, UIntPtr extra);
}
'@
$app = Join-Path $PSScriptRoot '../src-tauri/target/release/bettertick-desktop.exe'
if (!(Test-Path $app)) { throw "Built executable missing: $app" }
$process = Start-Process -FilePath $app -PassThru -RedirectStandardError (Join-Path $PSScriptRoot '../test-results/native-stderr.log') -RedirectStandardOutput (Join-Path $PSScriptRoot '../test-results/native-stdout.log')
try {
  $deadline = (Get-Date).AddSeconds(45)
  do {
    Start-Sleep -Milliseconds 500
    $process.Refresh()
    if ($process.HasExited) { throw "Desktop application exited during startup: $($process.ExitCode)" }
  } until ($process.MainWindowHandle -ne [IntPtr]::Zero -or (Get-Date) -gt $deadline)
  $handle = $process.MainWindowHandle
  if ($handle -eq [IntPtr]::Zero) { throw 'Desktop window did not appear' }
  if (![BetterTickWindow]::IsWindowVisible($handle)) { throw 'Desktop window is not visible' }
  if ($process.MainWindowTitle -ne 'BetterTick') { throw "Unexpected window title: $($process.MainWindowTitle)" }
  Write-Host 'PASS: Built Windows executable starts and displays the BetterTick window.'
  # Test the exact release binary through Windows accessibility and real keyboard input.
  & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'native-ready.ps1') -WindowHandle ($handle.ToInt64())
  if ($LASTEXITCODE -ne 0) { throw 'The native WebView did not pass its UI check' }
  $process.Refresh()
  if ($process.HasExited) { throw "Native process exited after UI readiness (exit $($process.ExitCode))" }
  if ($process.MainWindowHandle -ne [IntPtr]::Zero) { $handle = $process.MainWindowHandle }
  [void][BetterTickWindow]::PostMessage($handle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
  Start-Sleep -Seconds 2
  $process.Refresh()
  if ($process.HasExited) { throw "Closing the window terminated the tray application (exit $($process.ExitCode)): $(Get-Content (Join-Path $PSScriptRoot '../test-results/native-stderr.log') -Raw)" }
  if ([BetterTickWindow]::IsWindowVisible($handle)) { throw 'Window did not hide to tray' }
  Write-Host 'PASS: Closing the window keeps the process alive and hides to tray.'
  # Ctrl+Shift+A exercises the registered native shortcut and restores the hidden window.
  [BetterTickWindow]::keybd_event(0x11, 0, 0, [UIntPtr]::Zero)
  [BetterTickWindow]::keybd_event(0x10, 0, 0, [UIntPtr]::Zero)
  [BetterTickWindow]::keybd_event(0x41, 0, 0, [UIntPtr]::Zero)
  [BetterTickWindow]::keybd_event(0x41, 0, 2, [UIntPtr]::Zero)
  [BetterTickWindow]::keybd_event(0x10, 0, 2, [UIntPtr]::Zero)
  [BetterTickWindow]::keybd_event(0x11, 0, 2, [UIntPtr]::Zero)
  $deadline = (Get-Date).AddSeconds(10)
  do { Start-Sleep -Milliseconds 250 } until ([BetterTickWindow]::IsWindowVisible($handle) -or (Get-Date) -gt $deadline)
  if (![BetterTickWindow]::IsWindowVisible($handle)) { throw 'Global quick-add shortcut did not restore the native window' }
  Write-Host 'PASS: Ctrl+Shift+A restores the native window from tray.'
} finally {
  if (!$process.HasExited) { Stop-Process -Id $process.Id -Force }
}
