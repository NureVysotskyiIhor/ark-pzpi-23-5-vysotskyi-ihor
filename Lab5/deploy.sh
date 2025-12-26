#!/bin/bash
# ============================================================================
# POLLS SYSTEM DEPLOYMENT SCRIPT - Lab 5
# Automates environment check, build and backend startup
# ============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ============================================================================
# FUNCTIONS
# ============================================================================

print_header() {
    echo -e "\n${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} $1"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}\n"
}

print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_error()   { echo -e "${RED}❌ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_info()    { echo -e "${BLUE}ℹ️  $1${NC}"; }

check_command() {
    command -v "$1" &>/dev/null
}

# ============================================================================
# STEP 1: ENVIRONMENT CHECK
# ============================================================================

print_header "STEP 1: ENVIRONMENT CHECK"

print_info "Checking Java..."
if check_command java; then
    print_success "Java found: $(java -version 2>&1 | head -n 1)"
else
    print_error "Java not found (Java 21+ required)"
    exit 1
fi

print_info "Checking Maven..."
if check_command mvn; then
    print_success "Maven found: $(mvn -v | head -n 1)"
else
    print_error "Maven not found"
    exit 1
fi

print_info "Checking Git..."
if check_command git; then
    print_success "Git found"
else
    print_warning "Git not found (optional)"
fi

# ============================================================================
# STEP 2: DATABASE CONFIGURATION (NEON)
# ============================================================================

print_header "STEP 2: DATABASE CONFIGURATION (NEON)"

NEON_HOST="ep-wandering-mountain-aghgpzun-pooler.c-2.eu-central-1.aws.neon.tech"
NEON_USER="neondb_owner"
NEON_DB="neondb"

echo "  • Host: $NEON_HOST"
echo "  • User: $NEON_USER"
echo "  • Database: $NEON_DB"

read -s -p "Enter Neon DB password: " DB_PASSWORD
echo ""
export DB_PASSWORD

if check_command psql; then
    print_info "Testing database connection..."
    if PGPASSWORD="$DB_PASSWORD" psql -h "$NEON_HOST" -U "$NEON_USER" -d "$NEON_DB" -c "SELECT 1;" &>/dev/null; then
        print_success "Database connection successful"
    else
        print_warning "Database connection failed (continuing anyway)"
    fi
else
    print_warning "psql not installed, skipping DB check"
fi

# ============================================================================
# STEP 3: PROJECT SETUP
# ============================================================================

print_header "STEP 3: PROJECT SETUP"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/polls-backend"

print_info "Script directory: $SCRIPT_DIR"
print_info "Backend directory: $BACKEND_DIR"

if [ ! -d "$BACKEND_DIR" ]; then
    print_error "Backend directory not found"
    exit 1
fi

cd "$BACKEND_DIR"
print_success "Changed directory to backend"

# ============================================================================
# STEP 4: CLEAN & DEPENDENCIES
# ============================================================================

print_header "STEP 4: CLEAN & DEPENDENCIES"

print_info "Cleaning project..."
mvn clean -q
print_success "Clean completed"

print_info "Resolving dependencies..."
mvn dependency:resolve -q
print_success "Dependencies resolved"

# ============================================================================
# STEP 5: BUILD
# ============================================================================

print_header "STEP 5: BUILD BACKEND"

print_info "Building project (tests skipped)..."
mvn clean install -DskipTests -q
print_success "Build successful"

# ============================================================================
# JAR CHECK (NO HARDCODED NAME)
# ============================================================================

print_info "Searching for JAR file..."
JAR_FILE=$(ls target/*.jar 2>/dev/null | grep -v original | head -n 1)

if [ -z "$JAR_FILE" ]; then
    print_error "JAR file not found in target/"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
print_success "JAR created: $JAR_FILE ($JAR_SIZE)"

# ============================================================================
# STEP 6: RUN BACKEND
# ============================================================================

print_header "STEP 6: RUN BACKEND"

echo "  • Port: 8080"
echo "  • Database: Neon"
echo "  • Profile: development"

read -p "Start backend now? (y/n): " START_NOW

if [ "$START_NOW" = "y" ]; then
    print_info "Starting backend..."
    mvn spring-boot:run \
        -Dspring-boot.run.arguments="--spring.datasource.password=$DB_PASSWORD"
else
    print_info "Startup skipped"
    echo "To start manually:"
    echo "  cd $BACKEND_DIR"
    echo "  export DB_PASSWORD=******"
    echo "  mvn spring-boot:run"
fi

# ============================================================================
# FINISH
# ============================================================================

if [ "$START_NOW" != "y" ]; then
    print_header "DEPLOYMENT FINISHED"
    print_success "Backend is ready to run"
    echo "Swagger UI: http://localhost:8080/swagger-ui.html"
fi
