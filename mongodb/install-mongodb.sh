#!/bin/bash
# CentOS 8 에서 MondoDB 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/mongodb/install-mongodb.sh
# 공식 문서 참고 : https://docs.mongodb.com/manual/tutorial/install-mongodb-on-red-hat/

echo "* Update installed package *"
yum -y upgrade

echo "* Configure the package management system (yum). *"
MONGODB_REPO_DIR=/etc/yum.repos.d

if [ ! -f $MONGODB_REPO_DIR/mongodb-org-4.2.repo ]; then
    wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/mongodb/mongodb-org-4.2.repo -P $MONGODB_REPO_DIR/
  yum -y install rabbitmq-server
else
	echo "! $MONGODB_REPO_DIR/mongodb-org-4.2.repo already exists !"
fi

echo "* Install the MongoDB packages. *"
yum install -y mongodb-org

echo "* Configure SELinux *"
yum install -y checkpolicy
