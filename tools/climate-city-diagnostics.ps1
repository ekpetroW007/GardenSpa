param(
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"
$cities = @(
    [PSCustomObject]@{ City = "Калининград"; Latitude = 54.7104; Longitude = 20.4522 },
    [PSCustomObject]@{ City = "Москва"; Latitude = 55.7558; Longitude = 37.6173 },
    [PSCustomObject]@{ City = "Краснодар"; Latitude = 45.0355; Longitude = 38.9753 },
    [PSCustomObject]@{ City = "Сочи"; Latitude = 43.5855; Longitude = 39.7231 },
    [PSCustomObject]@{ City = "Екатеринбург"; Latitude = 56.8389; Longitude = 60.6057 },
    [PSCustomObject]@{ City = "Новосибирск"; Latitude = 55.0084; Longitude = 82.9357 },
    [PSCustomObject]@{ City = "Якутск"; Latitude = 62.0355; Longitude = 129.6755 },
    [PSCustomObject]@{ City = "Владивосток"; Latitude = 43.1155; Longitude = 131.8855 },
    [PSCustomObject]@{ City = "Мурманск"; Latitude = 68.9707; Longitude = 33.0749 }
)

function Get-PercentileValue {
    param(
        [double[]]$Values,
        [double]$Percentile
    )
    $sorted = @($Values | Sort-Object)
    $index = [int][Math]::Round(
        ($sorted.Count - 1) * $Percentile,
        [MidpointRounding]::AwayFromZero
    )
    return [double]$sorted[$index]
}

function Get-CanonicalDayOfYear {
    param([datetime]$Date)
    return ([datetime]::new(2000, $Date.Month, $Date.Day)).DayOfYear
}

function Get-DateFromCanonicalDay {
    param(
        [int]$DayOfYear,
        [int]$Year
    )
    $canonical = [datetime]::new(2000, 1, 1).AddDays($DayOfYear - 1)
    return [datetime]::new($Year, $canonical.Month, $canonical.Day)
}

function Get-ClimateForCity {
    param(
        [PSCustomObject]$City,
        [PSCustomObject]$Response
    )

    $years = @{}
    for ($index = 0; $index -lt $Response.daily.time.Count; $index++) {
        if ($null -eq $Response.daily.temperature_2m_min[$index] -or $null -eq $Response.daily.temperature_2m_max[$index]) {
            continue
        }
        $date = [datetime]::ParseExact(
            [string]$Response.daily.time[$index],
            "yyyy-MM-dd",
            [Globalization.CultureInfo]::InvariantCulture
        )
        if (-not $years.ContainsKey($date.Year)) {
            $years[$date.Year] = [Collections.Generic.List[object]]::new()
        }
        $precipitation = $Response.daily.precipitation_sum[$index]
        if ($null -eq $precipitation) {
            $precipitation = 0.0
        }
        $years[$date.Year].Add([PSCustomObject]@{
            Date = $date
            Minimum = [double]$Response.daily.temperature_2m_min[$index]
            Maximum = [double]$Response.daily.temperature_2m_max[$index]
            Precipitation = [Math]::Max(0.0, [double]$precipitation)
        })
    }

    $springFrostDays = [Collections.Generic.List[double]]::new()
    $autumnFrostDays = [Collections.Generic.List[double]]::new()
    $annualGdd5 = [Collections.Generic.List[double]]::new()
    $annualGdd10 = [Collections.Generic.List[double]]::new()
    $annualWarmPrecipitation = [Collections.Generic.List[double]]::new()
    $annualWinterMinimums = [Collections.Generic.List[double]]::new()

    foreach ($year in @($years.Keys | Sort-Object)) {
        $days = @($years[$year])
        if ($days.Count -lt 330) {
            continue
        }

        $springFrost = @($days | Where-Object { $_.Date.Month -le 6 -and $_.Minimum -le 0.0 } | Sort-Object Date | Select-Object -Last 1)
        $autumnFrost = @($days | Where-Object { $_.Date.Month -ge 7 -and $_.Minimum -le 0.0 } | Sort-Object Date | Select-Object -First 1)
        $springFrostDays.Add($(if ($springFrost.Count -eq 0) { 1 } else { Get-CanonicalDayOfYear $springFrost[0].Date }))
        $autumnFrostDays.Add($(if ($autumnFrost.Count -eq 0) { 366 } else { Get-CanonicalDayOfYear $autumnFrost[0].Date }))

        $gdd5 = 0.0
        $gdd10 = 0.0
        $warmPrecipitation = 0.0
        $winterMinimum = [double]::PositiveInfinity
        foreach ($day in $days) {
            $average = ($day.Minimum + $day.Maximum) / 2.0
            $gdd5 += [Math]::Max(0.0, $average - 5.0)
            $gdd10 += [Math]::Max(0.0, $average - 10.0)
            if ($day.Date.Month -ge 4 -and $day.Date.Month -le 10) {
                $warmPrecipitation += $day.Precipitation
            }
            if ($day.Date.Month -in @(1, 2, 12)) {
                $winterMinimum = [Math]::Min($winterMinimum, $day.Minimum)
            }
        }
        $annualGdd5.Add($gdd5)
        $annualGdd10.Add($gdd10)
        $annualWarmPrecipitation.Add($warmPrecipitation)
        $annualWinterMinimums.Add($winterMinimum)
    }

    $safeSpringDay = [int](Get-PercentileValue $springFrostDays.ToArray() 0.80)
    $safeAutumnDay = [int](Get-PercentileValue $autumnFrostDays.ToArray() 0.20)
    $frostFreeDays = [Math]::Max(0, $safeAutumnDay - $safeSpringDay)
    $gdd5Average = ($annualGdd5 | Measure-Object -Average).Average
    $gdd10Average = ($annualGdd10 | Measure-Object -Average).Average
    $precipitationAverage = ($annualWarmPrecipitation | Measure-Object -Average).Average
    $winterMinimumP10 = Get-PercentileValue $annualWinterMinimums.ToArray() 0.10
    $temperatureLabel = if ($frostFreeDays -lt 120 -or $gdd10Average -lt 1200) {
        "прохладный"
    } elseif ($frostFreeDays -gt 190 -or $gdd10Average -gt 2600) {
        "тёплый"
    } else {
        "умеренный"
    }
    $moistureLabel = if ($precipitationAverage -lt 260) {
        "влажность низкая"
    } elseif ($precipitationAverage -gt 480) {
        "влажность высокая"
    } else {
        "влажность умеренная"
    }
    $safeSpring = Get-DateFromCanonicalDay $safeSpringDay 2026
    $safeAutumn = Get-DateFromCanonicalDay $safeAutumnDay 2026

    return [PSCustomObject]@{
        City = $City.City
        Climate = "$temperatureLabel, $moistureLabel"
        SafeSpring = $safeSpring.ToString("dd.MM")
        SafeAutumn = $safeAutumn.ToString("dd.MM")
        FrostFreeDays = $frostFreeDays
        Gdd5 = [Math]::Round($gdd5Average)
        Gdd10 = [Math]::Round($gdd10Average)
        WarmSeasonPrecipitationMm = [Math]::Round($precipitationAverage)
        WinterMinimumP10C = [Math]::Round($winterMinimumP10, 1)
        TomatoStart = $safeSpring.AddDays(3).ToString("dd.MM")
        CarrotStart = $safeSpring.AddDays(-14).ToString("dd.MM")
        EggplantStart = $safeSpring.AddDays(16).ToString("dd.MM")
        SourceYears = $annualGdd5.Count
        Timezone = $Response.timezone
    }
}

$latitudeList = ($cities | ForEach-Object {
    $_.Latitude.ToString("0.0000", [Globalization.CultureInfo]::InvariantCulture)
}) -join ","
$longitudeList = ($cities | ForEach-Object {
    $_.Longitude.ToString("0.0000", [Globalization.CultureInfo]::InvariantCulture)
}) -join ","
$batchUrl = "https://archive-api.open-meteo.com/v1/archive?latitude=$latitudeList&longitude=$longitudeList&start_date=2006-01-01&end_date=2025-12-31&daily=temperature_2m_min%2Ctemperature_2m_max%2Cprecipitation_sum&timezone=auto"
$rawBatchResponse = Invoke-RestMethod -Uri $batchUrl -Method Get
$batchResponse = @($rawBatchResponse)

if ($batchResponse.Count -ne $cities.Count) {
    throw "Expected $($cities.Count) climate responses, received $($batchResponse.Count)"
}

$results = for ($index = 0; $index -lt $cities.Count; $index++) {
    Get-ClimateForCity -City $cities[$index] -Response $batchResponse[$index]
}

if ($OutputPath) {
    $results | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding UTF8
}

$results | Format-Table -AutoSize
