A. Brief description

	My program contains several classes, RUDPPacket for packing sending data, Linkage, LinkInfo, DistanceVector, RouteInfo, RouteTable for data structures, and bfclient for main process.
	Each bfclient will invoke 2 threads, one for listening to local port, and another for broadcasting route update.

B. Details on development environment

	It is hard to install JAVA 1.6 on Mac OSX 10.9 Maverick since Apple has it’s own JAVA version update control, so I use JAVA 1.7.0_67 to build my program. I’ve worked very carefully to avoid any API that has not been involved in JDK 1.6, but available in 1.7. And I’ve also test my code on a 1.6 machine as well.
	I used Netbeans 8.0.1 as my IDE, Mac/ubuntu Terminal and Windows CMD for run and test the JAVA program.

C. Instructions on how to run your code

	Just simply type make, and the .class file should be placed in ./classes directory. The runnable .class file is receiver.class and sender.class
	Use "java bfclient <listen_port> <timeout> [<linkage_ip> <linkage_port> <linkage_name>]” to invoke a client application.
	
	In application, use:
	 - linkdown <linkage_ip> <linkage_port> to cut down a linkage.
	 - linkup <linkage_ip> <linkage_port> to restore it.
	 - showrt to expose route table.
	 - close to terminate application.
	Any lack of arguments will receive exceptions and terminate the processes.

D. More

	According to the instruction, the implementation of window size is optional, so I didn't implement that. This is a simple send-and-wait mechanism, the window size is default to 1 and cannot be modified.