#!/bin/bash
# CentOS 8 에서 RabbitMQ 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/rabbitmq/install-rabbitmq.sh
# 공식 문서 참고 https://www.rabbitmq.com/install-rpm.html

# Setup Extra Packages for Enterprise Linux (EPEL)
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm

# Install Erlang
# Erlang package from the EPEL Repository
yum -y install erlang

# Install RabbitMQ Server
# Using Bintray Yum Repository
rpm --import https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc
wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/rabbitmq/rabbitmq.repo -P /etc/yum.repos.d/
yum -y install rabbitmq-server

# To start the daemon by default when the system boots, as an administrator run
systemctl enable rabbitmq-server.service

# Setup OS limits
wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/rabbitmq/limits.conf -P /etc/systemd/system/rabbitmq-server.service.d/