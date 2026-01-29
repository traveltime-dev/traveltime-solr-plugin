#!/usr/bin/env bash

# Version-specific configuration for Solr 9.4
export DOCKER_EXEC_USER_FLAG="-u 0"
export SCORE_FIELD=",score"

# Source the common test script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source "${SCRIPT_DIR}/../../../../common/src/test/resources/run-tests.sh"
