#!/bin/bash
set -ex

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -z "$1" ]; then
    echo "Usage: $0 <version-dir> <image-name>"
    exit 1
fi

VERSION_DIR="$1"
IMAGE_NAME="$2"

source "$PROJECT_ROOT/$VERSION_DIR/docker-env.sh"

docker build "$PROJECT_ROOT" \
    -f "$SCRIPT_DIR/Dockerfile" \
    --build-arg "SOLR_VERSION=$SOLR_VERSION" \
    --build-arg "VERSION_DIR=$VERSION_DIR" \
    --build-arg "SOLR_CORE_PATH=$SOLR_CORE_PATH" \
    --build-arg "POST_COMMAND=$POST_COMMAND" \
    -t "$IMAGE_NAME"
