Si470x userspace driver
=======================

Copyright (c) 2012 by Christophe Jacquet
Realeased under the GNU GPL v2

It compiles on MacOS X, Linux and Windows.

Quick instructions:
1) In the "hidapi" directory, go into the subdirectory corresponding to your platform ("libusb" for Linux) and run "make"
2) At the top level, run "make -f Makefile.<your plaform>"

Platform-specific notes
-----------------------

* Linux:

You may need to install the following packages:
 - libusb-1.0-0-dev
 - libudev-dev


* Windows

The driver compiles under mingw.