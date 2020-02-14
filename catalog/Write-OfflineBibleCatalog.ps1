[CmdletBinding()]
param (
	[string]$PlaylistFile = './biblia_full.m3u',
	[string]$mekUrl='http://mek.oszk.hu/08800/08820/mp3',
	[string]$File = './release/offline_catalog-bible_hu.json',
	[string]$Separator = ',',
	$replaces=@{
		"otestamentum"="Ótestamentum"
		"ujtestamentum"="Újszövetség"
		"01_teremtes"="01. Teremtés könyve"
		"02_kivonulas"="02. Kivonulás könyve"
		"03_levitak"="03. Leviták könyve"
		"04_szamok"="04. Számok könyve"
		"05_mtorv"="05. Második Törvénykönyv"
		"06_jozsue"="06. Józsue könyve"
		"07_birak"="07. Bírák könyve"
		"08_rut"="08. Rut könyve"
		"09_samuel1"="09. Sámuel 1. könyve"
		"10_samuel2"="10. Sámuel 2. könyve"
		"11_kir1"="11. Királyok 1. könyve"
		"12_kir2"="12. Királyok 2. könyve"
		"13_kron1"="13. Krónikák 1. könyve"
		"14_kron2"="14. Krónikák 2. könyve"
		"15_ezdras"="15. Ezdrás könyve"
		"16_nehem"="16. Nehemiás könyve"
		"17_tobias"="17. Tóbiás könyve"
		"18_judit"="18. Judit könyve"
		"19_eszter"="19. Eszter könyve"
		"20_makkab1"="20. Makkabeusok 1. könyve"
		"21_makkab2"="21. Makkabeusok 2. könyve"
		"22_job"="22. Jób könyve"
		"23_zsoltarok"="23. Zsoltárok könyve"
		"24_peldabesz"="24. Példabeszédek könyve"
		"25_predikator"="25. Prédikátor könyve"
		"26_enekek"="26. Énekek éneke"
		"27_bolcsesseg"="27. Bölcsesség könyve"
		"28_sirak"="28. Sirák fia könyve"
		"29_izaias"="29. Izajás könyve"
		"30_jeremias"="30. Jeremiás könyve"
		"31_siralmak"="31. Siralmak könyve"
		"32_baruk"="32. Báruk könyve"
		"33_ezekiel"="33. Ezekiel könyve"
		"34_daniel"="34. Dániel könyve"
		"35_ozeas"="35. Ozeás könyve"
		"36_joel"="36. Joel könyve"
		"37_amosz"="37. Ámosz könyve"
		"38_abdias"="38. Abdiás könyve"
		"39_jonas"="39. Jónás könyve"
		"40_mikeas"="40. Mikeás könyve"
		"41_nahum"="41. Náhum könyve"
		"42_habakuk"="42. Habakuk könyve"
		"43_szofonias"="43. Szofoniás könyve"
		"44_aggeus"="44. Aggeus könyve"
		"45_zakarias"="45. Zakariás könyve"
		"46_malakias"="46. Malakiás könyve"
		"47_mate"="47. Máté evangéliuma"
		"48_mark"="48. Márk evangéliuma"
		"49_lukacs"="49. Lukács evangéliuma"
		"50_janos"="50. János evangéliuma"
		"51_apcsel"="51. Apostolok Cselekedetei"
		"52_romai"="52. Rómaiaknak írt levél"
		"53_korint1"="53. Korintusiaknak írt 1. levél"
		"54_korint2"="54. Korintusiaknak írt 2. levél"
		"55_galata"="55. Galatáknak írt levél"
		"56_efezusi"="56. Efezusiaknak írt levél"
		"57_filippi"="57. Filippieknek írt levél"
		"58_kolosszei"="58. Kolosszeieknek írt levél"
		"59_tessz1"="59. Tesszalonikaiaknak írt 1. levél"
		"60_tessz2"="60. Tesszalonikaiaknak írt 2. levél"
		"61_timot1"="61. Timóteusnak írt 1. levél"
		"62_timot2"="62. Timóteusnak írt 2. levél"
		"63_titusz"="63. Titusznak írt levél"
		"64_filemon"="64. Filemonnak írt levél"
		"65_zsidok"="65. Zsidóknak írt levél"
		"66_jakab"="66. Szent Jakab levele"
		"67_peter1"="67. Szent Péter 1. levele"
		"68_peter2"="68. Szent Péter 2. levele"
		"69_janos1"="69. Szent János 1. levele"
		"70_janos2"="70. Szent János 2. levele"
		"71_janos3"="71. Szent János 3. levele"
		"72_judas"="72. Szent Júdás levele"
		"73_jelenesek"="73. Jelenések könyve"
	}
)

Set-Location $PSScriptRoot

$catalog = @()
$audioBooks = Get-Content -Path $PlaylistFile

$count = 0
$tCount = 1
$errorList = ""
$skipList = ""
$newM3U = ""

$prev = ''

foreach ($book in $audioBooks) {
	$count++
	if ($book -match "^#") {
		$msg = "$count - Skipping $($book)."
		Write-Host $msg -ForegroundColor Yellow
		$skipList += "$msg `n`n"
		$newM3U += $book + [System.Environment]::NewLine
		continue
	}
	
	$trackUrl = $book.ToString()
	Write-Host "$count - Processing $($id).." -ForegroundColor Magenta
	#$trackUrl = "$mekUrl/$($id)" -replace '\\',"/"
	#$newM3U += $trackUrl + [System.Environment]::NewLine

	$full_title = $trackUrl -ireplace "$mekUrl/",''
	foreach ($k in $replaces.Keys) {
		$full_title = $full_title -ireplace $k,$replaces.$k
	}

	$parts = $full_title -split "/"

	# New chapter
	if ($prev -ne $parts[1]) {
		$tCount = 1

		$prev = $parts[1]
	} else {
		$tCount++
	}

	$title = $parts[1] -replace "^\d+\. ",''

	$ebookObject = New-Object psobject -Property @{
		title="$title - $tCount. fejezet"
		image='http://mek.oszk.hu/08800/08820/borito.jpg'
		album=$parts[1]
		artist=$parts[0]
		genre='Biblia'
		source=$trackUrl
		trackNumber=$tCount
		totalTrackCount=0
		duration=0
		site=$full_title
   }

   
   $catalog += $ebookObject
}

Write-Host "----- Errors reported ------"
$errorList

Write-Host "----- Items skipped ------"
$skipList

Write-Host "----- Writing catalogue ----- "

$sortedProperty = ($catalog[0] | Get-Member -Type NoteProperty | Select-Object -Expand Name) | Where-object {$_ -notmatch "site|total"}

$sorted = New-object psobject -Property @{music=$catalog}
$sorted.music = $sorted.music | Sort-Object source,trackNumber | Select-Object -Property $sortedProperty 
Set-Content -Path $File -Value ($sorted | ConvertTo-Json) -Force -Encoding UTF8
