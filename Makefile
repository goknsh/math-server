compiler = javac
runner = java

.PHONY: clean docs start-server start-client

build-server:
	$(compiler) ./server/Main.java

build-client:
	$(compiler) ./client/Main.java

start-server: build-server
	$(runner) server.Main

start-client: build-client
	$(runner) client.Main

docs:
	javadoc -private -splitindex -d ./docs/javadoc ./server/TCPServer.java ./server/Main.java ./client/TCPClient.java ./client/Main.java ./lib/Protocol.java

default: build-server build-client docs
all: build-server build-client docs

clean:
	rm -rf ./**/*.class *.class
