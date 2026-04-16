#!/bin/bash

JAVA_FLAGS=()

for arg in "$@"; do
  if [ "$arg" == "--debugLogging" ]; then
    JAVA_FLAGS+=("-Dvc.db.logging=true")
  fi
  if [ "$arg" == "--dbTiming" ]; then
    JAVA_FLAGS+=("-Dvc.db.timing=true")
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