FROM python:3.9.5
WORKDIR /python-gRPC
COPY build build
COPY requirements.txt requirements.txt
COPY messenger.proto messenger.proto
RUN ./build
COPY . .
EXPOSE 50051
ENV DISPLAY :0
