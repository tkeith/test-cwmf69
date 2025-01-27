#!/usr/bin/env bash

# Database Setup Script for Delayed Messaging System
# Version: 1.0.0
# PostgreSQL Version: 14.x
# Prisma Version: 4.x

set -euo pipefail
IFS=$'\n\t'

# Global variables with default values
DB_NAME=${DB_NAME:-delayed_messaging}
DB_USER=${POSTGRES_USER:-postgres}
DB_PASSWORD=${POSTGRES_PASSWORD:-postgres}
DB_HOST=${POSTGRES_HOST:-localhost}
DB_PORT=${POSTGRES_PORT:-5432}
LOG_LEVEL=${LOG_LEVEL:-INFO}
BACKUP_DIR="./backups"
MAX_RETRIES=3
RETRY_DELAY=5

# Logging setup
LOGFILE="setup.log"
ERRORLOG="error.log"

log() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "${timestamp}|${level}|${message}" | tee -a "$LOGFILE"
    if [[ "$level" == "ERROR" ]]; then
        echo "${timestamp}|${level}|${message}" >> "$ERRORLOG"
    fi
}

error_exit() {
    log "ERROR" "$1"
    cleanup 1
    exit 1
}

# Function to check all prerequisites
check_prerequisites() {
    log "INFO" "Checking prerequisites..."

    # Check PostgreSQL client tools
    if ! command -v psql &> /dev/null; then
        error_exit "PostgreSQL client tools not found. Please install PostgreSQL 14."
    fi

    # Verify PostgreSQL version
    local pg_version=$(psql --version | grep -oE '[0-9]{1,2}\.' | cut -d. -f1)
    if [[ "$pg_version" -lt 14 ]]; then
        error_exit "PostgreSQL version 14 or higher required. Found version $pg_version"
    fi

    # Check Prisma CLI
    if ! command -v npx &> /dev/null; then
        error_exit "npx not found. Please install Node.js and npm."
    fi

    # Verify environment variables
    if [[ -z "${DATABASE_URL:-}" ]]; then
        export DATABASE_URL="postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
        log "INFO" "DATABASE_URL not set, using constructed value"
    fi

    # Check schema file
    if [[ ! -f "../prisma/schema.prisma" ]]; then
        error_exit "schema.prisma file not found"
    fi

    # Create backup directory
    mkdir -p "$BACKUP_DIR"

    # Test database connection
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "postgres" -c '\q' &> /dev/null; then
        error_exit "Cannot connect to PostgreSQL server"
    fi

    log "INFO" "Prerequisites check completed successfully"
}

# Function to create database with retries
create_database() {
    local retries=0
    local db_exists

    while [[ $retries -lt $MAX_RETRIES ]]; do
        log "INFO" "Attempting to create database (attempt $((retries + 1))/${MAX_RETRIES})"

        # Check if database exists
        if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
            log "INFO" "Database $DB_NAME already exists"
            return 0
        fi

        # Create database
        if PGPASSWORD="$DB_PASSWORD" createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME"; then
            log "INFO" "Database $DB_NAME created successfully"
            
            # Setup extensions
            setup_extensions
            return 0
        else
            retries=$((retries + 1))
            if [[ $retries -lt $MAX_RETRIES ]]; then
                log "WARN" "Failed to create database, retrying in $RETRY_DELAY seconds..."
                sleep $RETRY_DELAY
            fi
        fi
    done

    error_exit "Failed to create database after $MAX_RETRIES attempts"
}

# Function to setup required PostgreSQL extensions
setup_extensions() {
    log "INFO" "Setting up PostgreSQL extensions..."

    local extensions=("uuid-ossp" "pgcrypto" "btree_gist")
    
    for ext in "${extensions[@]}"; do
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS \"$ext\";" || {
            error_exit "Failed to create extension $ext"
        }
    done

    log "INFO" "Extensions setup completed"
}

# Function to create database backup
backup_database() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="${BACKUP_DIR}/${DB_NAME}_${timestamp}.sql"

    log "INFO" "Creating database backup: $backup_file"

    PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F p > "$backup_file" || {
        error_exit "Failed to create database backup"
    }

    # Verify backup file
    if [[ ! -s "$backup_file" ]]; then
        error_exit "Backup file is empty"
    fi

    # Cleanup old backups (keep last 5)
    find "$BACKUP_DIR" -name "${DB_NAME}_*.sql" -type f -printf '%T@ %p\n' | \
        sort -n | head -n -5 | cut -d' ' -f2- | xargs -r rm

    log "INFO" "Database backup completed: $backup_file"
    echo "$backup_file"
}

# Function to run Prisma migrations
run_migrations() {
    log "INFO" "Running database migrations..."

    # Create backup before migrations
    local backup_file=$(backup_database)

    # Run migrations
    if ! npx prisma migrate deploy --schema="../prisma/schema.prisma"; then
        log "ERROR" "Migration failed, attempting rollback..."
        
        # Rollback from backup
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" < "$backup_file"
        
        error_exit "Migration failed and rollback completed"
    fi

    # Generate Prisma client
    npx prisma generate --schema="../prisma/schema.prisma"

    log "INFO" "Database migrations completed successfully"
}

# Cleanup function
cleanup() {
    local exit_code=$1
    
    # Archive logs if there were errors
    if [[ $exit_code -ne 0 && -f "$ERRORLOG" ]]; then
        local timestamp=$(date +%Y%m%d_%H%M%S)
        mv "$ERRORLOG" "${BACKUP_DIR}/error_${timestamp}.log"
    fi

    # Remove temporary files
    rm -f *.tmp

    log "INFO" "Cleanup completed with exit code $exit_code"
}

# Main execution
main() {
    log "INFO" "Starting database setup for Delayed Messaging System"

    # Run setup steps
    check_prerequisites
    create_database
    run_migrations

    log "INFO" "Database setup completed successfully"
    cleanup 0
}

# Trap for cleanup
trap 'cleanup $?' EXIT

# Execute main function
main