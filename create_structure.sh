#!/bin/bash

echo "Creating Nora Tunnel directory structure..."

# Directories
mkdir -p app/src/main/java/com/nora/tunnel/config
mkdir -p app/src/main/java/com/nora/tunnel/data/local/dao
mkdir -p app/src/main/java/com/nora/tunnel/data/local/entity
mkdir -p app/src/main/java/com/nora/tunnel/data/preferences
mkdir -p app/src/main/java/com/nora/tunnel/data/repository
mkdir -p app/src/main/java/com/nora/tunnel/di
mkdir -p app/src/main/java/com/nora/tunnel/domain/model
mkdir -p app/src/main/java/com/nora/tunnel/domain/repository
mkdir -p app/src/main/java/com/nora/tunnel/domain/usecase
mkdir -p app/src/main/java/com/nora/tunnel/protocol/wireguard
mkdir -p app/src/main/java/com/nora/tunnel/receiver
mkdir -p app/src/main/java/com/nora/tunnel/security
mkdir -p app/src/main/java/com/nora/tunnel/ui/components
mkdir -p app/src/main/java/com/nora/tunnel/ui/screens
mkdir -p app/src/main/java/com/nora/tunnel/ui/theme
mkdir -p app/src/main/java/com/nora/tunnel/ui/viewmodel
mkdir -p app/src/main/java/com/nora/tunnel/validation
mkdir -p app/src/test/java/com/nora/tunnel/validation
mkdir -p app/src/main/res/values

mkdir -p app/src/main/res/xml
mkdir -p gradle

echo "Folders created successfully!"
