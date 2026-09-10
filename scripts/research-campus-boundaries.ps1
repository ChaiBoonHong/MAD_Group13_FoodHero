param(
    [string]$OutputPath = "supabase/campus-boundary-research.json"
)

$ErrorActionPreference = "Stop"
$userAgent = "FoodHero-campus-boundary-research/1.0 (university project)"

$campuses = @(
    @{ code="UTAR"; query="Universiti Tunku Abdul Rahman Kampar Campus"; official="https://study.utar.edu.my/campus-life.php" },
    @{ code="TARUMT"; query="Tunku Abdul Rahman University of Management and Technology Kuala Lumpur"; official="https://www.tarc.edu.my/contact/" },
    @{ code="SUNWAY"; query="Sunway University Malaysia"; official="https://sunwayuniversity.edu.my/contact-us" },
    @{ code="TAYLORS"; query="Taylor's University Lakeside Campus"; official="https://university.taylors.edu.my/en/about-taylors/contact-us.html" },
    @{ code="MMU"; query="Multimedia University"; official="https://www.mmu.edu.my/contact-us/" },
    @{ code="APU"; query="Asia Pacific University of Technology and Innovation Kuala Lumpur"; official="https://www.apu.edu.my/contact-us" },
    @{ code="UNIKL"; query="Universiti Kuala Lumpur City Campus"; official="https://www.unikl.edu.my/contact-us/" },
    @{ code="UCSI"; query="UCSI University Kuala Lumpur Campus"; official="https://www.ucsiuniversity.edu.my/contact-us" },
    @{ code="UNITEN"; query="Universiti Tenaga Nasional Putrajaya Campus"; official="https://www.uniten.edu.my/contact-us/" },
    @{ code="UTP"; query="Universiti Teknologi PETRONAS Seri Iskandar"; official="https://www.utp.edu.my/Pages/Contact-Us.aspx" },
    @{ code="INTI"; query="INTI International University Nilai"; official="https://newinti.edu.my/contact-us/" },
    @{ code="CURTIN"; query="Curtin University Malaysia Miri"; official="https://curtin.edu.my/contact/" },
    @{ code="SWINBURNE"; query="Swinburne University of Technology Sarawak Campus"; official="https://www.swinburne.edu.my/contact/" },
    @{ code="NOTTINGHAM"; query="University of Nottingham Malaysia Semenyih"; official="https://www.nottingham.edu.my/AboutUs/ContactUs/Contact-us.aspx" },
    @{ code="MONASH"; query="Monash University Malaysia"; official="https://www.monash.edu.my/about/contact-us" },
    @{ code="UM"; query="Universiti Malaya Kuala Lumpur"; official="https://www.um.edu.my/contact-us" },
    @{ code="UKM"; query="Universiti Kebangsaan Malaysia Bangi"; official="https://www.ukm.my/portalukm/contact/" },
    @{ code="USM"; query="Universiti Sains Malaysia Penang Main Campus"; official="https://www.usm.my/index.php/contact-us" },
    @{ code="UTM"; query="Universiti Teknologi Malaysia Johor Bahru"; official="https://www.utm.my/contact/" },
    @{ code="UPM"; query="Universiti Putra Malaysia Serdang"; official="https://www.upm.edu.my/contact" },
    @{ code="UITM"; query="Universiti Teknologi MARA Shah Alam"; official="https://www.uitm.edu.my/index.php/en/contact-us" },
    @{ code="UUM"; query="Universiti Utara Malaysia Sintok"; official="https://www.uum.edu.my/contact-us" },
    @{ code="IIUM"; query="International Islamic University Malaysia Gombak"; official="https://www.iium.edu.my/contact" },
    @{ code="UNIMAS"; query="Universiti Malaysia Sarawak Kota Samarahan"; official="https://www.unimas.my/contact-us" },
    @{ code="UMS"; query="Universiti Malaysia Sabah Kota Kinabalu"; official="https://www.ums.edu.my/v5/contact-us" },
    @{ code="UMPSA"; query="Universiti Malaysia Pahang Al-Sultan Abdullah Pekan"; official="https://www.umpsa.edu.my/en/contact-us" },
    @{ code="UTHM"; query="Universiti Tun Hussein Onn Malaysia Parit Raja"; official="https://www.uthm.edu.my/en/contact-us" },
    @{ code="UTEM"; query="Universiti Teknikal Malaysia Melaka Durian Tunggal"; official="https://www.utem.edu.my/en/contact-us.html" },
    @{ code="UNIMAP"; query="Universiti Malaysia Perlis Pauh Putra Campus"; official="https://www.unimap.edu.my/index.php/en/contact-us" },
    @{ code="USIM"; query="Universiti Sains Islam Malaysia Nilai"; official="https://www.usim.edu.my/contact-us/" },
    @{ code="UNISZA"; query="Universiti Sultan Zainal Abidin Gong Badak Campus"; official="https://www.unisza.edu.my/contact-us/" },
    @{ code="UMK"; query="Universiti Malaysia Kelantan Bachok Campus"; official="https://www.umk.edu.my/en/contact-us.html" },
    @{ code="UPSI"; query="Universiti Pendidikan Sultan Idris Sultan Abdul Jalil Shah Campus"; official="https://www.upsi.edu.my/contact-us/" },
    @{ code="UMT"; query="Universiti Malaysia Terengganu Kuala Nerus"; official="https://www.umt.edu.my/contact-us/" },
    @{ code="UPNM"; query="Universiti Pertahanan Nasional Malaysia Sungai Besi"; official="https://www.upnm.edu.my/index.php/en/contact-us" }
)

