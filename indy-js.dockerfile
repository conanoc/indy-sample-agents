FROM ubuntu:16.04

RUN apt-get update && apt-get install -y apt-transport-https

RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys CE7709D068DB5E88
RUN echo "deb https://repo.sovrin.org/sdk/deb xenial stable" >> /etc/apt/sources.list

RUN apt-get update && apt-get install -y libindy

RUN apt-get install -y curl
RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash -
RUN apt-get install -y nodejs

# For gyp
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:deadsnakes/ppa && apt-get update && apt-get install -y python3.6
RUN rm /usr/bin/python3 && ln -s /usr/bin/python3.6 /usr/bin/python3
RUN apt-get install -y build-essential
