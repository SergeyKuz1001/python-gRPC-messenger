# GRPC-Chat

GRPC-chat is a basic cross-platform p2p messenger, which is implemented as a part of SPbU Software Architecture course.

## *How to run?*

1)  Build docker-container:

```
$ docker build -t python-grpc .
```

2)  Create network:

```
$ docker network create messenger-nwk
```

3)  Run server:

```
$ docker run --rm -it -p 127.0.0.1:50051:50051/tcp --network messenger-nwk --name messenger-server python-grpc python messenger_server.py
```

4)  Run client:

```
$ docker run --rm -it --network messenger-nwk -e MESSENGER_SERVER=messenger-server python-grpc python messenger_client.py
```

## *Android client*

### Usage

1) Install [the client](./client.apk)
2) Launch server on pc and get *server hostname*
3) Send messages from text field

