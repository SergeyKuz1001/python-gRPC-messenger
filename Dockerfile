FROM python:3.9.5
COPY Makefile Makefile
COPY requirements.txt requirements.txt
COPY shared_proto/messenger.proto shared_proto/messenger.proto
RUN make install
