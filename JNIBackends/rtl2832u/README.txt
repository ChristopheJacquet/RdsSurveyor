Realtek RTL2832U userspace driver
=================================

Copyright (c) 2015 by Michael von Glasow
Released under the GNU GPL v2

It compiles on Linux, Windows and macOS.

Quick instructions:
1) At the top level, run "make -f Makefile.<your plaform>"

Platform-specific notes
-----------------------

* Linux:

You may need to install the following packages:
 - libusb-1.0-0-dev
 - libudev-dev
 - librtlsdr-dev


* Windows

The driver compiles under mingw.


* macOS

You can install the dependencies using Homebrew:
 - brew install rtl-sdr
 - brew install pkg-config

