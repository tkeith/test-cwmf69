#!/bin/bash

# Delayed Messaging System - Database Migration Script
# Version: 1.0.0
# Dependencies:
# - @prisma/client v4.x
# - postgresql v14

set -euo pipefail

# Constants
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly LOG_DIR="/var/log/dms/migrations"
readonly LOCK_FILE="/tmp/dms_migration.lock"
readonly MAX_RETRIES=3
readonly TIMESTAMP=$(date +%Y%m%d_%H%M%S)
readonly LOG_FILE="${LOG_DIR}/migration_${TIMESTAMP}.log"

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly NC='\033[0m' # No Color

# Initialize logging
init_logging() {
    mkdir -p "${LOG_DIR}"
    touch "${LOG_FILE}"
    exec 1> >(tee -a "${LOG_FILE}")
    exec 2> >(tee -a "${LOG_FILE}" >&2)
    
    # Rotate old logs (keep last 30 days)
    find "${LOG_DIR}" -type f -mtime +30 -delete
}

# Logging functions
log_info() { echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] $1" ; }
log_success() { echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [SUCCESS] ${GREEN}$1${NC}" ; }
log_warning() { echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [WARNING] ${YELLOW}$1${NC}" ; }
log_error() { echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [ERROR] ${RED}$1${NC}" >&2 ; }

# Cleanup function
cleanup() {
    local exit_code=$?
    if [ -f "${LOCK_FILE}" ]; then
        rm -f "${LOCK_FILE}"
        log_info "Released migration lock"
    fi
    
    if [ $exit_code -ne 0 ]; then
        log_error "Migration failed with exit code: ${exit_code}"
    fi
    
    log_info "Migration script completed at $(date)"
    exit ${exit_code}
}

# Set up error handling
trap cleanup EXIT
trap 'log_error "Caught SIGINT signal"; exit 1' SIGINT
trap 'log_error "Caught SIGTERM signal"; exit 1' SIGTERM

# Check database connection
check_database_connection() {
    local retries=0
    local max_retries=3
    
    # Source environment variables
    if [ -f "${SCRIPT_DIR}/../.env" ]; then
        source "${SCRIPT_DIR}/../.env"
    else
        log_error "Environment file not found"
        return 1
    fi
    
    # Validate DATABASE_URL format
    if [[ ! "${DATABASE_URL}" =~ ^postgresql://.*$ ]]; then
        log_error "Invalid DATABASE_URL format"
        return 1
    }
    
    # Extract database connection details
    local db_host=$(echo "${DATABASE_URL}" | sed -n 's/.*@\([^:]*\).*/\1/p')
    local db_port=$(echo "${DATABASE_URL}" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "${DATABASE_URL}" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    local db_user=$(echo "${DATABASE_URL}" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    
    while [ $retries -lt $max_retries ]; do
        if PGPASSWORD="${DATABASE_PASSWORD}" pg_isready -h "${db_host}" -p "${db_port}" -U "${db_user}" -d "${db_name}" -t 5; then
            log_success "Database connection successful"
            return 0
        fi
        
        retries=$((retries + 1))
        log_warning "Database connection attempt ${retries}/${max_retries} failed. Retrying..."
        sleep 5
    done
    
    log_error "Failed to connect to database after ${max_retries} attempts"
    return 1
}

# Verify migration prerequisites
verify_migration_prerequisites() {
    # Check for existing migration lock
    if [ -f "${LOCK_FILE}" ]; then
        log_error "Migration lock exists. Another migration might be in progress"
        return 1
    fi
    
    # Check disk space (minimum 1GB required)
    local available_space=$(df -BG "${LOG_DIR}" | awk 'NR==2 {print $4}' | sed 's/G//')
    if [ "${available_space}" -lt 1 ]; then
        log_error "Insufficient disk space for migration logs"
        return 1
    }
    
    # Verify schema file exists
    if [ ! -f "${SCRIPT_DIR}/../prisma/schema.prisma" ]; then
        log_error "Prisma schema file not found"
        return 1
    }
    
    # Check Prisma CLI installation
    if ! command -v npx &> /dev/null; then
        log_error "npx command not found. Please install Node.js and npm"
        return 1
    }
    
    return 0
}

# Run database migrations
run_migrations() {
    local start_time=$(date +%s)
    
    log_info "Starting database migration at $(date)"
    
    # Create migration lock
    touch "${LOCK_FILE}"
    
    # Generate Prisma client
    log_info "Generating Prisma client..."
    if ! npx prisma generate; then
        log_error "Failed to generate Prisma client"
        return 1
    fi
    
    # Run migrations
    log_info "Running database migrations..."
    if ! npx prisma migrate deploy --preview-feature; then
        log_error "Migration failed"
        return 1
    fi
    
    # Verify migration status
    log_info "Verifying migration status..."
    if ! npx prisma migrate status; then
        log_warning "Migration status check failed"
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_success "Migration completed successfully in ${duration} seconds"
    return 0
}

# Main execution
main() {
    log_info "Starting migration script"
    
    # Initialize logging
    init_logging
    
    # Check prerequisites
    if ! verify_migration_prerequisites; then
        log_error "Prerequisites check failed"
        exit 1
    fi
    
    # Check database connection
    if ! check_database_connection; then
        log_error "Database connection check failed"
        exit 1
    fi
    
    # Run migrations
    if ! run_migrations; then
        log_error "Migration failed"
        exit 1
    fi
    
    log_success "Migration process completed successfully"
    exit 0
}

# Start execution
main