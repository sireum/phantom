// #Sireum

/*
 Copyright (c) 2018, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sireum.hamr.phantom

import org.sireum._
import org.sireum.hamr.phantom.cli.phantomTool

object Phantom {

  val osateVersion: String = "2.9.0-vfinal"
  val osateUrlPrefix: String = s"https://osate-build.sei.cmu.edu/download/osate/stable/$osateVersion/products/osate2-$osateVersion"
  val osateLinuxUrl: String = s"$osateUrlPrefix-linux.gtk.x86_64.tar.gz"
  val osateMacUrl: String = s"$osateUrlPrefix-macosx.cocoa.x86_64.tar.gz"
  val osateWinUrl: String = s"$osateUrlPrefix-win32.win32.x86_64.zip"
  val sireumUpdateSite: String = "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.update.site"
  val cliUpdateSite: String =    "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.cli.update.site"
  val sireumFeatureID: String = "org.sireum.aadl.osate.feature.feature.group"
  val cliFeatureID: String = "org.sireum.aadl.osate.cli.feature.feature.group"

  def phantomDir: Os.Path = { Os.home / ".sireum" / "phantom" }
  def defaultOsateDir: Os.Path = { phantomDir / s"osate-$osateVersion" }

  def run(mode: String,
          osate: Option[Os.Path],
          projects: ISZ[Os.Path],
          main: Option[String],
          impl: Option[String],
          output: Option[String],
          projectDir: Option[Os.Path]): Z = {

    val osateDir: Os.Path = osate match {
      case Some(d) => d
      case _ =>
        if(!installOsate()) { return -1 }
        defaultOsateDir
    }

    val osateExe: Os.Path = if (Os.isMac) {
      osateDir / "MacOS" / "osate"
    } else if (Os.isLinux) {
      osateDir / "osate"
    } else if (Os.isWin) {
      osateDir / "osate.exe"
    } else {
      addError("Phantom only supports macOS, Linux, or Windows")
      return -1
    }

    if(!osateExe.exists) {
      addError(s"$osateExe does not exist")
      return -1
    }

    val installedPlugins = Os.proc(ISZ(
      osateExe.string,
      "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-listInstalledRoots")).at(osateDir).runCheck()

    val isInstalled = ops.StringOps(installedPlugins.out).contains(cliFeatureID)
    if(!isInstalled) {
      installPlugins(osateExe)
    }

    execute(osateExe, mode, projects, main, impl, output, projectDir)

    return 0
  }

  def installOsate(): B = {
    if (defaultOsateDir.exists && defaultOsateDir.isDir) {
      return true
    }
    defaultOsateDir.removeAll()
    val osateBundle = Os.tempFix("osate", "bundle")
    osateBundle.removeAll()
    if (Os.isMac) {
      println(s"Downloading OSATE ${osateVersion}...")
      phantomDir.mkdirAll()
      osateBundle.downloadFrom(osateMacUrl)
      Os.proc(ISZ("tar", "xfz", osateBundle.string)).at(phantomDir).console.runCheck()
      (phantomDir / "osate2.app" / "Contents").moveTo(defaultOsateDir)
      (phantomDir / "osate2.app").removeAll()
    } else if (Os.isLinux) {
      println(s"Downloading OSATE ${osateVersion}...")
      defaultOsateDir.mkdirAll()
      osateBundle.downloadFrom(osateLinuxUrl)
      Os.proc(ISZ("tar", "xfz", osateBundle.string)).at(defaultOsateDir).console.runCheck()
    } else if (Os.isWin) {
      println(s"Downloading OSATE ${osateVersion}...")
      defaultOsateDir.mkdirAll()
      osateBundle.downloadFrom(osateWinUrl)
      osateBundle.unzipTo(defaultOsateDir)
    } else {
      addError("Phantom only supports macOS, Linux, or Windows")
      return false
    }
    osateBundle.removeAll()
    return true
  }

  def installPlugins(osateExe : Os.Path): Unit = {
    Os.proc(ISZ(osateExe.string, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-repository", sireumUpdateSite, "-installIU", sireumFeatureID
    )).at(defaultOsateDir).console.runCheck()

    Os.proc(ISZ(osateExe.string, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-repository", cliUpdateSite, "-installIU", cliFeatureID
    )).at(defaultOsateDir).console.runCheck()
  }

  def execute(osateExe : Os.Path,
              mode: String,
              projects : ISZ[Os.Path],
              main: Option[String],
              impl: Option[String],
              output: Option[String],
              projectDir: Option[Os.Path]): Unit = {


    def getKey(name: String): String = {
      val cand = phantomTool.opts.filter(f => f.name == name)
      if (cand.isEmpty || cand.size > 1) { halt(s"Issue arose when looking up longKey for ${name}") }
      return cand(0).longKey
    }

    // Phantom keys will be used by the OSATE plugin CLI so need to make sure the
    // right long key names are used
    val modeKey: String = getKey("mode")
    val osateKey: String = getKey("osate")
    val projectsKey: String = getKey("projects")
    val mainKey: String = getKey("main")
    val implKey: String = getKey("impl")
    val outputKey: String = getKey("output")

    var args = ISZ(modeKey, mode)

    // don't pass osate option since phantom will be working on osateExe

    if(projects.nonEmpty) {
      args = (args :+ projectsKey) :+ ops.ISZOps(projects)
        .foldLeft((s: String, p: Os.Path) => s"$s${Os.pathSep}${p.value}", "")
    }

    if(main.nonEmpty) { args = (args :+ mainKey) :+ main.get }
    if(impl.nonEmpty) { args = (args :+ implKey) :+ impl.get }
    if(output.nonEmpty) { args = (args :+ outputKey) :+ output.get }

    if(projectDir.nonEmpty) { args = args :+ projectDir.get.value }

    Os.proc(ISZ[String](
      osateExe.string,
      "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.sireum.aadl.osate.cli") ++ args).at(defaultOsateDir).console.runCheck()
  }

  def addError(s: String): Unit = { cprintln(T, s"Error: $s. Pass '-h' for usage info.") }
  def addWarning(s: String): Unit = { cprintln(F, s"Warning: $s") }
}
