#!/bin/bash
# CentOS 8 에서 RabbitMQ 설치를 위한 스크립트
# 공식 문서 참고 https://www.rabbitmq.com/install-rpm.html

# Install Erlang
# Erlang package from the EPEL Repository
yum -y install erlang

# Install RabbitMQ Server
# Using Bintray Yum Repository
rpm --import https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc
c