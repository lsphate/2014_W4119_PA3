target:
	mkdir -p classes
	javac ./src/RUDPPacket.java -d ./classes
	javac ./src/Linkage.java -d ./classes
	javac ./src/RouteInfo.java -d ./classes
	javac ./src/LinkInfo.java -d ./classes -classpath ./classes
	javac ./src/RouteTable.java -d ./classes -classpath ./classes
	javac ./src/DistanceVector.java -d ./classes -classpath ./classes
	javac ./src/bfclient.java -d ./classes -classpath ./classes

clean:
	rm -f ./classes/RUDPPacket.class
	rm -f ./classes/Linkage.class
	rm -f ./classes/RouteInfo.class
	rm -f ./classes/LinkInfo.class
	rm -f ./classes/DistanceVector.class
	rm -f ./classes/RouteTable.class
	rm -f ./classes/bfclient.class
	rm -rf ./classes