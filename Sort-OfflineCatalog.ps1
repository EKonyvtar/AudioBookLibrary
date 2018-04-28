[CmdletBinding()]
param (
	[string]$File = './mobile/src/main/res/raw/offline_catalog.json'
)

cd $PSScriptRoot

$catalog = Get-Content -Path $File | ConvertFrom-Json
$sorted = $catalog.psobject.Copy()
$sortedProperty = ($sorted.music[0] | Get-Member -Type NoteProperty | Select-Object -Expand Name)

$sorted.music = $sorted.music | Sort-Object image,trackNumber,source | Select-Object -Property $sortedProperty
Set-Content -Path $File -Value ($sorted | ConvertTo-Json) -Force -Encoding UTF8
