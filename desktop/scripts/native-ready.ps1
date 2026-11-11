param([long]$WindowHandle)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes, System.Drawing, System.Windows.Forms
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class NativeFocus {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hwnd);
}
'@
$window = [System.Windows.Automation.AutomationElement]::FromHandle([IntPtr]$WindowHandle)
$scope = [System.Windows.Automation.TreeScope]::Descendants
function Find-Text($name) {
  $condition = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::NameProperty, $name)
  return $window.FindFirst($scope, $condition)
}
function Save-NativeScreenshot {
  $bounds = $window.Current.BoundingRectangle
  $bitmap = New-Object System.Drawing.Bitmap([int]$bounds.Width, [int]$bounds.Height)
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  try {
    $graphics.CopyFromScreen([int]$bounds.X, [int]$bounds.Y, 0, 0, $bitmap.Size)
    $bitmap.Save((Join-Path $PSScriptRoot '../test-results/native-webview.png'), [System.Drawing.Imaging.ImageFormat]::Png)
  } finally { $graphics.Dispose(); $bitmap.Dispose() }
}
try {
  $deadline = (Get-Date).AddSeconds(45)
  do {
    # Keep this script ASCII so Windows PowerShell 5.1 reads it consistently.
    $add = Find-Text (-join [char[]](0xc0c8, 0x20, 0xd560, 0x20, 0xc77c))
    if (!$add) { Start-Sleep -Milliseconds 500 }
  } until ($add -or (Get-Date) -gt $deadline)
  if (!$add) { throw 'The native accessibility tree did not expose the new-task button' }
  [void][NativeFocus]::SetForegroundWindow([IntPtr]$WindowHandle)
  $invoke = $add.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern)
  $invoke.Invoke()
  Start-Sleep -Milliseconds 300
  [System.Windows.Forms.SendKeys]::SendWait('Native UI QA{ENTER}')
  $deadline = (Get-Date).AddSeconds(15)
  do {
    $task = Find-Text 'Native UI QA'
    if (!$task) { Start-Sleep -Milliseconds 250 }
  } until ($task -or (Get-Date) -gt $deadline)
  if (!$task) { throw 'Typing and submitting a task did not create the native task row' }
  Save-NativeScreenshot
  Write-Host 'PASS: Native Windows accessibility and keyboard input create a task in the redesigned app.'
} catch {
  $window.FindAll($scope, [System.Windows.Automation.Condition]::TrueCondition) | ForEach-Object {
    "$($_.Current.ControlType.ProgrammaticName): $($_.Current.Name) [$($_.Current.AutomationId)]"
  } | Out-File (Join-Path $PSScriptRoot '../test-results/native-accessibility.log') -Encoding utf8
  Save-NativeScreenshot
  throw
}
