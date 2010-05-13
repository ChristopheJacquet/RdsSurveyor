all:
	gcc -Wall -I /usr/lib/jvm/java-6-openjdk/include/ -I /usr/lib/jvm/java-6-sun/include/linux/ -shared -std=gnu99 -o libv4ltuner.so v4ltuner.c
	cp libv4ltuner.so ../RDSSurveyor

clean:
	rm libv4ltuner.so