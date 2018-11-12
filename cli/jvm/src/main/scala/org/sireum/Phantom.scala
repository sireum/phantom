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
    case Some(o: Cli.PhantomOption) => phantom(o)
    case _ =>
  }

  def phantom(o: Cli.PhantomOption): Unit = {
    // TODO: check CLI args


    val outMode = o.mode

    val osateDir = o.osate match {
      case SSome(d) => Path(new File(d.value).getAbsolutePath)
      case _ => installOsate(); defaultOsateDir
    }

    assert(o.projects.size>0, "Must provide a project folder path")
    val projects = o.projects.map { it =>
          Path(new File(it.value))
    }

    val mainPackage : String = if(o.main.nonEmpty) {
      o.main.get
    } else {
      projects(0).toIO.list().head
    }

    assert(o.args.size == 1, "Must provice exactly one system implementation")
    val impl : String = o.args(0)


    val outExt = if(o.mode == Mode.Json) String(".json")  else String(".msgpack")

    val outDir: Path = if(o.output.nonEmpty) {
      Path(new File(o.output.get.value))
    } else {
      Path(projects(0) + impl.value + outExt)
    }




    val osateExe = if(scala.util.Properties.isMac) {
      defaultOsateDir / 'MacOS
    } else if(scala.util.Properties.isLinux) {
      defaultOsateDir
    } else {
      defaultOsateDir
    }

    val installedPlugins : CommandResult = %%(osateExe / 'osate, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-listInstalledRoots")(defaultOsateDir)
    val isInstalled = installedPlugins.out.lines.exists(
      _.startsWith(cliFeatureID))

    if(!isInstalled) {
      installPlugins(osateExe)
    }

    execute(osateExe, projects(0), mainPackage, impl, outDir)
  }

  def installOsate(): Unit = {
    if (exists ! defaultOsateDir && defaultOsateDir.isDir) {
      return
    }
    rm ! defaultOsateDir
    val osateBundle = "osate.bundle"
    if (scala.util.Properties.isMac) {
      println("Downloading OSATE2 ...")
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
      Console.err.println("Phantom only supports macOS, Linux, or Windows")
      System.exit(-1)
    }
  }

  def installPlugins(osateExe : Path):Unit = {
    %(osateExe / 'osate, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-repository", sireumUpdateSite, "-installIU", sireumFeatureID
    )(defaultOsateDir)

    %(osateExe / 'osate, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.eclipse.equinox.p2.director",
      "-repository", cliUpdateSite, "-installIU", cliFeatureID
    )(defaultOsateDir)
  }

  def execute(osateExe : Path, project : Path, mainPackage: String, impl: String, out: Path):Unit = {
    %(osateExe / 'osate, "-nosplash",
      "-console",
      "-consoleLog",
      "-application",
      "org.sireum.aadl.osate.cli",
      project.toString(), mainPackage.toString(), impl.value, out.toString()
    )(defaultOsateDir)

  }

}
