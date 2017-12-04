#!/usr/bin/env bash

./gradlew generatePomFileForLibraryPublication publish :instrumentation:bintrayUpload -PbintrayUser=$bintrayUser -PbintrayKey=$bintrayKey -PdryRun=false
