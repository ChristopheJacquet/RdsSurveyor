RDS Surveyor, Radio Data System decoder
=======================================

RDS Surveyor is a complete tool for decoding and analyzing [Radio Data System](http://en.wikipedia.org/wiki/Radio_Data_System) (RDS) data. RDS (also known as RBDS in North America) is a communication protocol for embedding streams of digital information in FM radio broadcasts.

RDS Surveyor is Java-based, hence it runs on all platforms. It has been successfully used on Linux, MacOS and Windows.

### Supported hardware

RDS Surveyor can use diverse data sources:

* RTL-SDR USB dongles supported by librtlsdr,
* Si470x-based USB FM tuners (e.g. Silicon Lab's USBFMRADIO),
* GNS FM9 TMC receiver,
* Video4Linux receivers which feature RDS,
* data and clock signals (as output, for example, by a TDA 7330 receiver IC) and sampled by the sound card,
* log files from itself or RDS Spy (both synchronized group data or unsynchronized bitstreams).

### Supported RDS features

The following features of RDS are currently implemented:

* basic features for programme identification: PI, PTY, TP/TA, various flags,
* program service name (PS), including intelligent handling of scrolling PS,
* long program service name (LPS) defined in RDS2,
* alternative frequencies (AF),
* radiotext (RT) and RadiotextPlus (RT+),
* clock time (CT),
* cross-referencing of other networks (EON),
* radio paging (RP),
* Traffic Message Channel (TMC),
* EN301700 (cross-referencing of DAB from RDS),
* RBDS-specific features, e.g. PI-code to callsign decoding.

As of March 2018, it handles only the basic data stream of RDS1, but support for RDS2's additional data streams will be added when broadcasters start using this feature.

### How to use RDS Surveyor?

#### Downloading a compiled executable

#### Compiling

To compile and the main program, you need Java SE 6 or later and the Gradle build system. Go into the `RDSSurveyor` directory and run:

```
gradle jar
```

You get the JAR executable in `build/libs/rdssurveyor.jar`. It is sufficient for playback, clock/data signal input via the sound card and GNS FM9 input.

For other input sources, you need to compile a driver or "JNI Backend". For example, for the Si470x backend, go to `JNIBackends/Si470x` and then run the makefile corresponding to your platform, for instance:

```
makefile -f Makefile.mac
```

You get a shared object file (`.dylib`, `.so`, `.dll`) which you need to provide RDS Surveyor to use the backend.

For the RTL-SDR driver, you need to install `librtlsdr-dev`. Then go to `JNIBackends/rtl2832u`.

#### Running

To read from a log file from RDS Surveyor or RDS Spy:

```
java -jar rdssurveyor.jar -ingrouphexfile <file.rds>
```

To use the Si470x or RTL-SDR backend, you need to reference the shared object file. Examples:

* To input from Si470x on Linux: `java -jar rdssurveyor.jar -intuner si470x.so`.
* To input from RTL-SDR on Mac: `java -jar rdssurveyor.jar -insdr rtl.dylib`.

I advise you to create a directory called `log` to store your receive logs permanently. Then just run the program with `java -Djava.io.tmpdir=log -jar ...`.

### Contributors

* Christophe Jacquet: main program, original developer.
* Michael von Glasow: RTL-SDR and GNS FM9 backends, major improvement of TMC support, various fixes.
* Dominique Matz: Video4Linux backend.