import PySimpleGUI as sg
import messenger_pb2
import datetime
import sys

sg.theme('DarkAmber')


class ChatWindow:
    def __init__(self, app, name):
        
        layout = [[sg.Text('Peer-to-peer chat', size=(100, 4), justification='center', font=("Helvetica", 25))],
            [sg.Output(size=(150, 10), key='-OUTPUT-',  font=("Helvetica", 15))],
            [sg.In(key='-IN-', size=(43,3), font=("Helvetica", 15))],
            [sg.Button('Send', size=(32, 2)), sg.Button('Clear', size=(32, 2))],
            [sg.Text('', size=(35, 5))],
            [sg.Text('', size=(35, 1)), sg.Button('Exit', size=(30, 2))]]

        self.name = name
        self.app = app
        self.window = sg.Window(self.name, layout, size=(1024,768), finalize=True)
        self.window.Element('-OUTPUT-').TKOut.output.bind("<Key>", lambda e: "break")

    def print(self, msg):
        print(msg.time.strftime("%D,  %H:%M"), msg.name, ">", msg.message)
        
    def processing(self):
        while True:
            event, values = self.window.read()

            if event == 'Send':
                return values['-IN-']

            if event == sg.WIN_CLOSED or event == 'Exit':
                if self.app.__class__.__name__ == 'Client':
                    res = self.app.client.stopMessaging(messenger_pb2.Empty())
                    self.window.close()
                else:
                    self.window.close()
                    self.app.stop_event.set()

                return

            if event == 'Clear':
                self.window['-OUTPUT-'].update('')
