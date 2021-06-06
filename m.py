import PySimpleGUI as sg
import argparse
import messenger_server
import messenger_client
from MainMenu import MainMenu

if __name__ == '__main__':

        
    menu = MainMenu()
    args = menu.processing()
    del menu
    if args[0] == 'server':
        #sg.popup_ok('Please, wait for connection')
        print(args[1])
        messenger_server.Server(args[1]['name'])
    else:
        messenger_client.Client(args[1]['name'], args[1]['host'], args[1]['port'])
