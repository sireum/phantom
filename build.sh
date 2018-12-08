#!/bin/bash -e
export SCRIPT_HOME=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
. ${SCRIPT_HOME}/prelude.sh
cd ${SCRIPT_HOME}
${SCRIPT_HOME}/mill-standalone all \
  cli.assembly \
  cli.tests
if [ -n "$COMSPEC" -a -x "$COMSPEC" ]; then
  cp out/cli/assembly/dest/out.jar phantom.bat
else
  cp out/cli/assembly/dest/out.jar phantom
  chmod +x phantom
fi
