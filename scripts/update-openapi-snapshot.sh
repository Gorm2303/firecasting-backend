#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${1:-$(cd "$(dirname "$0")/../firecasting" && pwd)}"
cd "$PROJECT_ROOT"

./mvnw -pl application -Dtest=dk.gormkrings.contract.OpenApiSnapshotTest -Dopenapi.snapshot.update=true test
