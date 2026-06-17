param(
    [Parameter(Mandatory = $true)]
    [string]$MainWxsPath
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $MainWxsPath)) {
    throw "main.wxs not found: $MainWxsPath"
}

$content = Get-Content -Path $MainWxsPath -Raw

if ($content -match 'AutostartComponent') {
    Write-Host "main.wxs already patched for auto-start."
    exit 0
}

$autostartComponent = @'
  <DirectoryRef Id="TARGETDIR">
    <Component Id="AutostartComponent" Guid="A1B2C3D4-E5F6-4789-A012-3456789ABCDE">
      <RegistryKey Root="HKCU" Key="Software\Microsoft\Windows\CurrentVersion\Run">
        <RegistryValue Type="string" Name="$(var.JpAppName)" Value="&quot;[INSTALLDIR]$(var.JpAppName).exe&quot;" KeyPath="yes" />
      </RegistryKey>
    </Component>
  </DirectoryRef>

'@

$componentRef = '    <ComponentRef Id="AutostartComponent" />' + [Environment]::NewLine

if ($content -notmatch '</Feature>') {
    throw "Could not find </Feature> in main.wxs"
}

$content = $content -replace '</Feature>', ($componentRef + '  </Feature>')
$content = $content -replace '</Product>', ($autostartComponent + '</Product>')

Set-Content -Path $MainWxsPath -Value $content -NoNewline
Write-Host "Patched main.wxs for Windows login auto-start."
