// #Sireum

/*
 Copyright (c) 2017-2021, Kansas State University
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

  @datatype class Feature(val name: String, val id: String, val updateSite: String)
}

import Phantom._

@datatype class Phantom(val osateVersion: String, val osateOpt: Option[Os.Path], val quiet: B, sireumHome: Os.Path) {
  val osateUrlPrefix: String = s"https://osate-build.sei.cmu.edu/download/osate/stable/$osateVersion/products/"
  val osateBundle: String = Os.kind match {
    case Os.Kind.Mac => s"osate2-$osateVersion-macosx.cocoa.x86_64.tar.gz"
    case Os.Kind.Linux => s"osate2-$osateVersion-linux.gtk.x86_64.tar.gz"
    case Os.Kind.Win => s"osate2-$osateVersion-win32.win32.x86_64.zip"
    case _ => halt("Infeasible")
  }

  val osateUrl: String = s"$osateUrlPrefix$osateBundle"

  //val awasFeature: Feature = Feature("AWAS", "org.sireum.aadl.osate.awas.feature.feature.group", "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.awas.update.site")

  val procEnv: ISZ[(String, String)] = ISZ(("PATH", s"${Os.env("PATH").get}${Os.pathSep}${Os.path(Os.env("JAVA_HOME").get) / "bin"}"))

  val phantomDir: Os.Path = osateOpt match {
    case Some(osate) => osate
    case _ => Os.home / ".sireum" / "phantom"
  }

  val osateDir: Os.Path = phantomDir / s"osate-$osateVersion${if (Os.isMac) ".app" else ""}"

  def update(osateExe: Os.Path,
             features: ISZ[Feature]): Z = {

    for (o <- features) {
      if (isInstalled(o.id, osateExe)) {
        addInfo(s"Uninstalling ${o.name} OSATE plugin")
        uninstallPlugin(o.id, osateExe)
      }
    }

    for (o <- ops.ISZOps(features).reverse) {
      if (!isInstalled(o.id, osateExe)) {
        addInfo(s"Installing ${o.name} OSATE plugin from ${o.updateSite}")
        installPlugin(o.id, o.updateSite, osateExe)
      }
    }

    val st: ST =
      st"""Sireum is now available via an ${osateExe.name} CLI.  E.g.:
          |  alias osireum='${getOsateLauncherString(osateExe)} -data @user.home/.sireum -application org.sireum.aadl.osate.cli'
          |
          |  Then invoke osireum to see the command line usage.
          |
          |  The following tools have customized behavior when run from osireum:
          |    - hamr phantom:  some options ignored (e.g. --update, --osate)
          |    - hamr codegen: accepts .project or .system files in addition to serialized AIR files"""

    addInfo(st.render)

    return 0
  }

  def getOsateExe(): Option[Os.Path] = {
    installOsate()

    val brand = "osate"
    val (osateExe, osateIni, javaLoc): (Os.Path, Os.Path, Os.Path) = if (Os.isMac) {
      (osateDir / "Contents" / "MacOS" / brand, osateDir / "Contents" / "Eclipse" / "osate.ini", sireumHome / "bin" / "mac" / "java" / "bin" / "java")
    } else if (Os.isLinux) {
      (osateDir / brand, osateDir / "osate.ini", sireumHome / "bin" / "linux" / "java" / "bin" / "java")
    } else if (Os.isWin) {
      (osateDir / s"$brand.exe", osateDir / "osate.ini", sireumHome / "bin" / "win" / "java" / "bin" / "java.exe")
    } else {
      addError("Phantom only supports macOS, Linux, or Windows")
      return None()
    }

    for (p <- ISZ(osateExe, osateIni, sireumHome / "bin" / "sireum.jar", javaLoc) if !p.exists) {
      addError(s"${p.canon.value} does not exist. This needs to be resolved before proceeding")
      return None()
    }

    var content = ops.ISZOps(osateIni.readLines)

    var pos = content.indexOf("-vm")
    if (pos < content.s.size) { // remove old vm entry
      content = ops.ISZOps(content.slice(0, pos) ++ content.slice(pos + 2, content.s.size))
    }

    def custContains(prefix: String, o: ISZ[String]): Z = {
      for (i <- 0 until o.size if ops.StringOps(o(i)).contains(prefix)) { return i }; return o.size
    }

    pos = custContains("-Dorg.sireum.home=", content.s)
    if (pos < content.s.size) { // remove old sireum location
      content = ops.ISZOps(content.slice(0, pos) ++ content.slice(pos + 1, content.s.size))
    }

    pos = content.indexOf("-vmargs")
    val newContent: ISZ[String] = content.slice(0, pos) ++ ISZ[String]("-vm", javaLoc.canon.value, "-vmargs", s"-Dorg.sireum.home=${sireumHome.canon.value}") ++ (
      if (pos < content.s.size) content.slice(pos + 1, content.s.size) else ISZ[String]())

    osateIni.writeOver(st"${(newContent, "\n")}".render)

    return Some(osateExe)
  }

  def installOsate(): B = {
    if (osateDir.exists && osateDir.isDir) {
      return T
    }
    osateDir.removeAll()
    val osateBundlePath = Os.home / "Downloads" / "sireum" / osateBundle
    osateBundlePath.up.canon.mkdirAll()
    if (!osateBundlePath.exists) {
      addInfo(s"Downloading $osateUrl ...")
      osateBundlePath.downloadFrom(osateUrl)
      println()
    }
    if (Os.isMac) {
      phantomDir.mkdirAll()
      Os.proc(ISZ("tar", "xfz", osateBundlePath.string)).at(phantomDir).runCheck()
      (osateDir.up.canon / "osate2.app").moveTo(osateDir)
    } else {
      osateDir.mkdirAll()
      Os.proc(ISZ("tar", "xfz", osateBundlePath.string)).at(osateDir).runCheck()
    }

    addInfo(s"OSATE $osateVersion installed at $osateDir")

    return osateDir.exists
  }

  def featuresInstalled(features: ISZ[Feature], osateExe: Os.Path): B = {
    return ops.ISZOps(features).forall(f => isInstalled(f.id, osateExe))
  }

  def isInstalled(featureId: String, osateExe: Os.Path): B = {
    val installedPlugins = Os.proc(getOsateLauncher(osateExe) ++ ISZ[String]("-application", "org.eclipse.equinox.p2.director",
      "-listInstalledRoots")).env(procEnv).at(osateExe.up).runCheck()
    return ops.StringOps(installedPlugins.out).contains(featureId)
  }

  def uninstallPlugin(featureId: String, osateExe: Os.Path): Unit = {
    Os.proc(getOsateLauncher(osateExe) ++ ISZ[String]("-application", "org.eclipse.equinox.p2.director",
      "-uninstallIU", featureId
    )).env(procEnv).at(osateExe.up).runCheck()
  }

  def installPlugin(featureId: String, updateSite: String, osateExe: Os.Path): Unit = {
    Os.proc(getOsateLauncher(osateExe) ++ ISZ[String]("-application", "org.eclipse.equinox.p2.director",
      "-repository", updateSite, "-installIU", featureId
    )).env(procEnv).at(osateExe.up).runCheck()
  }

  def execute(osateExe: Os.Path,
              mode: String,
              projects: ISZ[Os.Path],
              main: Option[Os.Path],
              impl: Option[String],
              output: Option[Os.Path],
              projectDir: Option[Os.Path]): Z = {

    def getKey(name: String): String = {
      val cand = phantomTool.opts.filter(f => f.name == name)
      if (cand.isEmpty || cand.size > 1) {
        halt(s"Issue arose when looking up longKey for $name")
      }
      return s"--${cand(0).longKey}"
    }

    // Phantom keys will be used by the OSATE plugin CLI so need to make sure the
    // right long key names are used
    //val update: String = getKey("update")
    val modeKey: String = getKey("mode")
    //val osateKey: String = getKey("osate")
    val projectsKey: String = getKey("projects")
    val mainKey: String = getKey("main")
    val implKey: String = getKey("impl")
    val outputKey: String = getKey("output")

    var args: ISZ[String] = ISZ("hamr", "phantom")

    args = (args :+ modeKey) :+ mode

    // don't pass update or osate options since phantom will be working on osateExe

    if (projects.nonEmpty) {
      args = (args :+ projectsKey) :+ st"${(projects, Os.pathSep)}".render
    }

    if (main.nonEmpty) {
      args = (args :+ mainKey) :+ main.get.value
    }
    if (impl.nonEmpty) {
      args = (args :+ implKey) :+ impl.get
    }
    if (output.nonEmpty) {
      args = (args :+ outputKey) :+ output.get.value
    }

    if (projectDir.nonEmpty) {
      args = args :+ projectDir.get.value
    }

    val procArgs = getOsateLauncher(osateExe) ++
      ISZ[String]("-data", "@user.home/.sireum", "-application", "org.sireum.aadl.osate.cli") ++ args
    //println(st"${(procArgs, " ")}".render)

    val prc = Os.proc(procArgs).env(procEnv).at(osateDir)

    val result: Os.Proc.Result = if (quiet) prc.run() else prc.console.run()

    return result.exitCode
  }

  def getOsateLauncher(osateExe: Os.Path): ISZ[String] = {
    var ret: ISZ[String] = ISZ[String](osateExe.string, "-nosplash", "-console", "-consoleLog", "--launcher.suppressErrors")
      if(Os.isWin) {
        // osate.exe will try to open a new console window when consoleLog is used. This causes headless
        // CI builds to halt/freeze. Instead, run it directly from the launcher
        // TODO: ensure osate.ini is being is used, pass other java system properties??
        val launcherJar = (osateDir / "plugins").list.filter(p => ops.StringOps(p.name).startsWith("org.eclipse.equinox.launcher_") && p.ext == "jar")
        if(launcherJar.size == 1) {
          val javaLoc = sireumHome / "bin" / "win" / "java" / "bin" / "java.exe"
          // launcher does not have '--launcher.suppressErrors option
          ret = ISZ[String](javaLoc.value, s"-Dorg.sireum.home=${sireumHome.value}", "-cp", launcherJar(0).string, "org.eclipse.core.launcher.Main", "-nosplash", "-console", "-consoleLog")
        }
      }
    return ret
  }

  def getOsateLauncherString(osateExe: Os.Path): String = {
    return st"${(getOsateLauncher(osateExe), " ")}".render
  }

  def addInfo(s: String): Unit = {
    if (!quiet) {
      cprintln(F, s)
    }
  }

  def addError(s: String): Unit = {
    cprintln(T, s"Error: $s.")
  }

  def addWarning(s: String): Unit = {
    cprintln(F, s"Warning: $s")
  }
}
