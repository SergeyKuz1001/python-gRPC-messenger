import PySimpleGUI as sg
import sys
sg.theme('DarkAmber')


layout = [[sg.Text('Peer-to-peer chat', size=(100, 4), justification='center', font=("Helvetica", 25))],
            [sg.Output(size=(150,10), key='-OUTPUT-',  font=("Helvetica", 15))],
            [sg.In(key='-IN-', size=(43,3), font=("Helvetica", 15))],
            [sg.Button('Send', size=(32, 2)), sg.Button('Clear', size=(32, 2))],
            [sg.Text('', size=(35, 5))],
            [sg.Text('', size=(35, 1)), sg.Button('Exit', size=(30, 2))]]

    


class ChatWindow:
    def __init__(self):
        self.window  = sg.Window('Peer-to-peer chat', layout, size=(1024,768), finalize=True)


    def print(self, msg):
        print(msg.time, msg.name, ">", msg.message)
        
    def processing(self):
        while True:
            event, values = self.window.read()
            if event == 'Send':
                return values['-IN-']
            
            if event in (sg.WIN_CLOSED, 'Exit'):
                sys.exit()
                
            if event == 'Clear':
                self.window['-OUTPUT-'].update('')

