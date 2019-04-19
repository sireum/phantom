# Phantom -- OSATE Headless

Phantom is a CLI-based tool that produces AADL instance models represented in [AIR](https://github.com/sireum/air).
The AIR models can then be used in 
downstream tools such as [AWAS](https://github.com/sireum/v3-awas) or [ACT](https://github.com/sireum/act-plugin).

## Prerequisites

Java version 10 or older

## Installation

Execute the following commands to build Phantom

```bash
git clone https://github.com/sireum/kekinian.git
cd kekinian
bin/build.cmd
```
   
## Usage

Type `bin/sireum aadl phantom` (or `bin\sireum.bat aadl phantom` under Windows) to view Phantom's command line options

## Known Issues

Java 11 is not supported by OSATE version 2.3.6 and below.  Refer to the [OSATE release notes](http://osate.org/osate-releases.html) for the current support status.
