param(
  [string]$ProjectRoot = $null
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
  $ProjectRoot = Join-Path $PSScriptRoot "..\\firecasting"
}

$ProjectRoot = (Resolve-Path -Path $ProjectRoot).Path

Set-Location -Path $ProjectRoot

# Regenerate OpenAPI snapshot (writes openapi.yaml)
./mvnw.cmd --% -pl application -Dtest=dk.gormkrings.contract.OpenApiSnapshotTest -Dopenapi.snapshot.update=true test
