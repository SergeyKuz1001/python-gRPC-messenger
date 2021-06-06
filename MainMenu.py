import PySimpleGUI as sg
import sys
import messenger_server
import messenger_client


sg.theme('DarkAmber')
main_font = "Helvetica"



class MainMenu:
    def __init__(self):
        self.window  = self.main_menu()
        self.data = {'name': None, 'port': None, 'Host': 'localhost'}
        self.state = 'main'

    def processing(self):
        while True:
            event, values = self.window.read()
            if event == sg.WIN_CLOSED:
                self.window.close()
                sys.exit()
                
            if self.state == 'main':
                if event == 'Exit':
                    self.window.close()
                    exit(0)
                elif event == 'Server':
                    self.window.close()
                    self.window = self.server_menu()
                elif event == 'Client':
                    self.window.close()
                    self.window = self.client_menu()
                    
            elif self.state == 'server':
                if event == sg.WIN_CLOSED:
                    self.window.close()
                    exit(0)
                elif event == 'Submit':
                    if values['-N-'] and values['-P-']:
                        self.data['name'] = values['-N-']
                        self.data['port'] = values['-P-']
                        self.window.close()
                        return (self.state, self.data)
                    else:
                        sg.popup_error('You have to enter all nessesary data')
                elif event == 'Cancel':
                    self.window.close()
                    self.window = self.main_menu()

                    
            elif self.state == 'client':
                if event == sg.WIN_CLOSED:
                    self.window.close()
                    exit(0)
                elif event == 'Submit':
                    if values['-N-'] and values['-P-'] and values['-H-']: 
                        self.data['name'] = values['-N-']
                        self.data['port'] = values['-P-']
                        self.data['host'] = values['-H-']
                        self.window.close()
                        return (self.state, self.data)
                    else:
                        sg.popup_error('You have to enter all nessesary data')
                        
                elif event == 'Cancel':
                    self.window.close()
                    self.window = self.main_menu()
                    
        
    def main_menu(self):
        self.state = 'main'
        main_layout = [  [sg.Text('Simple peer-to-peer chat', size=(100, 4), justification='center', font=(main_font, 25))],
            [sg.Text('Choose your mode:', size=(70, 2), justification='center', font=(main_font, 20))],
            [sg.Button('Server', size=(40, 2), font=(main_font, 20))],
            [sg.Button('Client',size=(40, 2), font=(main_font, 20))],
            [sg.Text('', size=(35, 5))],
            [sg.Button('Exit',size=(40, 1),  font=(main_font, 16))]]
        
        return sg.Window('Peer-to-peer chat', main_layout, element_justification='c', size=(1024,768), finalize=True)
        


    def client_menu(self):

        
        self.state  = 'client'
        client_layout = [[sg.Text('Please, enter all information here',size=(100, 4), justification='center', font=(main_font, 25))],
          [sg.Text('Name:', size=(8, 1)), sg.In(key='-N-', size=(40, 5), font=(main_font, 20))],
          [sg.Text('', size=(35, 3))],
          [sg.Text('Port:', size=(8, 1)), sg.In(key='-P-', size=(40, 5), font=(main_font, 20))],
          [sg.Text('', size=(35, 3))],
          [sg.Text('Host:', size=(8, 1)), sg.In(key='-H-', size=(40, 5), font=(main_font, 20)),],
          [sg.Text('', size=(35, 6))],
          [sg.Text('', size=(25, 1)), sg.Button('Submit', size=(20, 3)), sg.Text('', size=(5, 1)), sg.Button('Cancel', size=(20, 3))]]


        return sg.Window('Client', client_layout, size=(1024,768), finalize=True)
        
            
    def server_menu(self):
        
        self.state = 'server'
        server_layout = [[sg.Text('Please, enter all information here',size=(100, 4), justification='center', font=(main_font, 25))],
          [sg.Text('Name:', size=(8, 1)), sg.In(key='-N-', size=(40, 5), font=(main_font, 20))],
          [sg.Text('', size=(35, 3))],
          [sg.Text('Port:', size=(8, 1)), sg.In(key='-P-', font=(main_font, 20))],
          [sg.Text('', size=(35, 3))],
          [sg.Text('', size=(35, 6))],
          [sg.Text('', size=(25, 1)), sg.Button('Submit', size=(20, 3)), sg.Text('', size=(5, 1)), sg.Cancel(size=(20, 3))]]
        return sg.Window('Server', server_layout, size=(1024,768), finalize=True)
        


