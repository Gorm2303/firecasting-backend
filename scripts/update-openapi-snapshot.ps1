param(
  [string]$ProjectRoot = "d:\Github\firecasting-backend\firecasting"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location -Path $ProjectRoot

# Regenerate OpenAPI snapshot (writes openapi.yaml)
./mvnw.cmd -pl application -Dtest=dk.gormkrings.contract.OpenApiSnapshotTest -Dopenapi.snapshot.update=true test
