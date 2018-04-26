[CmdletBinding()]
param (
	[string]$File = './mobile/src/main/res/raw/offline_catalog.json'
)

cd $PSScriptRoot

$catalog = Get-Content -Path $File | ConvertFrom-Json
$sorted = $catalog.psobject.Copy()
$sorted.music = $sorted.music | Sort-Object image,trackNumber,source
Set-Content -Path $File -Value ($sorted | ConvertTo-Json)
