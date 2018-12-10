/*
 Copyright (c) 2018, Jason Belt, Kansas State University
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

{

  import org.sireum._
  import org.sireum.cli.CliOpt._

  val phantomTool: Tool = Tool(
    name = "phantom",
    command = "phantom",
    description = "",
    header = "Sireum Phantom: Headless OSATE AADL to AIR Translator",
    usage = "<option>* <system-name>",
    opts = ISZ(
      Opt (name = "mode", longKey = "mode", shortKey = Some('m'),
        tpe = Type.Choice(name = "mode", sep = None(), elements = ISZ("jsonCompact", "json", "msgpack")),
        description = "Serialization method"
      ),
      Opt (name = "osate", longKey = "osate", shortKey = Some('e'),
        tpe = Type.Path(multiple = F, default = None()),
        description = "OSATE installation path"
      ),
      Opt (name = "projects", longKey = "projects", shortKey = Some('p'),
        tpe = Type.Path(multiple = T, default = Some(".")),
        description = "OSATE project folders"
      ),
      Opt (name = "main", longKey = "main-package", shortKey = Some('a'),
        tpe = Type.Str(sep = None(), default = None()),
        description = "AADL main package file"
      ),
      Opt (name = "output", longKey = "output", shortKey = Some('o'),
        tpe = Type.Path(multiple = F, default = None()),
        description = "AIR output file path")
    ),
    groups = ISZ()
  )

  phantomTool
}
