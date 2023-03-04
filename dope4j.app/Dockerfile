# Matrix of TensorRT supported versions:
# https://docs.nvidia.com/deeplearning/frameworks/support-matrix/index.html#unique_742037603

FROM nvcr.io/nvidia/tensorrt:22.08-py3

RUN apt update

RUN apt -y install sudo
RUN echo "%sudo   ALL=(ALL:ALL) ALL" >> /etc/sudoers

RUN apt -y install zsh

# TensorRT image has root user exposed as a default one
# For security reasons we change that to 'ubuntu' user
RUN useradd -s /bin/zsh -m -G sudo ubuntu
RUN echo 'ubuntu:ubuntu' | chpasswd

RUN apt install -y openjdk-17-jre

ARG WORKSPACE=/home/ubuntu
COPY release/dope4j*zip $WORKSPACE

RUN unzip $WORKSPACE/dope4j*zip -d $WORKSPACE

USER ubuntu
ENV HOME /home/ubuntu
WORKDIR $WORKSPACE/dope4j
