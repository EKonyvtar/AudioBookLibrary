[CmdletBinding()]
param (
	[string]$CatalogFile = './book.csv',
	[string]$FilePattern = './release/offline_catalog.json',
	[string]$Separator = ',',
	[string[]]$Locales = @("German") #,"French") #,"English","Spanish")
)

function Reorder-Text($text) {
	#$text = $text -ireplace "&iacute;","í"
	$text = [System.Web.HttpUtility]::HtmlDecode($text)
	try {
		if ($text.Contains($Separator)) {
			return ($text.Split($Separator)[-1]).Trim() + " " + ($text.Split($Separator)[0]).Trim()
		}
	} catch {

	}
	return $text
}

function Reorder-Name($text) {
	$text = [System.Web.HttpUtility]::HtmlDecode($text)
	$name = Reorder-Text $text
	if ($text.Contains($Separator)) {
		return ($name + "$Separator " + $text.Split($Separator)[0]).Trim()
	}
	return $name
}


function Reorder-Title($text) {
	if ($text -imatch ",\s*\w+") {
		$order = $text.Split(",")
		if ($order.Count -lt 3) {
			if ($text -imatch "livre|bible|tome") {
				return ($text -replace ",","")
			}
			return ($order[1]).Trim() + " " +($order[0]).Trim()
		} else {
			return ($order[1]).Trim() + " " +($order[0]).Trim() + $order[2]
		}
	}
	return $text
}

function Test-RemoteFile($remotefile) {
	$file = Get-RemoteFile $remotefile
	return ($file -ne $null)
}

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

function Get-Tracks($zipfile) {
	# http://www.archive.org/download/fables_lafontaine_01_librivox/fables_lafontaine_01_librivox_64kb_mp3.zip
	$m3u=$zipfile -ireplace "_mp3.zip",".m3u"
	
	$cacheFile = $m3u
	$cacheFile = $cacheFile -ireplace "http://",""
	$cacheFile = $cacheFile -ireplace "/","|"
	$cacheFile = $cacheFile -ireplace "\?","#"
	$cacheFile = "./cache/$cacheFile"

	try {
		if (-Not (Test-Path $cacheFile)) {
			$m3uResponse = Invoke-WebRequest -Uri $m3u -UseBasicParsing -OutFile $cacheFile -ea Stop #| Select -ExpandProperty Content
		}

		$m3uContent = Get-Content $cacheFile -Raw
		return $m3uContent.Split("`n")

	} catch { 
		Write-Error $_
	}
	return @()
}

function Get-Cover($zipfile) {
	if (-Not $zipfile) { return "" }
	#https://ia800702.us.archive.org/19/items/fables_lafontaine_01_librivox/fables_lafontaine_01_librivox_files.xml
	$remotefile=$zipfile -ireplace "_64kb_mp3.zip","_files.xml"
	$xml = $null
	try {
		$content = Get-RemoteFile $remotefile
		$xml = [xml]$content
		$parentUrl = Split-Path -Path $remotefile -Parent
		$images = $xml.files.file | Where-Object name -imatch "\.jpg"

		foreach ($image in $images) {
			$imageUrl = "$parentUrl/"+$image.name
			try {
				if (Test-RemoteFile $imageUrl) {
					return $imageUrl
				} else {
					Write-Error "$imageUrl missing"
				}
			} catch {
				#Does not exist
				Write-Error $_
			}
		}
	} catch { }
	return ""
}

Set-Location $PSScriptRoot

$catalog = @()
$allBook = Import-Csv -Path $CatalogFile -Delimiter $Separator

foreach ($locale in $Locales) {
	Write-Host "###### PROCESSING $locale #######"
	$File = $FilePattern -ireplace "catalog","catalog-$locale"
	$part = "Part"
	switch ($locale) {
		"French" { $part = "Partie" }
		"Spanish" { $part = "Parte" }
		"German" { $part = "Teile" }
		Default {}
	}
	$count = 0
	$audioBooks = $allBook | Where-Object language -ieq $locale
	foreach ($book in $audioBooks) {
		$count++
		if ($book -match "^#" -or [String]::IsNullOrEmpty($book.zipfile)) {
			Write-Host "$count - Skipping book $($book)." -ForegroundColor Yellow
			continue
		}
		
		$id = $book._id
		try {
			$id = $book.etext.Split("/")[-1]
		} catch {
			Write-Error "Error fetching ETEXT ID"
		}

		$genre = "Fiction"
		if (![String]::IsNullOrEmpty($book.category)) { $genre = $book.category}

		Write-Host "$count - Processing $($id).." -ForegroundColor Magenta
		$ebookObject = New-Object psobject -Property @{
			title=''
			image=(Get-Cover $book.zipfile)
			album=(Reorder-Title $book.title)
			artist=(Reorder-Name $book.author)
			genre=$genre
			source=''
			trackNumber=0
			totalTrackCount=0
			duration=0
			site=$book.etext
		}

		$trackUrls = (Get-Tracks $book.zipfile)
		# Populate tracks
		$trackNumber = 0
		$totalCount = $trackUrls| Measure-Object | Select-Object -Expand Count

		foreach ($t in $trackUrls) {
			if ([String]::IsNullOrEmpty($t)) { continue }
			Write-Host "`t $t" -ForegroundColor Magenta

			$trackNumber++
			$trackObject = $ebookObject.psobject.Copy()
			$trackObject.title = "$part $trackNumber"
			$trackObject.source = $t
			$trackObject.trackNumber = $trackNumber
			$trackObject.totalTrackCount = $totalCount-1

			$catalog += $trackObject
			$trackObject
		}
		if ($trackNumber -eq 0) {
			Write-Error "No tracks found for $($ebookObject.album) on $($ebookObject.zipfile)"
		}
	}
	$sortedProperty = ($catalog[0] | Get-Member -Type NoteProperty | Select-Object -Expand Name)

	$sorted = New-object psobject -Property @{music=$catalog}
	$sorted.music = $sorted.music | Sort-Object album,site,trackNumber | Select-Object -Property $sortedProperty
	Set-Content -Path $File -Value ($sorted | ConvertTo-Json) -Force -Encoding UTF8
}