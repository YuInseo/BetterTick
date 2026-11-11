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
$process = Start-Process -FilePath $app -PassThru
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
  Start-Sleep -Seconds 4
  [void][BetterTickWindow]::PostMessage($handle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
  Start-Sleep -Seconds 2
  $process.Refresh()
  if ($process.HasExited) { throw 'Closing the window terminated the tray application' }
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
