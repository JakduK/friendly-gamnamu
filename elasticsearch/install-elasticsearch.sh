#!/bin/bash
# CentOS 8 에서 Elasticsearch 7.x 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/elasticsearch/install-elasticsearch.sh
# 공식 문서 참고 : https://www.elastic.co/guide/en/elasticsearch/reference/current/rpm.html
* 노리 플러그인 : https://www.elastic.co/guide/en/elasticsearch/plugins/7.6/analysis-nori.html

echo "* Setup hosts *"
if ! grep -q ".jakduk" /etc/hosts; then
	sudo /sbin/runuser -l root -c "echo '192.168.0.18 elasticsearch5.jakduk' >> /etc/hosts"
else
    echo "** elasticsearch.jakduk host config already exists /etc/hosts path **"
fi

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

echo "* Install Korean (nori) Analysis Plugin *"
/usr/share/elasticsearch/bin/elasticsearch-plugin install analysis-nori

echo "* Modify Firewall *"
firewall-cmd --add-port=9200/tcp --permanent
firewall-cmd --reload

# 이후 /etc/elasticsearch/elasticsearch.yml 과 /etc/elasticsearch/jvm.options 설정 등이 필요함.