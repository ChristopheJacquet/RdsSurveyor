Si470x userspace driver
=======================

Copyright (c) 2012 by Christophe Jacquet
Realeased under the GNU GPL v2

It compiles on MacOS X, Linux and Windows.

Quick instructions:
1) In the "hidapi" directory, go into the subdirectory corresponding to your platform and run "make" (this step is not required on Linux)
2) At the top level, run "make -f Makefile.<your plaform>"

Platform-specific notes
-----------------------

* Linux:

You may need to install the following packages:
 - libhidapi-dev
 - libusb-1.0-0-dev
 - libudev-dev

In order to use this driver with a non-root account, you need to supply the appropriate rules for udev.
To do this, copy 40-si470x-libusb.rules to /lib/udev/rules.d.


* Windows

The driver compiles under mingw.