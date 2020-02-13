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

if [ ! -f mongodb_cgroup_memory.te ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/mongodb/mongodb_cgroup_memory.te
else
	echo "! mongodb_cgroup_memory.te already exists !"
fi

checkmodule -M -m -o mongodb_cgroup_memory.mod mongodb_cgroup_memory.te
semodule_package -o mongodb_cgroup_memory.pp -m mongodb_cgroup_memory.mod
sudo semodule -i mongodb_cgroup_memory.pp

echo "* Modify Firewall *"
firewall-cmd --add-port=27017/tcp --permanent
firewall-cmd --reload

echo "* Start MongoDB. *"
systemctl start mongod
systemctl enable mongod

echo "* Setup hosts *"

if ! grep -q ".jakduk" /etc/hosts; then
	sudo /sbin/runuser -l root -c "echo '192.168.0.12 mongodb1.jakduk' >> /etc/hosts"
	sudo /sbin/runuser -l root -c "echo '192.168.0.12 mongodb3.jakduk' >> /etc/hosts"
else
    echo "** mongodb.jakduk host config already exists /etc/hosts path **"
fi

# /etc/mongod.conf 수정 및 mongo 터미널 들어가서, rs.conf() 등 설정 필요 함.