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
$ make docs # For Linux
```
```sh
$ javadoc -private -splitindex -d ./docs/javadoc ./server/TCPServer.java ./server/Main.java ./client/TCPClient.java ./client/Main.java ./lib/Protocol.java # For Windows
```

From there, a `docs/javadoc` directory will be created. You can then open `docs/javadoc/index.html` to browse the
documentation.

## Server

The server is a single-threaded TCP server that evaluates mathematical expressions. All the server code is located in
the `server` directory.

The server may be started with this command:

```sh
$ make start-server # For Linux
```
```sh
$ javac ./server/Main.java ; java server.Main # For Windows
```

## Client

The client is a simple TCP client that sends mathematical expressions to be evaluated to the server and displays the
server's response. All the client code is located in the `client` directory.

The client may be started with this command:

```sh
$ make start-client # For Linux
```
```sh
$ javac ./client/Main.java ; java client.Main # For Windows
```

## Logger

Logs are generated using the `java.util.logging` class, which will produce a XML file in the base folder named `TCPServer.log`. Each logging event will store the time of the event in the entry, and the log is persistent between executions of the `TCPServer`, with new entries being appended to the same log file.

The following events are logged with timestamps:
* Client joins - Logs client's name, IP address
* Client disconnects - Logs client's name and total time connected
* Client command received - Logs client's name, the command, and the response to the command
* Server startup
* Server shutdown - For each client, logs client's name and the total time connected
* Errors - Logs certain types of error and the relevant exception


## Protocol

To send messages, the server and client call the `marshal` method from the `Protocol` class, which accepts a `Map<String, String>` (which holds the parameters for the message) and returns a `String` which the client or server sends over the network.

To receive messages, the server and client read a newline-terminated `String` and call the `unmarshal` method from the `Protocol` class, which accepts a `String` and returns a `Map<String, String>`.

The `marshal` method converts the `Map<String, String>` into a `String` by URL encoding the key and value with the UTF-8 charset, separating the key and its value with an `=`, then separating key-value pairs with an `&`. Lastly, it ends the `String` with a newline so it knows when to stop reading on the socket. The unmarshal method simply works in reverse to convert a `String` back into `Map<String, String>`.

### Message Format
The `TCPClient` and `TCPServer` classes have message builder functions that take in parameters which are put in a `Map<String, String>` and returned as a marshalled `String` that the client or server can send over the network.

The client’s messaging format:
* Only `cmd`, `name`, and `eq` are valid `String` keys.
* The `String` key `cmd` is mandatory. The `String` key `name` is included if `cmd`’s value is `hello` or `exit`. The `String` key `eq` is included if `cmd`’s value is `math`.
* For `cmd`, the valid values are `hello`, `math`, and `exit`.
* For `name`, any valid UTF-8 string is accepted.
* For `eq`, any valid UTF-8 string is accepted, and is validated server-side.
Additionally, the client’s raw input is required to be less than 1024 bytes in UTF-8. This is validated client-side and this restriction is in place just to ensure the server’s buffer of size 2048 does not overflow.

The server’s message format:
* `resp` is the only valid `String` key.
* For the client command `hello`, the value for `resp` will be `Hello, <name>`. The response is validated client-side and is considered an ACK if it validates correctly. This command establishes the connection between the client and server.
* For the client command `exit`, the value for `resp` will be `Bye, <name>`. The response is validated client-side and is considered an ACK if it validates correctly. This command terminates the connection between the client and server.
* For client command `math`, the value for `resp` will be either the answer to the equation from the client or an error message.
