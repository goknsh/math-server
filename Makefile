compiler = javac
runner = java

.PHONY: clean docs start-server start-client

build-server:
	$(compiler) ./client/Main.java

build-client:
	$(compiler) ./server/Main.java

start-server: build-server
	$(runner) client.Main

start-client: build-client
	$(runner) server.Main

docs:
	javadoc -private -splitindex -d ./docs/javadoc ./server/TCPServer.java ./server/Main.java ./client/TCPClient.java ./client/Main.java ./lib/Protocol.java

clean:
	rm -rf *.class
