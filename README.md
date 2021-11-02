```
                __  .__
  _____ _____ _/  |_|  |__             ______ ______________  __ ___________
 /     \\__  \\   __\  |  \   ______  /  ___// __ \_  __ \  \/ // __ \_  __ \
|  Y Y  \/ __ \|  | |   Y  \ /_____/  \___ \\  ___/|  | \/\   /\  ___/|  | \/
|__|_|  (____  /__| |___|  /         /____  >\___  >__|    \_/  \___  >__|
      \/     \/          \/               \/     \/                 \/
```
A multi-threaded server that evaluates mathematical expressions.

## Requirements
* JDK 14.0.2

## Server
The server is a multi-threaded TCP server that evaluates mathematical expressions. All the server code is located in the `server` directory.

The server may be started with this command from the root of this project:
```sh
$ javac .\server\Main.java ; java server.Main
```

## Client
The client a simple TCP sends mathematical expressions to be evaluated and displays the server's response. All the client code is located in the `client` directory.

The client may be started with this command from the root of this project:
```sh
$ javac .\client\Main.java ; java client.Main
```

## Logger
Yet to be implemented.

## Protocol
The protocol simply URL encodes a `Map` of `Strings` to send messages from the client and server. Each message from the client or server must be marshalled into a URL encoded string and each message will be unmarshalled into a `Map` of `Strings` by the client or server to understand and manipulate the message. Note that each type of request will have their own builder functions located in classes where they are deemed to be needed.
