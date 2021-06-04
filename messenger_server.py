from concurrent import futures
import logging

import grpc

import messenger_pb2
import messenger_pb2_grpc


class Messenger(messenger_pb2_grpc.MessengerServicer):

    def sayMessage(self, request, context):
        logging.info(f'"{request.message}" from {request.whoami}')
        return messenger_pb2.MessengerReply()


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    messenger_pb2_grpc.add_MessengerServicer_to_server(Messenger(), server)
    port = "50052"
    server.add_insecure_port(f"0.0.0.0:{port}")
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    serve()
