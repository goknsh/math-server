```
                __  .__
  _____ _____ _/  |_|  |__             ______ ______________  __ ___________
 /     \\__  \\   __\  |  \   ______  /  ___// __ \_  __ \  \/ // __ \_  __ \
|  Y Y  \/ __ \|  | |   Y  \ /_____/  \___ \\  ___/|  | \/\   /\  ___/|  | \/
|__|_|  (____  /__| |___|  /         /____  >\___  >__|    \_/  \___  >__|
      \/     \/          \/               \/     \/                 \/
```

A single-threaded server that evaluates mathematical expressions.

## Requirements

* [Java SE Development Kit 8u211](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html)
* [Java SE Runtime Environment 8u251](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html)

## Docs

The code has comments throughout. Additionally, we use JavaDoc to document the classes and functions. You can generate
the JavaDoc with this command:

```sh
$ make docs
```

From there, a `docs/javadoc` directory will be created. You can then open `docs/javadoc/index.html` to browse the
documentation.

## Server

The server is a single-threaded TCP server that evaluates mathematical expressions. All the server code is located in
the `server` directory.

The server may be started with this command:

```sh
$ make start-server
```

## Client

The client is a simple TCP client that sends mathematical expressions to be evaluated to the server and displays the
server's response. All the client code is located in the `client` directory.

The client may be started with this command:

```sh
$ make start-client
```

## Logger

Yet to be implemented.

## Protocol

The protocol simply URL encodes a `Map` of `String`s into a newline-terminated string to send messages to and from the
client and server. Each message from the client or server must be marshalled into a newline-terminated URL encoded
string and each message will be unmarshalled into a `Map` of `String`s by the client or server to understand and
manipulate the message. Note that each type of request will have their own builder functions located in classes where
they are deemed to be needed.
