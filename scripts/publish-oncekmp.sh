#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-central}"

GROUP="${ONCEKMP_GROUP:-space.joechen}"
ARTIFACT_ID="${ONCEKMP_ARTIFACT_ID:-oncekmp}"
VERSION="${ONCEKMP_VERSION:-0.1.0}"
REPO_URL="${ONCEKMP_REPO_URL:-https://github.com/joechen/OnceKmp}"
SCM_CONNECTION="${ONCEKMP_SCM_CONNECTION:-scm:git:git://github.com/joechen/OnceKmp.git}"
SCM_DEVELOPER_CONNECTION="${ONCEKMP_SCM_DEVELOPER_CONNECTION:-scm:git:ssh://git@github.com/joechen/OnceKmp.git}"

COMMON_ARGS=(
  "-PONCEKMP_GROUP=${GROUP}"
  "-PONCEKMP_ARTIFACT_ID=${ARTIFACT_ID}"
  "-PONCEKMP_VERSION=${VERSION}"
  "-PONCEKMP_REPO_URL=${REPO_URL}"
  "-PONCEKMP_SCM_CONNECTION=${SCM_CONNECTION}"
  "-PONCEKMP_SCM_DEVELOPER_CONNECTION=${SCM_DEVELOPER_CONNECTION}"
)

if [[ "${MODE}" == "local" ]]; then
  ./gradlew :onceKmp:publishToMavenLocal "${COMMON_ARGS[@]}" "-PONCEKMP_SIGN_PUBLICATIONS=false"
  exit 0
fi

: "${ORG_GRADLE_PROJECT_mavenCentralUsername:?Missing mavenCentralUsername}"
: "${ORG_GRADLE_PROJECT_mavenCentralPassword:?Missing mavenCentralPassword}"
: "${ORG_GRADLE_PROJECT_signingInMemoryKey:?Missing signingInMemoryKey}"
: "${ORG_GRADLE_PROJECT_signingInMemoryKeyPassword:?Missing signingInMemoryKeyPassword}"

./gradlew :onceKmp:publishAndReleaseToMavenCentral "${COMMON_ARGS[@]}" "-PONCEKMP_SIGN_PUBLICATIONS=true"
