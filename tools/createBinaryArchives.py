#!/usr/bin/python3

# Copyright (C) 2019-2021 Julian Valentin, LTeX Development Community
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

import pathlib
import re
import shutil
import subprocess
import tarfile
import tempfile
import urllib.parse
import urllib.request
import zipfile

javaVersion = "11.0.12+7"



def createBinaryArchive(platform: str, arch: str) -> None:
  print(f"Processing platform/arch '{platform}/{arch}'...")
  ltexLsVersion = getLtexLsVersion()
  targetDirPath = pathlib.Path(__file__).parent.parent.joinpath("target")
  ltexLsArchivePath = pathlib.Path(__file__).parent.parent.joinpath(
      targetDirPath, f"ltex-ls-{ltexLsVersion}.tar.gz")

  with tempfile.TemporaryDirectory() as tmpDirPathStr:
    tmpDirPath = pathlib.Path(tmpDirPathStr)

    print("Extracting LTeX LS archive...")
    with tarfile.open(ltexLsArchivePath, "r:gz") as tarFile: tarFile.extractall(path=tmpDirPath)

    ltexLsDirPath = tmpDirPath.joinpath(f"ltex-ls-{ltexLsVersion}")
    relativeJavaDirPath = downloadJava(tmpDirPath, ltexLsDirPath, platform, arch)

    print("Setting default for JAVA_HOME in startup script...")

    if platform == "windows":
      ltexLsDirPath.joinpath("bin", "ltex-ls").unlink()
      binScriptPath = ltexLsDirPath.joinpath("bin", "ltex-ls.bat")
      searchPattern = re.compile("^set REPO=.*$", flags=re.MULTILINE)
    else:
      ltexLsDirPath.joinpath("bin", "ltex-ls.bat").unlink()
      binScriptPath = ltexLsDirPath.joinpath("bin", "ltex-ls")
      searchPattern = re.compile("^BASEDIR=.*$", flags=re.MULTILINE)

    with open(binScriptPath, "r") as file: binScript = file.read()

    if platform == "windows":
      insertStr = f"\r\nif not defined JAVA_HOME set JAVA_HOME=\"%BASEDIR%\\{relativeJavaDirPath}\""
    else:
      insertStr = f"\n[ -z \"$JAVA_HOME\" ] && JAVA_HOME=\"$BASEDIR\"/{relativeJavaDirPath}"

    regexMatch = searchPattern.search(binScript)
    assert regexMatch is not None
    binScript = binScript[:regexMatch.end()] + insertStr + binScript[regexMatch.end():]
    with open(binScriptPath, "w") as file: file.write(binScript)

    ltexLsBinaryArchiveFormat = ("zip" if platform == "windows" else "gztar")
    ltexLsBinaryArchiveExtension = (".zip" if platform == "windows" else ".tar.gz")
    ltexLsBinaryArchivePath = targetDirPath.joinpath(
        f"ltex-ls-{ltexLsVersion}-{platform}-{arch}")
    print(f"Creating binary archive '{ltexLsBinaryArchivePath}{ltexLsBinaryArchiveExtension}'...")
    shutil.make_archive(str(ltexLsBinaryArchivePath), ltexLsBinaryArchiveFormat,
        root_dir=tmpDirPath)
    print("")



def downloadJava(tmpDirPath: pathlib.Path, ltexLsDirPath: pathlib.Path,
      platform: str, arch: str) -> str:
  javaArchiveExtension = (".zip" if platform == "windows" else ".tar.gz")
  javaArchiveName = (f"OpenJDK11U-jdk_{arch}_{platform}_hotspot_"
      f"{javaVersion.replace('+', '_')}{javaArchiveExtension}")

  javaUrl = ("https://github.com/adoptium/temurin11-binaries/releases/download/"
      f"jdk-{urllib.parse.quote_plus(javaVersion)}/{javaArchiveName}")
  javaArchivePath = ltexLsDirPath.joinpath(javaArchiveName)
  print(f"Downloading JDK from '{javaUrl}' to '{javaArchivePath}'...")
  urllib.request.urlretrieve(javaUrl, javaArchivePath)
  print("Extracting JDK archive...")

  if javaArchiveExtension == ".zip":
    with zipfile.ZipFile(javaArchivePath, "r") as zipFile: zipFile.extractall(path=tmpDirPath)
  else:
    with tarfile.open(javaArchivePath, "r:gz") as tarFile: tarFile.extractall(path=tmpDirPath)

  print("Removing JDK archive...")
  javaArchivePath.unlink()

  relativeJavaDirPathString = f"jdk-{javaVersion}"
  jdkDirPath = tmpDirPath.joinpath(relativeJavaDirPathString)
  jmodsDirPath = (jdkDirPath.joinpath("jmods") if platform == "mac" else
      jdkDirPath.joinpath("Contents", "Home", "jmods"))
  javaTargetDirPath = ltexLsDirPath.joinpath(relativeJavaDirPathString)

  print("Creating Java distribution...")
  subprocess.run(["jlink", "--module-path", str(jmodsDirPath), "--add-modules", "java.se",
      "--strip-debug", "--no-man-pages", "--no-header-files", "--compress=2",
      "--output", str(javaTargetDirPath)])

  print("Removing JDK directory...")
  shutil.rmtree(jdkDirPath)

  return relativeJavaDirPathString



def getLtexLsVersion() -> str:
  with open("pom.xml", "r") as file:
    regexMatch = re.search(r"<version>(.*?)</version>", file.read())
    assert regexMatch is not None
    return regexMatch.group(1)



def main() -> None:
  createBinaryArchive("linux", "x64")
  createBinaryArchive("mac", "x64")
  createBinaryArchive("windows", "x64")


if __name__ == "__main__":
  main()
