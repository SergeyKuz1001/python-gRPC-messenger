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
                val = values['-IN-']
                self.window['-IN-'].update('')
                if val:
                    return val
            
            if event == '_EXIT_' or event == 'Exit' or event == sg.WIN_CLOSED:
                return

            if event == 'Clear':
                self.window['-OUTPUT-'].update('')

