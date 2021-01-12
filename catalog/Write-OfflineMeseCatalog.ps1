[CmdletBinding()]
param (
	[string]$CatalogFile = './mek_mese_list.txt',
	[string]$File = './release/offline_catalog-Mese.json'
)

Set-Location $PSScriptRoot
./Write-OfflineMekCatalog.ps1 -CatalogFile $CatalogFile -File $File