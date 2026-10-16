#!/usr/bin/env bash
set -euo pipefail
backend="$(cd "$(dirname "$0")/.." && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac --release 17 -d "$classes" \
  "$backend/src/main/java/io/metersphere/workstation/service/SqlQueryBatchExecutor.java" \
  "$backend/tests/SqlQueryBatchExecutorContract.java"
java -cp "$classes" io.metersphere.workstation.service.SqlQueryBatchExecutorContract
