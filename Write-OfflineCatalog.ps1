[CmdletBinding()]
param (
	[string]$CatalogUrl = 'http://oszkapi-dev.azurewebsites.net/api/audiobooks',
	[string]$File = './mobile/src/main/res/raw/offline_catalog.json'
)

cd $PSScriptRoot

$catalog = @()
$audioBooks = Invoke-RestMethod $CatalogUrl |
Select-Object -First 2

foreach ($book in $audioBooks) {
	$ebookObject = New-Object psobject -Property @{
		title=''
		image=$book.thumbnailUrl
		album=$book.fullTitle
		artist=''
		genre='Novella'
		source=''
		trackNumber=0
		totalTrackCount=0
		duration=0
		site=''
	}

	Write-Verbose "Enriching $book"
	if ($book.fullTitle -match ':') {
		Write-Verbose "Splitting fullTitle"
		$ebookObject.album = $book.fullTitle.Split(':')[1].Trim()
		$ebookObject.artist = $book.fullTitle.Split(':')[0].Trim()
	}
	$trackUrl = "$CatalogUrl/$($book.id)"
	$ebookObject.site = $trackUrl
	Write-Host "Processing $($book.fullTitle).." -ForegroundColor Magenta
    Write-Host "`t $trackUrl" -ForegroundColor Magenta

    # Get Audiobook details
    $trackDeatils = $null
	$trackDeatils = Invoke-RestMethod $trackUrl

    # Override Author
    if ($trackDeatils | Get-Member author) { $ebookObject.artist = $trackDeatils.author }

    # Override Genre
    if ($trackDeatils | Get-Member type) {
        $ebookObject.genre = [string]::Join(',',($trackDeatils.type | where { $_ -notmatch 'hang'}))
    }

    # Populate tracks
	$trackNumber = 0
	foreach ($t in $trackDeatils.tracks) {
		$trackNumber++
		$trackObject = $ebookObject.psobject.Copy()
		$trackObject.title = $t.title
		$trackObject.source = $t.fileUrl
		$trackObject.trackNumber = $trackNumber
		$trackObject.totalTrackCount = $trackDeatils.tracks | Measure-Object | Select-Object -Expand Count

        if ($t | Get-Member lengthTotalSeconds) {
            $trackObject.duration = $t.lengthTotalSeconds
        }

		$catalog += $trackObject
		$trackObject
	}
	if ($trackNumber -eq 0) {
		Write-Error "No tracks found for $($t.title) on $trackUrl"
	}
}

$offline_catalog_json = New-object psobject -Property @{music=$catalog} | ConvertTo-Json -Depth 2
$offline_catalog_json

Set-Content -Path $File -Value $offline_catalog_json -Force -Encoding UTF8

