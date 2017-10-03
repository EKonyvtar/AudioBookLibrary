[CmdletBinding()]
param (
	$url = 'http://oszkapi-dev.azurewebsites.net/api/audiobooks',
	$file = './source.json'
)

$catalog = @()
$audioBooks = Invoke-RestMethod $url |
Select -First 2

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
		duration=200
		site=''
	}

	Write-Verbose "Enriching $book"
	if ($book.fullTitle -match ':') {
		Write-Verbose "Splitting fullTitle"
		$ebookObject.album = $book.fullTitle.Split(':')[1].Trim()
		$ebookObject.artist = $book.fullTitle.Split(':')[0].Trim()
	}
	$trackUrl = "http://oszkapi-dev.azurewebsites.net/api/audiobooks/$($book.id)"
	$ebookObject.site = $trackUrl
	$ebookObject

	$trackDeatils = Invoke-RestMethod $trackUrl
	$trackNumber = 0
	foreach ($t in $trackDeatils.tracks) {
		$trackNumber++
		$trackObject = $ebookObject.psobject.Copy()
		$trackObject.title = $t.title
		$trackObject.source = $t.fileUrl
		$trackObject.trackNumber = $trackNumber
		$trackObject.totalTrackCount = $trackDeatils.tracks | Measure-Object | Select -Expand Count
		$catalog += $trackObject
		$trackObject
	}
	#Read-Host "More?"
	break;
}

$offline_catalog = New-object psobject -Property @{music=$catalog}
#$offline_catalog

$offline_catalog_json = $offline_catalog | ConvertTo-Json -Depth 3
$offline_catalog_json

#Set-Content -Path $file -Value $offline_catalog_json -Force

