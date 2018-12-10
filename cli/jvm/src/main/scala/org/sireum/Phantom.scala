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

package org.sireum

import java.io.File

import ammonite.ops._
import org.sireum.Cli.Mode
import org.sireum.{Some => SSome}

object Phantom extends scala.App {

  val osateVersion = "2.3.5-vfinal"
  val osateUrlPrefix = s"https://osate-build.sei.cmu.edu/download/osate/stable/$osateVersion/products/osate2-$osateVersion"
  val osateLinuxUrl = s"$osateUrlPrefix-linux.gtk.x86_64.tar.gz"
  val osateMacUrl = s"$osateUrlPrefix-macosx.cocoa.x86_64.tar.gz"
  val osateWinUrl = s"$osateUrlPrefix-win32.win32.x86_64.zip"
  val phantomDir = Path(new File(System.getProperty("user.home"), s".sireum/phantom").getAbsolutePath)
  val sireumUpdateSite = "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.update.site/site.xml"
  val cliUpdateSite = "https://raw.githubusercontent.com/sireum/osate-plugin-update-site/master/org.sireum.aadl.osate.cli.update.site/site.xml"
  val sireumFeatureID = "org.sireum.aadl.osate.feature.feature.group"
  val cliFeatureID = "org.sireum.aadl.osate.cli.feature.feature.group"
  val defaultOsateDir = phantomDir / s"osate-$osateVersion"

  Cli(File.pathSeparatorChar).parsePhantom(ISZ(args.toSeq.map(s => s: String):_ *), 0) match {
    case Some(o: Cli.PhantomOption) => System.exit(phantom(o))
    case _ =>
      println(Cli(File.pathSeparatorChar).parsePhantom(ISZ(""), 1).get.asInstanceOf[Cli.PhantomOption].help)
      System.exit(-1)
  }

  def phantom(o: Cli.PhantomOption): Int = {
    o.args.size match {
      case z"0" => println(o.help); return 0
      case z"1" =>
      case _ =>
        addError("Too many arguments provided. Expecting a single system implementation")
        return -1
    }

    val projects = o.projects.map { it =>
      val f = new File(it.value)
      if(!f.exists()) {
        addError(s"${it.value} is not a valid directory")
        return -1
      }
      Path(f.getAbsolutePath)
    }

    val mainPackage : String = if(o.main.nonEmpty) {
      o.main.get
    } else {
      projects(0).toIO.list().head
    }

    val (serializeType, outExt) = o.mode match {
      case Mode.JsonCompact => (o.mode.toString, ".json")
      case _ =>
        addWarning("Currently only JSON Compact is supported.  Using that instead.")
        (Mode.JsonCompact.toString, ".json")
    }

    val impl : String = o.args(0)

    val outFile: Path = if(o.output.nonEmpty) {
      val f = new File(o.output.get.value)
      if(f.exists() && f.isDirectory) {
        addError(s"${f.getAbsolutePath} is a directory.  Should be the name for the generated ${serializeType} file.")
        return -1
      }
      Path(f)
    } else {
      Path(projects(0) + impl.value + outExt)
    }

     val osateDir = o.osate match {
      case SSome(d) => Path(new File(d.value).getAbsolutePath)
      case _ =>
        if(!installOsate()) { return -1 }
        defaultOsateDir
    }

    val osateExe: Path = if(scala.util.Properties.isMac) {
      osateDir / 'MacOS / 'osate
    } else if(scala.util.Properties.isLinux) {
      osateDir / 'osate
    } else if(scala.util.Properties.isWin) {
      osateDir / "osate.exe"
    } else {
      addError("Phantom only supports macOS, Linux, or Windows")
      return -1
    }

    if(!osateExe.toIO.exists){
      Console.err.println(s"${osateExe.toIO.getAbsolutePath} does not exist")
      return -1
    }

    val installedPlugins : CommandResult = %%(osateExe, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-listInstalledRoots")(osateDir)

    val isInstalled = installedPlugins.out.lines.exists(_.startsWith(cliFeatureID))

    if(!isInstalled) {
      installPlugins(osateExe)
    }

    execute(osateExe, projects(0), mainPackage, impl, serializeType, outFile)

    0
  }

  def installOsate(): Boolean = {
    if (exists ! defaultOsateDir && defaultOsateDir.isDir) {
      return true
    }
    rm ! defaultOsateDir
    val osateBundle = "osate.bundle"
    if (scala.util.Properties.isMac) {
      println("Downloading OSATE2 ...")
      mkdir ! phantomDir
      %('curl, "-Lo", osateBundle, osateMacUrl)(phantomDir)
      %('tar, 'xfz, osateBundle)(phantomDir)
      mv(phantomDir / "osate2.app" / 'Contents, defaultOsateDir)
      rm ! phantomDir / "osate2.app"
      rm ! phantomDir / osateBundle
    } else if (scala.util.Properties.isLinux) {
      println("Downloading OSATE2 ...")
      mkdir ! defaultOsateDir
      %('curl, "-Lo", osateBundle, osateLinuxUrl)(defaultOsateDir)
      %('tar, 'xfz, osateBundle)(defaultOsateDir)
      rm ! defaultOsateDir / osateBundle
    } else if (scala.util.Properties.isWin) {
      println("Downloading OSATE2 ...")
      mkdir ! defaultOsateDir
      %('curl, "-Lo", osateBundle, osateWinUrl)(defaultOsateDir)
      %('unzip, "-q", osateBundle)(defaultOsateDir)
      rm ! defaultOsateDir / osateBundle
    } else {
      addError("Phantom only supports macOS, Linux, or Windows")
      return false
    }
    return true
  }

  def installPlugins(osateExe : Path): Unit = {
    %(osateExe, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-repository", sireumUpdateSite, "-installIU", sireumFeatureID
    )(defaultOsateDir)

    %(osateExe, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-repository", cliUpdateSite, "-installIU", cliFeatureID
    )(defaultOsateDir)
  }

  def execute(osateExe : Path, project : Path, mainPackage: String, impl: String, serializeType: String, out: Path): Unit = {
    %(osateExe, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.sireum.aadl.osate.cli",
      project.toString(), mainPackage.toString(), impl.value, out.toString()
    )(defaultOsateDir)
  }

  def addError(s: String): Unit = { Console.err.println(s"Error: ${s}") }
  def addWarning(s: String): Unit = { Console.out.println(s"Warning: ${s}") }
}
