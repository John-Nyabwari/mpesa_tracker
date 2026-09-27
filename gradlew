#!/bin/sh

#
# Copyright © 2015 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

# Resolve APP_HOME
app_path=$0
while
    APP_HOME=${app_path%"${app_path##*/}"}
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*)   app_path=$link ;;
      *)    app_path=$APP_HOME$link ;;
    esac
done

APP_BASE_NAME=${0##*/}
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s\n' "$PWD" ) || exit

# 1. Ensure .env exists for Secrets Gradle Plugin in CI environments
if [ ! -f "$APP_HOME/.env" ]; then
    if [ -f "$APP_HOME/.env.example" ]; then
        cp "$APP_HOME/.env.example" "$APP_HOME/.env" 2>/dev/null || true
    else
        touch "$APP_HOME/.env" 2>/dev/null || true
    fi
fi

# 2. Ensure debug.keystore exists in CI environments without modifying existing keystores
if [ ! -f "$APP_HOME/debug.keystore" ]; then
    if [ -f "$APP_HOME/debug.keystore.base64" ]; then
        base64 -d "$APP_HOME/debug.keystore.base64" > "$APP_HOME/debug.keystore" 2>/dev/null || true
    elif command -v keytool >/dev/null 2>&1; then
        keytool -genkeypair -v \
            -keystore "$APP_HOME/debug.keystore" \
            -storepass android \
            -alias androiddebugkey \
            -keypass android \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1 || true
    fi
fi

# 3. Auto-accept Android SDK licenses in CI so compileSdk 36.1 can install cleanly
if [ "$CI" = "true" ] || [ "$GITHUB_ACTIONS" = "true" ]; then
    SDK_ROOT="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
    if [ -n "$SDK_ROOT" ]; then
        mkdir -p "$SDK_ROOT/licenses" 2>/dev/null || true
        printf "\n8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n84831b9409646a918e30573bab4c9c91346d8abd\n" > "$SDK_ROOT/licenses/android-sdk-license" 2>/dev/null || true
        printf "\n84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910\n" > "$SDK_ROOT/licenses/android-sdk-preview-license" 2>/dev/null || true
    fi
fi

# 4. In CI, ensure JAVA_HOME points to JDK 21 if available on the runner
if [ "$CI" = "true" ] || [ "$GITHUB_ACTIONS" = "true" ]; then
    if [ -n "$JAVA_HOME_21_X64" ] && [ -d "$JAVA_HOME_21_X64" ]; then
        export JAVA_HOME="$JAVA_HOME_21_X64"
        export PATH="$JAVA_HOME/bin:$PATH"
    elif [ -d "/usr/lib/jvm/temurin-21-jdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk-amd64"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# 5. Resolve and execute the required Gradle distribution from gradle-wrapper.properties
REQ_GRADLE_VER="9.3.1"
if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.properties" ]; then
    EXTRACTED_VER=$(sed -n 's/.*gradle-\([0-9.]*\)-bin\.zip.*/\1/p' "$APP_HOME/gradle/wrapper/gradle-wrapper.properties" | head -n 1)
    if [ -n "$EXTRACTED_VER" ]; then
        REQ_GRADLE_VER="$EXTRACTED_VER"
    fi
fi

if command -v gradle >/dev/null 2>&1; then
    INSTALLED_VER=$(gradle --version 2>/dev/null | awk '/^Gradle / {print $2}' | head -n 1)
    if [ "$INSTALLED_VER" = "$REQ_GRADLE_VER" ]; then
        exec gradle "$@"
    fi
fi

DIST_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${REQ_GRADLE_VER}-bin"
GRADLE_BIN="$DIST_DIR/gradle-${REQ_GRADLE_VER}/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$DIST_DIR"
    DIST_URL="https://services.gradle.org/distributions/gradle-${REQ_GRADLE_VER}-bin.zip"
    echo "Downloading Gradle ${REQ_GRADLE_VER} from ${DIST_URL} ..."
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$DIST_DIR/gradle.zip" "$DIST_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$DIST_DIR/gradle.zip" "$DIST_URL"
    fi
    if [ -f "$DIST_DIR/gradle.zip" ]; then
        unzip -q -o "$DIST_DIR/gradle.zip" -d "$DIST_DIR"
        rm -f "$DIST_DIR/gradle.zip"
        chmod +x "$GRADLE_BIN" 2>/dev/null || true
    fi
fi

if [ -x "$GRADLE_BIN" ]; then
    exec "$GRADLE_BIN" "$@"
elif command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "ERROR: Could not locate or download Gradle ${REQ_GRADLE_VER}." >&2
    exit 1
fi
