#!/usr/bin/env bash

../gradlew clean build generatePomFileForLibraryPublication publish bintrayUpload -PbintrayUser=$bintrayUser -PbintrayKey=$bintrayKey -PdryRun=false
