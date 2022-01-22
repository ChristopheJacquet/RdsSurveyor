include ../Makevars.linux

all:
	gcc -Wall $(JAVAINCS) -shared -std=gnu99 -o libv4ltuner.so v4ltuner.c
	cp libv4ltuner.so ../RDSSurveyor

clean:
	rm libv4ltuner.so
