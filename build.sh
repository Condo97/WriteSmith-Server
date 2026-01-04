#!/bin/bash

# WriteSmith Server Build Script
# Builds the project using Gradle (same as IntelliJ)

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_usage() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  build     Build the project (default)"
    echo "  clean     Clean build outputs"
    echo "  jar       Build the fat JAR only"
    echo "  test      Run tests"
    echo "  run       Build and run the server"
    echo "  rebuild   Clean and build"
    echo ""
}

# Default command
COMMAND="${1:-build}"

case "$COMMAND" in
    build)
        echo -e "${YELLOW}Building WriteSmith Server...${NC}"
        ./gradlew :lib:build -x test
        echo -e "${GREEN}✓ Build complete!${NC}"
        echo "JAR location: lib/out/WriteSmith_Server.jar"
        ;;
    clean)
        echo -e "${YELLOW}Cleaning build outputs...${NC}"
        ./gradlew clean
        rm -rf lib/out
        echo -e "${GREEN}✓ Clean complete!${NC}"
        ;;
    jar)
        echo -e "${YELLOW}Building JAR...${NC}"
        ./gradlew :lib:jar
        echo -e "${GREEN}✓ JAR built!${NC}"
        echo "JAR location: lib/out/WriteSmith_Server.jar"
        ;;
    test)
        echo -e "${YELLOW}Running tests...${NC}"
        ./gradlew :lib:test
        echo -e "${GREEN}✓ Tests complete!${NC}"
        ;;
    run)
        echo -e "${YELLOW}Building and running WriteSmith Server...${NC}"
        ./gradlew :lib:jar
        echo -e "${GREEN}Starting server...${NC}"
        java -jar lib/out/WriteSmith_Server.jar
        ;;
    rebuild)
        echo -e "${YELLOW}Rebuilding WriteSmith Server...${NC}"
        ./gradlew clean :lib:build -x test
        echo -e "${GREEN}✓ Rebuild complete!${NC}"
        echo "JAR location: lib/out/WriteSmith_Server.jar"
        ;;
    help|--help|-h)
        print_usage
        ;;
    *)
        echo -e "${RED}Unknown command: $COMMAND${NC}"
        print_usage
        exit 1
        ;;
esac

