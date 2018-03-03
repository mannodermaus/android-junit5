#!/usr/bin/env bash

./gradlew generatePomFileForLibraryPublication publish :android-junit5:bintrayUpload :android-junit5-embedded-runtime:bintrayUpload -PbintrayUser=$bintrayUser -PbintrayKey=$bintrayKey -PdryRun=false --info
