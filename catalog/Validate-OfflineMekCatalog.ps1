[CmdletBinding()]
param (
	[string]$File = './release/offline_catalog-Hungarian.json'
)

$titles = @{}
Set-Location $PSScriptRoot
$audioBooks = Get-Content -Path $File | ConvertFrom-Json 

foreach ($book in $audioBooks.music) {
	$title = $book.album
	$id = $book.image -ireplace "/borito.jpg",""

	if (!$titles.ContainsKey($title)) {
		$titles.$title = New-Object -TypeName System.Collections.ArrayList
	}

	$titles.$title += $id
	#$titles.$title = $titles.$title | Select-Object -Unique
}

Write-Warning "Duplicate Warnings:"
foreach($title in $titles.Keys) {
	Write-Host "> $title" -ForegroundColor Magenta
	Write-Warning "  $($titles.$title | Select-Object -Unique)"
}
