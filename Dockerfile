FROM python:3.9.5
WORKDIR /python-gRPC
RUN python -m pip install --upgrade pip
COPY requirements.txt requirements.txt
RUN python -m pip install -r requirements.txt
COPY messenger.proto messenger.proto
RUN python -m grpc_tools.protoc -I . --python_out=. --grpc_python_out=. messenger.proto
COPY messenger_client.py messenger_client.py
COPY messenger_server.py messenger_server.py
EXPOSE 50051
