#!/usr/bin/env bash

./gradlew generatePomFileForLibraryPublication publish bintrayUpload -PbintrayUser=$bintrayUser -PbintrayKey=$bintrayKey -PdryRun=false
