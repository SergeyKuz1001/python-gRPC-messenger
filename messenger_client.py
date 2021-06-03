from concurrent import futures
import logging
import os

import grpc

import messenger_pb2
import messenger_pb2_grpc


def run():
    host = os.getenv("MESSENGER_SERVER", "localhost")
    port = "50051"
    channel = grpc.insecure_channel(f"{host}:{port}")
    client = messenger_pb2_grpc.MessengerStub(channel)
    client_name = input("Enter your name: ")
    while True:
        message = input()
        request = messenger_pb2.MessengerMessage(whoami = client_name, message = message)
        client.sayMessage(request)


if __name__ == '__main__':
    logging.basicConfig(filename="messages.log", level=logging.INFO)
    run()
