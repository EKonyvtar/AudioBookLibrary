[CmdletBinding()]
param (
	[string]$PlaylistFile = './biblia_full.m3u',
	[string]$mekUrl='http://mek.oszk.hu/08800/08820/mp3',
	[string]$File = './release/offline_catalog-bible_hu.json',
	[string]$Separator = ',',
	$replaces=@{
		"otestamentum"="Ótestamentum"
		""="Teremtés könyve"
		""="Kivonulás könyve"
		""="Leviták könyve"
		""="Számok könyve"
		""="Második Törvénykönyv"
		""="Józsue könyve"
		""="Bírák könyve"
		""="Rut könyve"
		""="Sámuel 1. könyve"
		""="Sámuel 2. könyve"
		""="Királyok 1. könyve"
		""="Királyok 2. könyve"
		""="Krónikák 1. könyve"
		""="Krónikák 2. könyve"
		""="Ezdrás könyve"
		""="Nehemiás könyve"
		""="Tóbiás könyve"
		""="Judit könyve"
		""="Eszter könyve"
		""="Makkabeusok 1. könyve"
		""="Makkabeusok 2. könyve"
		""="Jób könyve"
		""="Zsoltárok könyve"
		""="Példabeszédek könyve"
		""="Prédikátor könyve"
		""="Énekek éneke"
		""="Bölcsesség könyve"
		""="Sirák fia könyve"
		""="Izajás könyve"
		""="Jeremiás könyve"
		""="Siralmak könyve"
		""="Báruk könyve"
		""="Ezekiel könyve"
		""="Dániel könyve"
		""="Ozeás könyve"
		""="Joel könyve"
		""="Ámosz könyve"
		""="Abdiás könyve"
		""="Jónás könyve"
		""="Mikeás könyve"
		""="Náhum könyve"
		""="Habakuk könyve"
		""="Szofoniás könyve"
		""="Aggeus könyve"
		""="Zakariás könyve"
		""="Malakiás könyve"
		"ujtestamentum"="Újszövetség"
		""="Máté evangéliuma"
		""="Márk evangéliuma"
		""="Lukács evangéliuma"
		""="János evangéliuma"
		""="Apostolok Cselekedetei"
		""="Rómaiaknak írt levél"
		""="Korintusiaknak írt 1. levél"
		""="Korintusiaknak írt 2. levél"
		""="Galatáknak írt levél"
		""="Efezusiaknak írt levél"
		""="Filippieknek írt levél"
		""="Kolosszeieknek írt levél"
		""="Tesszalonikaiaknak írt 1. levél"
		""="Tesszalonikaiaknak írt 2. levél"
		""="Timóteusnak írt 1. levél"
		""="Timóteusnak írt 2. levél"
		""="Titusznak írt levél"
		""="Filemonnak írt levél"
		""="Zsidóknak írt levél"
		""="Szent Jakab levele"
		""="Szent Péter 1. levele"
		""="Szent Péter 2. levele"
		""="Szent János 1. levele"
		""="Szent János 2. levele"
		""="Szent János 3. levele"
		""="Szent Júdás levele"
		""="Jelenések könyve"

	}
)

Set-Location $PSScriptRoot

#$catalog = @()
$audioBooks = Get-Content -Path $PlaylistFile

$count = 0
$errorList = ""
$skipList = ""
$newM3U = ""

foreach ($book in $audioBooks) {
	$count++
	if ($book -match "^#") {
		$msg = "$count - Skipping $($book)."
		Write-Host $msg -ForegroundColor Yellow
		$skipList += "$msg `n`n"
		$newM3U += $book + [System.Environment]::NewLine
		continue
	}
	
	$id = $book
	Write-Host "$count - Processing $($id).." -ForegroundColor Magenta
	$trackUrl = "$mekUrl/$($id)" -replace '\\',"/"
	$newM3U += $trackUrl + [System.Environment]::NewLine
}
	# $ebookObject = New-Object psobject -Property @{
	# 	title=''
	# 	image='http://mek.oszk.hu/08800/08820/borito.jpg'
	# 	album=''
	# 	artist=''
	# 	genre='Biblia'
	# 	source=''
	# 	trackNumber=0
	# 	totalTrackCount=0
	# 	duration=0
	# 	site=''
	# }

	

	

	# Write-Verbose "Adding fullTitle"
	# $ebookObject.album = $id.split('/')

    # Populate tracks
	#$trackNumber = 0
# 	foreach ($t in $trackDetails.tracks) {
# 		$trackNumber++
# 		$trackObject = $ebookObject.psobject.Copy()
# 		$trackObject.title = $t.title
# 		$trackObject.source = $t.fileUrl
# 		$trackObject.trackNumber = $trackNumber
# 		$trackObject.totalTrackCount = $trackDetails.tracks | Measure-Object | Select-Object -Expand Count

#         if ($t | Get-Member lengthTotalSeconds) {
#             $trackObject.duration = $t.lengthTotalSeconds
#         }

# 		# Fix individual issues
# 		foreach ($k in $fixes.Keys) {
# 			$trackObject.source = $trackObject.source -replace $k,$fixes.$k
# 		}

# 		$catalog += $trackObject
# 		$trackObject
# 	}
# 	if ($trackNumber -eq 0) {
# 		$msg = "No tracks found for $($t.title) on $trackUrl"
# 		Write-Error $msg
# 		$errorList += "$msg`n`n"
# 	}
# }
Write-Host "----- Errors reported ------"
$errorList

Write-Host "----- Items skipped ------"
$skipList

Write-Host "----- Writing catalogue ----- "
Set-Content -Path $OutputPlaylistFile -Value $newM3U -Force

# $sortedProperty = ($catalog[0] | Get-Member -Type NoteProperty | Select-Object -Expand Name) | Where-object {$_ -notmatch "site|total"}

# $sorted = New-object psobject -Property @{music=$catalog}
# $sorted.music = $sorted.music | Sort-Object image,trackNumber,source | Select-Object -Property $sortedProperty 
# Set-Content -Path $File -Value ($sorted | ConvertTo-Json) -Force -Encoding UTF8
