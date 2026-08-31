#!/bin/bash

#
# Copyright 2024-2026 VulpesStudios & Contributers
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

JAVA_FLAGS=()

for arg in "$@"; do
  if [ "$arg" == "--dbLogging" ]; then
    JAVA_FLAGS+=("-Dvc.db.logging=true")
  fi
  if [ "$arg" == "--dbTiming" ]; then
    JAVA_FLAGS+=("-Dvc.db.timing=true")
  fi
  if [ "$arg" == "--softwareLogging" ]; then
      JAVA_FLAGS+=("-Dvc.software.logging=true")
  fi
  if [ "$arg" == "--softwareTiming" ]; then
        JAVA_FLAGS+=("-Dvc.software.timing=true")
    fi
done

rm -rf build/meta-repo/*
./gradlew copyFilesForMetaRepo

cd build || exit

mkdir -p run/devCloud/launcher/dependencies/vulpescloud
cp meta-repo/* run/devCloud/launcher/dependencies/vulpescloud/

cat << 'EOF' > run/devCloud/launcher/launcher-config.toml
[auto-updates]

    branch = "invalid"

    enabled = false
EOF

cd run/devCloud || exit

java "${JAVA_FLAGS[@]}" --enable-native-access=ALL-UNNAMED -Dio.netty.noUnsafe=true --sun-misc-unsafe-memory-access=allow -jar launcher/dependencies/vulpescloud/vulpescloud-launcher.jar