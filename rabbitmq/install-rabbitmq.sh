#!/bin/bash
# CentOS 8 에서 RabbitMQ 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/rabbitmq/install-rabbitmq.sh
# 공식 문서 참고 : https://www.rabbitmq.com/install-rpm.html

echo "* Update installed package *"
yum -y upgrade

echo "* Setup Extra Packages for Enterprise Linux (EPEL) *"
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm

# Erlang package from the EPEL Repository
echo "* Install Erlang *"
yum -y install erlang

# Using Bintray Yum Repository
echo "* Install RabbitMQ Server *"
RABBIT_MQ_REPO_DIR=/etc/yum.repos.d

if [ ! -f $RABBIT_MQ_REPO_DIR/rabbitmq.repo ]; then
  rpm --import https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/rabbitmq/rabbitmq.repo -P $RABBIT_MQ_REPO_DIR/
  yum -y install rabbitmq-server
else
	echo "! $RABBIT_MQ_REPO_DIR/rabbitmq.repo already exists !"
fi

echo "* To start the daemon by default when the system boots, as an administrator run *"
systemctl enable rabbitmq-server.service

echo "** Setup OS limits **"
ETC_SYSTEMD_SYSTEM_RABBITMQ_SERVER_DIR=/etc/systemd/system/rabbitmq-server.service.d

if [ ! -f $ETC_SYSTEMD_SYSTEM_RABBITMQ_SERVER_DIR/limits.conf ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/rabbitmq/limits.conf -P $ETC_SYSTEMD_SYSTEM_RABBITMQ_SERVER_DIR/
else
	echo "! $ETC_SYSTEMD_SYSTEM_RABBITMQ_SERVER_DIR/limits.conf already exists !"
fi

echo "* Enable Management Plugin *"
rabbitmq-plugins enable rabbitmq_management

echo "* Add administrator user *"
rabbitmqctl add_user admin wkrenakstp@
rabbitmqctl set_user_tags admin administrator

echo "* Modify Firewall *"
firewall-cmd --add-port={5672,15672}/tcp --permanent
firewall-cmd --reload

# Management 사이트에서 이후 admin 계정에 / virtualhost 생성 및 queue 생성 필요