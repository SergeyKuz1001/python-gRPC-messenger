import grpc
import threading
import os
import sys
import datetime
import messenger_pb2
import messenger_pb2_grpc
import PySimpleGUI as sg


from ChatMenu import ChatWindow
from message import Message


class Client:
    def __init__(self, name, host, port):
        self.my_name = name
        self.companion_name = None
        self.messages = []
        self.client = None
        self.mes = []
        self.my_stream = iter(self.mes)
        self.companion_stream = None

        self.run(host, port)

    def run(self, host, port):
        channel = grpc.insecure_channel(f"{host}:{port}")
        self.client = messenger_pb2_grpc.MessengerStub(channel)

        request = messenger_pb2.MessengerNameRequest(name=self.my_name)
        resp = self.client.startMessaging(request)
        if resp.connected:
            self.companion_name = resp.name
        else:
            print("server already has connected client")
            return

        self.main_window = ChatWindow(self, 'Client')
        self.companion_stream = self.client.sendMessage(self.my_stream)

        th = threading.Thread(target=self.get_messages, daemon=True)
        th.start()

        th1 = threading.Thread(target=self.send_messages, daemon=True)
        th1.start()

        while True:
            message = self.main_window.processing()
            request = messenger_pb2.MessengerMessage(message=message)
            self.mes.append(request)
            self.messages.append(Message(message, self.my_name, datetime.datetime.now(), 'client'))

    def get_messages(self):
        try:
            for mes in self.companion_stream:
                self.main_window.print(Message(mes.message, self.companion_name, datetime.datetime.now(), 'server'))
                self.messages.append(Message(mes.message, self.companion_name, datetime.datetime.now(), 'server'))
        except grpc.RpcError as rpc_error_call:
            self.main_window.window.close()
            sg.popup_ok("Server dropped connection", keep_on_top = True)
            os.exit(0)

    def send_messages(self):
        while True:
            message = self.main_window.processing()
            request = messenger_pb2.MessengerMessage(message=message)
            self.mes.append(request)
            self.messages.append(Message(message, self.my_name, datetime.datetime.now(), 'client'))
