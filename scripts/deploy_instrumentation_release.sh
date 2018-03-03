#!/usr/bin/env bash

./gradlew generatePomFileForLibraryPublication publish :instrumentation:bintrayUpload :instrumentation-runner:bintrayUpload -PbintrayUser=$bintrayUser -PbintrayKey=$bintrayKey -PdryRun=false --info
