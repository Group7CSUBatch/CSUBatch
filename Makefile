# Makefile for CSUbatch Batch Scheduling System
# Provides build, run, test, and clean operations

# Java compiler and flags
JAVAC = javac
JAVA = java
JAVAC_FLAGS = -d ./target/classes -cp ./target/classes
MVN = mvn

# Source and target directories
SRC_DIR = ./src/main/java
TEST_DIR = ./src/test/java
TARGET_DIR = ./target
CLASSES_DIR = $(TARGET_DIR)/classes
TEST_CLASSES_DIR = $(TARGET_DIR)/test-classes

# Main class
MAIN_CLASS = com.project.unified.UnifiedApp

# Jar file
JAR_FILE = $(TARGET_DIR)/CSUbatch.jar

# Source files
JAVA_FILES = $(shell find $(SRC_DIR) -name "*.java")
TEST_FILES = $(shell find $(TEST_DIR) -name "*.java")

# Default target
all: compile

# Clean the project
clean:
	@echo "Cleaning project..."
	@rm -rf $(TARGET_DIR)
	@mkdir -p $(CLASSES_DIR)
	@mkdir -p $(TEST_CLASSES_DIR)
	@echo "Clean complete."

# Compile the source code
compile:
	@echo "Compiling source files..."
	@mkdir -p $(CLASSES_DIR)
	@$(JAVAC) $(JAVAC_FLAGS) $(JAVA_FILES)
	@echo "Compilation complete."

# Compile test files
compile-tests:
	@echo "Compiling test files with Maven..."
	@$(MVN) compile test-compile
	@echo "Test compilation complete."

# Run tests
test: compile-tests
	@echo "Running tests with Maven..."
	@$(MVN) test
	@echo "Tests complete."

# Create executable jar
jar: compile
	@echo "Creating executable JAR..."
	@$(MVN) package
	@echo "JAR created: $(JAR_FILE)"

# Run the application
run: jar
	@echo "Running CSUbatch..."
	@java -jar $(JAR_FILE)

# Build everything (compile, test, create jar)
build: clean jar test
	@echo "Build complete."

# Run with Maven
mvn-run:
	@echo "Running with Maven..."
	@$(MVN) exec:java -Dexec.mainClass="$(MAIN_CLASS)"

# Maven clean, compile, test, package
mvn-build:
	@echo "Building with Maven..."
	@$(MVN) clean package

# Quick build without checkstyle validation and jacoco
quick-build:
	@echo "Quick building (bypassing checkstyle and jacoco)..."
	@$(MVN) clean package -Dcheckstyle.skip=true -Djacoco.skip=true
	@echo "Quick build complete."

# Run with quick build (bypass checkstyle and jacoco)
quick-run: quick-build
	@echo "Running CSUbatch (quick build)..."
	@java -jar $(JAR_FILE)

# Help target
help:
	@echo "CSUbatch Makefile Help"
	@echo "---------------------"
	@echo "Available targets:"
	@echo "  all (default): Compile the project"
	@echo "  clean: Remove generated files and directories"
	@echo "  compile: Compile the source code"
	@echo "  compile-tests: Compile the test code"
	@echo "  test: Run tests"
	@echo "  jar: Create executable JAR"
	@echo "  run: Run the application"
	@echo "  build: Build everything (clean, compile, test, jar)"
	@echo "  mvn-run: Run using Maven"
	@echo "  mvn-build: Build using Maven"
	@echo "  quick-build: Build without checkstyle and jacoco validation"
	@echo "  quick-run: Run after quick build (bypasses validations)"
	@echo "  help: Show this help message"

# Phony targets
.PHONY: all clean compile compile-tests test jar run build mvn-run mvn-build quick-build quick-run help clean-backups clean-logs fix-plus fix-indent clean-all

# Additional maintenance targets

# Clean backup files created during fixes
clean-backups:
	@echo "Searching for backup (.bak) files..."
	@count=$$(find src -name "*.bak" -type f | wc -l); \
	if [ $$count -eq 0 ]; then \
		echo "No .bak files found."; \
	else \
		echo "Found $$count .bak files. Removing..."; \
		find src -name "*.bak" -type f -delete; \
		echo "All .bak files have been removed successfully."; \
	fi

# Clean log files
clean-logs:
	@echo "Clearing all files in logs directory..."
	@if [ -d "logs" ]; then \
		rm -f logs/*; \
		echo "All log files have been removed."; \
	else \
		mkdir -p logs; \
		echo "Created logs directory (it didn't exist)."; \
	fi
	@echo "Log cleanup completed successfully."

# Remove plus signs from the start of lines in Java files
fix-plus:
	@echo "Removing + signs from Java files..."
	@for file in $$(find src -name "*.java" -type f); do \
		echo "Processing $$file"; \
		cp "$$file" "$$file.bak"; \
		sed -i 's/^                + //' "$$file"; \
	done
	@echo "All + signs have been removed. Backup files have been saved with .bak extension."

# Fix indentation in Java files
fix-indent:
	@echo "Fixing indentation issues in Java files..."
	@for file in $$(find src -name "*.java" -type f); do \
		echo "Processing $$file"; \
		cp "$$file" "$$file.bak"; \
		temp_file="$$file.temp"; \
		while IFS= read -r line; do \
			if [[ $$line =~ ^package ]] || [[ $$line =~ ^import ]] || [[ $$line =~ ^\/\*\* ]] || \
				[[ $$line =~ ^\* ]] || [[ $$line =~ ^public\ class ]] || [[ $$line =~ ^public\ enum ]] || \
				[[ $$line =~ ^class ]] || [[ $$line =~ ^@SuppressWarnings ]] || [[ $$line == "}" ]] || \
				[[ -z $$line ]]; then \
				echo "$$line" >> "$$temp_file"; \
			else \
				echo "    $$line" >> "$$temp_file"; \
			fi \
		done < "$$file"; \
		mv "$$temp_file" "$$file"; \
	done
	@echo "Indentation fixes completed. Backup files have been saved with .bak extension."

# Clean everything (logs, backups, and build artifacts)
clean-all: clean clean-logs clean-backups
	@echo "Project completely cleaned." 