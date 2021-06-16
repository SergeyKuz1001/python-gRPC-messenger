from concurrent import futures
import logging
import grpc
import threading
import datetime
import messenger_pb2
import messenger_pb2_grpc
import os
import PySimpleGUI as sg


from ChatMenu import ChatWindow
from message import Message


class Server(messenger_pb2_grpc.MessengerServicer):
    def __init__(self, name):
        self.my_name = name
        self.companion_name = None
        self.connected = False
        self.messages = []
        self.main_window = None
        self.stop_event = None
        self.mes = []
        self.my_stream = iter(self.mes)
        self.companion_stream = None

        self.serve()

    def startMessaging(self, request, context):
        logging.info(f'got name "{request.name}"')
        if not self.connected:
            self.companion_name = request.name
            self.connected = True

            self.main_window = ChatWindow(self, 'Server')
            return messenger_pb2.MessengerNameResponse(name=self.my_name, connected=True)

        else:
            return messenger_pb2.MessengerNameResponse(connected=False)

    def stopMessaging(self, request, context):
        logging.info('disconnected')
        self.connected = False
        self.stop_event.set()
        return messenger_pb2.Empty()
        
    def sendMessage(self, request_iterator, context):
        self.companion_stream = request_iterator
        th = threading.Thread(target=self.get_messages, daemon=True)
        th.start()

        th1 = threading.Thread(target=self.send_messages, daemon=True)
        th1.start()

    def serve(self):
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

    def get_messages(self):
        try:
            for mes in self.companion_stream:
                self.main_window.print(Message(mes.message, self.companion_name, datetime.datetime.now(), 'server'))
                self.messages.append(Message(mes.message, self.companion_name, datetime.datetime.now(), 'server'))
        except grpc.RpcError as rpc_error_call:
            self.main_window.window.close()
            sg.popup_ok("connection dropped", keep_on_top=True)
            os.exit(0)

    def send_messages(self):
        while True:
            message = self.main_window.processing()
            request = messenger_pb2.MessengerMessage(message=message)
            self.mes.append(request)
            self.messages.append(Message(message, self.my_name, datetime.datetime.now(), 'client'))
