[CmdletBinding()]
param (
	[string]$CatalogUrl = 'http://oszkapi-dev.azurewebsites.net/api/audiobooks',
	[string]$CatalogFile = './mek_ebook_list.txt',
	[string]$File = './release/offline_catalog-Hungarian.json',
	[string]$Separator = ',',
	$fixes=@{
		"0900af032"="0900af022"
	}
)


function Get-RemoteFile($remotefile) {
	$cacheFile = $remotefile
	$cacheFile = $cacheFile -ireplace "http://",""
	$cacheFile = $cacheFile -ireplace "/","|"
	$cacheFile = $cacheFile -ireplace "\?","#"
	$missingFile = "./cache/missing/$cacheFile"
	$cacheFile = "./cache/$cacheFile"

	try {
		if (-Not (Test-Path $cacheFile)) {
			$response = Invoke-WebRequest -Uri $remotefile -UseBasicParsing -OutFile $cacheFile #| Select -ExpandProperty Content
		}
		$content = Get-Content $cacheFile -Raw
		Remove-item -Path $missingFile -EA 0 -Force
		return $content
	} catch {
		Set-Content -Path $missingFile -Value $remotefile -Force -EA 0
	}

	return $null;
}

function Invoke-CachedRestMethod($remotefile) {
	$content = Get-RemoteFile($remotefile)
	if (!$content) { throw "Empty response from $remotefile .." }
	return ConvertFrom-Json $content
}

Set-Location $PSScriptRoot

$catalog = @()
$audioBooks = Get-Content -Path $CatalogFile

$count = 0
$errorList = ""
$skipList = ""

foreach ($book in $audioBooks) {
	$count++
	if ($book -match "^#") {
		$msg = "$count - Skipping $($book)."
		Write-Host $msg -ForegroundColor Yellow
		$skipList += "$msg `n`n"
		continue
	}
	
	$id = $book.Split("/")[-1]
	Write-Host "$count - Processing $($id).." -ForegroundColor Magenta

	$ebookObject = New-Object psobject -Property @{
		title=''
		image="$book/borito.jpg"
		album=''
		artist=''
		genre='Novella'
		source=''
		trackNumber=0
		totalTrackCount=0
		duration=0
		site=''
	}

	$trackUrl = "$CatalogUrl/$($id)"
	$ebookObject.site = $trackUrl
    Write-Host "`t $trackUrl" -ForegroundColor Magenta

    # Get Audiobook details
	$trackDetails = $null
	try {
		$trackDetails = Invoke-CachedRestMethod $trackUrl
	} catch {
		$trackDetails = $null
	}

	Write-Verbose "Adding fullTitle"
	if ($trackDetails | Get-Member author) { $ebookObject.artist = $trackDetails.author }
	if ($trackDetails | Get-Member title) { $ebookObject.album = $trackDetails.title }

	if (!$trackDetails -or !$ebookObject.album) {
		$msg = "$count - Skipping $($book) with empty response"
		Write-Host $msg -ForegroundColor Yellow
		$skipList += "$msg `n`n"
		continue
	}

	try {
		if ($trackDetails.creators[0].isFamilyFirst -eq $false) {
			$ebookObject.artist = $ebookObject.artist + $Separator + $trackDetails.creators[0].familyName + " " +  $trackDetails.creators[0].givenName
		}
		Write-Warning "Expanded name to $($ebookObject.artist)"
	} catch {
		$_  | Out-Null
	}

    # Override Genre
    if ($trackDetails | Get-Member type) {
        $ebookObject.genre = [string]::Join($Separator,($trackDetails.type | Where-Object { $_ -notmatch 'hang'}))
    }

    # Populate tracks
	$trackNumber = 0
	foreach ($t in $trackDetails.tracks) {
		$trackNumber++
		$trackObject = $ebookObject.psobject.Copy()
		$trackObject.title = $t.title
		$trackObject.source = $t.fileUrl
		$trackObject.trackNumber = $trackNumber
		$trackObject.totalTrackCount = $trackDetails.tracks | Measure-Object | Select-Object -Expand Count

        if ($t | Get-Member lengthTotalSeconds) {
            $trackObject.duration = $t.lengthTotalSeconds
        }

		# Fix individual issues
		foreach ($k in $fixes.Keys) {
			$trackObject.source = $trackObject.source -replace $k,$fixes.$k
		}

		$catalog += $trackObject
		$trackObject
	}
	if ($trackNumber -eq 0) {
		$msg = "No tracks found for $($t.title) on $trackUrl"
		Write-Error $msg
		$errorList += "$msg`n`n"
	}
}
Write-Host "----- Errors reported ------"
$errorList

Write-Host "----- Items skipped ------"
$skipList

Write-Host "----- Writing catalogue ----- "
$sortedProperty = ($catalog[0] | Get-Member -Type NoteProperty | Select-Object -Expand Name) | Where-object {$_ -notmatch "site|total"}

$sorted = New-object psobject -Property @{music=$catalog}
$sorted.music = $sorted.music | Sort-Object image,trackNumber,source | Select-Object -Property $sortedProperty 
Set-Content -Path $File -Value ($sorted | ConvertTo-Json) -Force -Encoding UTF8
