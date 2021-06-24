# commands
python = python
docker = docker
main_prog = $(python) main.py

# action-files (service files)
requirements = .requirements
pb2_files = .pb2-files
docker_image = .docker-image
docker_network = .docker-network
service_files = \
    $(requirements) \
    $(pb2_files) \
    $(docker_image) \
    $(docker_network)

# constants for running simple server and simple client
name = anonymous
port = 50051
host = localhost

# other constants
image_name = peer-to-peer-messenger
network_name = peer-to-peer-messenger-network
simple_server_docker_name = simple-server
docker_container_workdir = /python-gRPC
requirements_file = requirements.txt
proto_file = shared_proto/messenger.proto

# rules with action-file targets
$(requirements) : $(requirements_file)
	$(python) -m pip install --upgrade pip
	$(python) -m pip install -r $^
	touch $@

$(pb2_files) : $(requirements)
	$(python) -m grpc_tools.protoc -I . --python_out=. --grpc_python_out=. \
            $(proto_file)
	touch $@

$(docker_image) : Dockerfile Makefile $(requirements_file) $(proto_file)
	$(docker) build -t $(image_name) .
	touch $@

$(docker_network) :
	$(docker) network create $(network_name) || true
	touch $@

# rules for common running
simple_server : $(requirements) $(pb2_files)
	$(main_prog) \
            --mode server \
            --name $(name) \
            --port $(port)

simple_client : $(requirements) $(pb2_files)
	$(main_prog) \
            --mode client \
            --name $(name) \
            --port $(port) \
            --host $(host)

# rules for running via docker
simple_server_docker : $(docker_image) $(docker_network)
	xhost +si:localuser:root
	$(docker) run \
            --rm \
            -it \
            -v $(shell pwd):$(docker_container_workdir) \
            -w $(docker_container_workdir) \
            -e DISPLAY=:0 \
            -p $(localhost):$(port):$(port)/tcp \
            --network $(network_name) \
            -v /tmp/.X11-unix:/tmp/.X11-unix \
            --name $(simple_server_docker_name) \
            $(image_name) \
            make simple_server \
                name=$(name) \
                port=$(port)

simple_client_docker : $(docker_image) $(docker_network)
	xhost +si:localuser:root
	$(docker) run \
            --rm \
            -it \
            -v $(shell pwd):$(docker_container_workdir) \
            -w $(docker_container_workdir) \
            -e DISPLAY=:0 \
            --network $(network_name) \
            -v /tmp/.X11-unix:/tmp/.X11-unix \
            $(image_name) \
            make simple_client \
                name=$(name) \
                port=$(port) \
                host=$(simple_server_docker_name)

# other rules
install : $(requirements) $(pb2_files)

clean :
	rm -f $(service_files)
