import grpc
import threading
import sys
import datetime
import messenger_pb2
import messenger_pb2_grpc
import PySimpleGUI as sg


from ChatMenu import ChatWindow
from message import Message




class Client:
    def __init__(self, name, host, port):
        self.name = name
        self.server_name = None
        self.messages = []
        self.client = None

        self.run(host, port)

    def run(self, host, port):
        channel = grpc.insecure_channel(f"{host}:{port}")
        self.client = messenger_pb2_grpc.MessengerStub(channel)

        request = messenger_pb2.MessengerNameRequest(name=self.name)
        resp = self.client.startMessaging(request)
        if resp.connected:
            self.server_name = resp.name
        else:
            print("server already has connected client")
            return

        self.main_window = ChatWindow(self, 'Client')
        th = threading.Thread(target=self.get_messages, daemon=True)
        th.start()
        
        while True:
                message = self.main_window.processing()
                try:
                    request = messenger_pb2.MessengerMessage(message=message)
                    self.client.getMessage(request)
                except:
                    sys.exit()
                self.main_window.print(Message(message, self.name, datetime.datetime.now(), 'client'))
                self.messages.append(Message(message, self.name, datetime.datetime.now(), 'client'))

    def get_messages(self):
        resp = self.client.sendMessage(messenger_pb2.Empty())
        for mes in resp:
            self.main_window.print(Message(mes.message, self.server_name, datetime.datetime.now(), 'server'))
            self.messages.append(Message(mes.message, self.server_name, datetime.datetime.now(), 'server'))
