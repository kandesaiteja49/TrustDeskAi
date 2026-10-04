#!/bin/bash

# Configuration
export BASE_URL="http://localhost:8083/api/v1"
export CONVERSATION_ID="conv-$(date +%s)"

# Colors for logging
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Ensure we are using the current directory for script execution if needed
# export PATH=$PATH:.
