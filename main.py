import argparse
import messenger_server
import messenger_client

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--mode', type=str, choices=['server', 'client'], required=True, help='server or client mode')
    parser.add_argument('--name', type=str, required=True, help='your name')
    parser.add_argument('--host', type=str, help='host')
    parser.add_argument('--port', type=int, help='port')
    args = parser.parse_args()

    if args.mode == 'client' and args.host is None:
        parser.error('host must be specified in client mode')

    if args.port is None:
        args.port = 50051

    if args.mode == 'server':
        messenger_server.Server(args.name, args.port)
    else:
        messenger_client.Client(args.name, args.host, args.port)
