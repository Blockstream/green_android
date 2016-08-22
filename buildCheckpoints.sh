#!/bin/bash
set -e

./gradlew --project-dir=bitcoinj/tools buildMainnetCheckpoints && mv bitcoinj/tools/checkpoints app/src/production/assets/checkpoints
./gradlew --project-dir=bitcoinj/tools buildTestnetCheckpoints && mv bitcoinj/tools/checkpoints-testnet app/src/btctestnet/assets/checkpoints
