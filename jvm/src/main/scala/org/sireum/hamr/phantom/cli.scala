// #Sireum
/*
 Copyright (c) 2017-2021, Jason Belt, Kansas State University
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
        ||    phantom --update [--osate <path>] [--properties <path>]
        ||
        ||      Just update/install Sireum OSATE plugins
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
        tpe = Type.Choice(name = "phantomMode", sep = None(), elements = ISZ("json", "msgpack")),
        description = "Serialization method"
      ),
      Opt(name = "output", longKey = "output-file", shortKey = Some('f'),
        tpe = Type.Path(multiple = F, default = None()),
        description = "AIR output file path"),
      Opt(name = "projects", longKey = "projects", shortKey = Some('p'),
        tpe = Type.Path(multiple = T, default = None()),
        description = "OSATE project directories, each must contain an OSATE '.project' file"
      ),
      Opt(name = "quiet", longKey = "quiet", shortKey = Some('q'),
        tpe = Type.Flag(default = F),
        description = "Do not print informational messages"),
    ),
    groups = ISZ(
      OptGroup(name = "OSATE", opts = ISZ(
        Opt(name = "osate", longKey = "osate", shortKey = Some('o'),
          tpe = Type.Path(multiple = F, default = None()),
          description = "Existing OSATE installation path, otherwise an internal version of OSATE will be used"
        ),
        Opt(name = "update", longKey = "update", shortKey = Some('u'),
          tpe = Type.Flag(default = F),
          description = "Update (or install) Sireum OSATE plugins"
        ),
        Opt(name = "features", longKey = "features", shortKey = None(),
          tpe = Type.Str(sep = Some(';'), default = None()),
          description = "Plugin features to update/install, each of the form <feature-id>=<repo-url-1>,...,<repo-url-N>"
        ),
        Opt(name = "version", longKey = "version", shortKey = Some('v'),
          tpe = Type.Str(sep = None(), default = Some("2.9.2-vfinal")),
          description = "OSATE version"
        ),
      ))
    )
  )
}
