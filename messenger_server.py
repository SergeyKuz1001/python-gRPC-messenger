from concurrent import futures
import logging
import grpc
import threading
import datetime
from shared_proto import messenger_pb2
from shared_proto import messenger_pb2_grpc
import os
import PySimpleGUI as sg


from chat_window import ChatWindow
from message import Message


class Server(messenger_pb2_grpc.MessengerServicer):
    def __init__(self, name):
        self.name = name
        self.connected = False
        self.client_name = None
        self.messages = []
        self.main_window = None
        self.stop_event = None

        self.serve()

    def startMessaging(self, request, context):
        """ receives client name and starts chat window """
        logging.info(f'got name "{request.name}"')
        if not self.connected:
            self.client_name = request.name
            self.connected = True

            self.main_window = ChatWindow(self, 'Server')
            return messenger_pb2.MessengerNameResponse(name=self.name, connected=True)

        else:
            return messenger_pb2.MessengerNameResponse(connected=False)

    def stopMessaging(self, request, context):
        """ disconnects from client and stops server """
        logging.info('disconnected')
        self.connected = False
        self.stop_event.set()
        return messenger_pb2.Empty()
        
    def sendMessage(self, request_iterator, context):
        """ receives messages from gui and send them """
        while self.connected:
            mes = self.main_window.processing()
            if mes is None:
                    self.main_window.window.close()
                    self.stop_event.set()
            self.main_window.print(Message(mes, self.name, datetime.datetime.now(), 'server'))
            self.messages.append(Message(mes, self.name, datetime.datetime.now(), 'server'))
            yield messenger_pb2.MessengerMessage(message=mes)

    def getMessage(self, request, context):
        """ receives messages from client """
        logging.info(f'got message "{request.message}" from {self.client_name}')
        self.main_window.print(Message(request.message, self.client_name, datetime.datetime.now(), 'client'))
        self.messages.append(Message(request.message, self.client_name, datetime.datetime.now(), 'client'))
        return messenger_pb2.Empty()

    def serve(self):
        """ start server """
        self.stop_event = threading.Event()
        server = grpc.server(futures.ThreadPoolExecutor())
        messenger_pb2_grpc.add_MessengerServicer_to_server(self, server)
        port = "50051"
        server.add_insecure_port(f"[::]:{port}")
        server.start()
        self.stop_event.wait()
        server.stop(None)
        self.main_window.window.close()
        if not self.connected:
            sg.popup_ok("Client disconnected", keep_on_top = True)
        os._exit(0)