$results = foreach ($campus in $campuses) {
    $encoded = [uri]::EscapeDataString($campus.query)
    $url = "https://nominatim.openstreetmap.org/search?format=jsonv2&countrycodes=my&limit=5&polygon_geojson=1&polygon_threshold=0.00005&q=$encoded"
    $matches = @(Invoke-RestMethod -Uri $url -Headers @{ "User-Agent"=$userAgent; "Accept-Language"="en" }) | ForEach-Object { $_ }
    $match = $matches | Where-Object { $_.geojson.type -in @("Polygon", "MultiPolygon") } | Select-Object -First 1
    if (-not $match) { $match = $matches | Select-Object -First 1 }

    [ordered]@{
        institution_code = $campus.code
        search_query = $campus.query
        official_campus_source = $campus.official
        geometry_source = if ($match) { "https://www.openstreetmap.org/$($match.osm_type)/$($match.osm_id)" } else { $null }
        geometry_license = "OpenStreetMap contributors, ODbL 1.0"
        display_name = if ($match) { $match.display_name } else { $null }
        osm_type = if ($match) { $match.osm_type } else { $null }
        osm_id = if ($match) { $match.osm_id } else { $null }
        latitude = if ($match) { [double]$match.lat } else { $null }
        longitude = if ($match) { [double]$match.lon } else { $null }
        geometry_type = if ($match) { $match.geojson.type } else { $null }
        boundary_geojson = if ($match -and $match.geojson.type -in @("Polygon", "MultiPolygon")) { $match.geojson } else { $null }
        review_status = if ($match -and $match.geojson.type -in @("Polygon", "MultiPolygon")) { "candidate_requires_visual_review" } else { "unresolved_no_polygon" }
    }
    Start-Sleep -Milliseconds 1100
}

$document = [ordered]@{
    generated_at = (Get-Date).ToUniversalTime().ToString("o")
    method = "Official campus pages identify the intended campus; Nominatim/OpenStreetMap supplies candidate mapped polygons. Every candidate must be visually reviewed before schema import."
    attribution = "Copyright OpenStreetMap contributors; data available under ODbL 1.0: https://www.openstreetmap.org/copyright"
    campuses = $results
}

$parent = Split-Path -Parent $OutputPath
if ($parent -and -not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent | Out-Null }
$document | ConvertTo-Json -Depth 100 | Set-Content -Path $OutputPath -Encoding utf8

$resolved = @($results | Where-Object review_status -eq "candidate_requires_visual_review").Count
$unresolved = $results.Count - $resolved
Write-Output "Wrote $($results.Count) campus research records to $OutputPath ($resolved polygon candidates, $unresolved unresolved)."
