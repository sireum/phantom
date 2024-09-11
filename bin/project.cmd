::/*#! 2> /dev/null                                   #
@ 2>/dev/null # 2>nul & echo off & goto BOF           #
if [ -z ${SIREUM_HOME} ]; then                        #
  echo "Please set SIREUM_HOME env var"               #
  exit -1                                             #
fi                                                    #
exec "${SIREUM_HOME}/bin/sireum" slang run "$0" "$@"  #
:BOF
setlocal
if not defined SIREUM_HOME (
  echo Please set SIREUM_HOME env var
  exit /B -1
)
"%SIREUM_HOME%\bin\sireum.bat" slang run "%0" %*
exit /B %errorlevel%
::!#*/
// #Sireum

import org.sireum._
import org.sireum.project.ProjectUtil._
import org.sireum.project.Project

val library = "library"

val phantom = "hamr-phantom"

val homeDir = Os.slashDir.up.canon

val phantomJvm = moduleJvmPub(
  id = phantom,
  baseDir = homeDir,
  jvmDeps = ISZ(library),
  jvmIvyDeps = ISZ(),
  pubOpt = pub(
    desc = "HAMR Phantom Headless AADL Tool",
    url = "github.com/sireum/phantom",
    licenses = bsd2,
    devs = ISZ(jasonBelt, thari)
  )
)

val project = Project.empty + phantomJvm

projectCli(Os.cliArgs, project)