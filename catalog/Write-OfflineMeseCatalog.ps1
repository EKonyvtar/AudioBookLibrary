[CmdletBinding()]
param (
	[string]$MekFile = './mek_mese_list.txt',
	[string]$CsvFile = './archive_mese_list.csv',
	[string]$File = './release/offline_catalog-Mese.json'
)

Set-Location $PSScriptRoot
./Write-OfflineMekCatalog.ps1 -CatalogFile $MekFile -File $File

$catalogContent = Get-Content  -Path $File -Raw
$catalog = ConvertFrom-Json -InputObject $catalogContent
$catalog = $catalog.music

Write-Host "----- Merging catalogue ----- "
$csvContent = Get-Content  -Path $CsvFile -Raw
$csv = ConvertFrom-Csv -InputObject $csvContent -Delimiter ','
$catalog += $csv

Write-Host "----- Rewriting catalogue ----- "
$sortedProperty = ($catalog[0] | Get-Member -Type NoteProperty | Select-Object -Expand Name) | Where-object { $_ -notmatch "site|total" }
$sorted = New-object psobject -Property @{music = $catalog }
$sorted.music = $sorted.music | Sort-Object image, trackNumber, source | Select-Object -Property $sortedProperty 
Set-Content -Path $File -Value ($sorted | ConvertTo-Json) -Force -Encoding UTF8