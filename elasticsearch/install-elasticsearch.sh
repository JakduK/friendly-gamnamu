#!/bin/bash
# CentOS 8 에서 Elasticsearch 7.x 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/elasticsearch/install-elasticsearch.sh
# 공식 문서 참고 : https://www.elastic.co/guide/en/elasticsearch/reference/current/rpm.html

echo "* Update installed package *"
yum -y upgrade

echo "* Import the Elasticsearch PGP Key *"
rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch

echo "* Installing from the RPM repository *"
ELASTICSEARCH_REPO_DIR=/etc/yum.repos.d

if [ ! -f $ELASTICSEARCH_REPO_DIR/elasticsearch.repo ]; then
  rpm --import https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/elasticsearch/elasticsearch.repo -P $ELASTICSEARCH_REPO_DIR/
  yum install -y --enablerepo=elasticsearch elasticsearch
else
	echo "! $ELASTICSEARCH_REPO_DIR/elasticsearch.repo already exists !"
fi

echo "* Running Elasticsearch with systemd *"
systemctl daemon-reload
systemctl enable elasticsearch.service
systemctl start elasticsearch.service