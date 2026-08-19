if (-not $env:IMD_REPO) {
    $env:IMD_REPO = "C:\SpringBootWorks\IMD"
}

$envFile = Join-Path $PSScriptRoot "..\.env.properties"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $key, $value = $line.Split("=", 2)
        if ($key.StartsWith("IMD_") -and -not [Environment]::GetEnvironmentVariable($key, "Process")) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

$baseUrl = if ($env:IMD_BASE_URL) { $env:IMD_BASE_URL } else { "http://127.0.0.1:8000" }
$uri = [System.Uri]$baseUrl
$hostName = if ($uri.Host) { $uri.Host } else { "127.0.0.1" }
$port = if (-not $uri.IsDefaultPort) { $uri.Port } else { 8000 }

python -m uvicorn app:app --host $hostName --port $port
