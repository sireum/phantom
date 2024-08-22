// #Sireum
/*
 Copyright (c) 2017-2024, Jason Belt, Kansas State University
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
import org.sireum.cli.CliOpt._

object cli {

  // the usage field will be placed inside a ST so to get newlines that don't have large
  // indentations we need to nest another ST inside that.
  val tqs: String = "\"\"\""
  val usage : String =
    st"""$${st$tqs
        ||    phantom --update [--osate <path>] [--features <config>] [--sireum-home <path>]
        ||
        ||      Just update/install features
        ||
        ||or: phantom [<options>] [<project-directory>]
        ||
        ||      Generate AIR.  Either:
        ||        - point to a directory containing a .project or .system file, or
        ||        - populate the 'projects', 'main-package', and 'sys-impl' options$tqs.render}""".render

  val phantomTool: Tool = Tool(
    name = "phantom",
    command = "phantom",
    header = "Sireum Phantom: Headless OSATE AADL to AIR Translator",
    usage = usage,
    usageDescOpt = None(),
    description = "",
    opts = ISZ(
      Opt(name = "impl", longKey = "sys-impl", shortKey = Some('s'),
        tpe = Type.Str(sep = None(), default = None()),
        description = "Name of the system implementation."
      ),
      Opt(name = "main", longKey = "main-package", shortKey = Some('a'),
        tpe = Type.Path(multiple = F, default = None()),
        description = "AADL main package file that contains a system implementation."
      ),
      Opt(name = "mode", longKey = "mode", shortKey = Some('m'),
        tpe = Type.Choice(name = "phantomMode", sep = None(), elements = ISZ("json", "json_compact", "msgpack")),
        description = "AADL model serialization method"
      ),
      Opt(name = "output", longKey = "output-file", shortKey = Some('f'),
        tpe = Type.Path(multiple = F, default = None()),
        description = "AIR output file path"),
      Opt(name = "projects", longKey = "projects", shortKey = Some('p'),
        tpe = Type.Path(multiple = T, default = None()),
        description = "OSATE project directories, each must contain an OSATE '.project' file"
      ),
      Opt(name = "verbose", longKey = "verbose", shortKey = Some('v'),
        tpe = Type.Flag(default = F),
        description = "Verbose output"),
      Opt(name = "verbosePlus", longKey = "verbose+", shortKey = None(),
        tpe = Type.Flag(default = F),
        description = "Increased verbose output")
    ),
    groups = ISZ(
      OptGroup(name = "OSATE", opts = ISZ(
        Opt(name = "osate", longKey = "osate", shortKey = Some('o'),
          tpe = Type.Path(multiple = F, default = None()),
          description = "Either the path to an existing installation of OSATE, or the path where OSATE should be installed"
        ),
        Opt(name = "update", longKey = "update", shortKey = Some('u'),
          tpe = Type.Flag(default = F),
          description = "Update (or install) features"
        ),
        Opt(name = "features", longKey = "features", shortKey = None(),
          tpe = Type.Str(sep = Some(';'), default = None()),
          description = "Plugin features to update/install, each of the form <feature-id>=<repo-url-1>,...,<repo-url-N>. Latest Sireum plugins installed if not provided"
        ),
        Opt(name = "version", longKey = "version", shortKey = None(),
          tpe = Type.Str(sep = None(), default = Some("2.14.0-vfinal")),
          description = "OSATE version"
        ),
        Opt(name = "sireum", longKey = "sireum-home", shortKey = None(),
          tpe = Type.Path(multiple = F, default = None()),
          description = "Change the location of the Sireum home installation directory that OSATE uses"
        ),
      ))
    )
  )
}
