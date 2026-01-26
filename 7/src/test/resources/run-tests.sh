#!/usr/bin/env bash

# Version-specific configuration for Solr 7
export DOCKER_EXEC_USER_FLAG=""
export POST_COMMAND="post"
export SCORE_FIELD=""

# Source the common test script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source "${SCRIPT_DIR}/../../../../common/src/test/resources/run-tests.sh"
