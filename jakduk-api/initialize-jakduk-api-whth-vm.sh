#!/bin/bash
# CentOS 8 에서 JAKDUK-API Application 초기화를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/initialize-jakduk-api-whth-vm.sh

echo "* Add jakduk user *"
if ! grep -q "jakduk" /etc/passwd; then
	sudo useradd jakduk
else
    echo "** jakduk user already exists /etc/passwd **"
fi

echo "* Setup sudoers for jakduk user *"
SUDOERS_DIR=/etc/sudoers.d

if [ ! -f $SUDOERS_DIR/jakduk-user ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/jakduk-user -P $SUDOERS_DIR/
  chmod 0400 $SUDOERS_DIR/jakduk-user
else
	echo "! $SUDOERS_DIR/jakduk-user already exists !"
fi

echo "* Setup jenkins jakduk user id_rsa.pub *"
SUDOERS_DIR=/etc/sudoers.d

sudo /sbin/runuser -l jakduk -c "mkdir -p .ssh"
sudo /sbin/runuser -l jakduk -c "chmod 700 .ssh"

if ! grep -q "jakduk" .ssh/authorized_keys; then
  sudo /sbin/runuser -l jakduk -c "wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/jenkins_jakduk_id_rsa.pub"
	sudo /sbin/runuser -l jakduk -c "cat jenkins_jakduk_id_rsa.pub >> .ssh/authorized_keys"
	sudo /sbin/runuser -l jakduk -c "chmod 600 .ssh/authorized_keys"
	sudo /sbin/runuser -l jakduk -c "rm -f jenkins_jakduk_id_rsa.pub"
else
    echo "** jakduk user id_rsa.pub already exists .ssh/authorized_keys **"
fi
