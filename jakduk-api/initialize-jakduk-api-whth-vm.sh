#!/bin/bash
# CentOS 8 에서 JAKDUK-API Application 초기화를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/initialize-jakduk-api-whth-vm.sh

 if [ "$#" -eq  "0" ]
   then
     echo "Input profile argument. e.g. dev, prod"
 else
     echo "PROFILE : $1"
 fi

PROFILE=$1

echo "* Add jakduk user *"
if ! grep -q "jakduk" /etc/passwd; then
	sudo useradd jakduk
else
    echo "WARN : jakduk user already exists /etc/passwd"
fi

echo "* Setup sudoers for jakduk user *"
SUDOERS_DIR=/etc/sudoers.d

if [ ! -f $SUDOERS_DIR/jakduk-user ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/jakduk-user -P $SUDOERS_DIR/
  chmod 0400 $SUDOERS_DIR/jakduk-user
else
	echo "WARN : $SUDOERS_DIR/jakduk-user already exists"
fi

echo "* Setup jenkins jakduk user id_rsa.pub *"

sudo /sbin/runuser -l jakduk -c "mkdir -p .ssh"
sudo /sbin/runuser -l jakduk -c "chmod 700 .ssh"

if ! grep -q "jakduk" .ssh/authorized_keys; then
  sudo /sbin/runuser -l jakduk -c "wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/jenkins_jakduk_id_rsa.pub"
	sudo /sbin/runuser -l jakduk -c "cat jenkins_jakduk_id_rsa.pub >> .ssh/authorized_keys"
	sudo /sbin/runuser -l jakduk -c "chmod 600 .ssh/authorized_keys"
	sudo /sbin/runuser -l jakduk -c "rm -f jenkins_jakduk_id_rsa.pub"
else
    echo "WARN : jakduk user id_rsa.pub already exists .ssh/authorized_keys"
fi

echo "* Setup jakduk working directory *"
if [ ! -d /jakduk ]; then
  mkdir /jakduk
  mkdir /jakduk/api
  mkdir /jakduk/storage
  chown -R jakduk:jakduk /jakduk
else
	echo "WARN : /jakduk directory already exists"
fi

echo "* Setup jakduk stoarge mount *"
yum install -y nfs-utils

if ! grep -q "jakduk/storage" /etc/fstab ; then
  sudo /sbin/runuser -l root -c "echo '192.168.0.9:/storage /jakduk/storage nfs defaults 0 0' >> /etc/fstab"
  sudo /sbin/runuser -l root -c "mount -a"
else
    echo "WARN : jakduk stoarge mount already exists /etc/fstab"
fi

echo "* Install JDK 1.8 *"
yum install -y java-1.8.0-openjdk.x86_64

echo "* Setup jakduk-api service for systemd *"
SYSTEMD_DIR=/etc/systemd/system

if [ ! -f $SYSTEMD_DIR/jakduk-api.service ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/jakduk-api/jakduk-api.service -P $SYSTEMD_DIR/
  systemctl enable jakduk-api.service
else
	echo "WARN : $SYSTEMD_DIR/jakduk-api.service already exists"
fi

echo "* Setup jakduk-api conf file *"

if [ ! -f /jakduk/api/jakduk-api-1.0.0.conf ]; then
  sudo /sbin/runuser -l jakduk -c "echo 'export JAVA_OPTS=\"-Dspring.profiles.active=$PROFILE -Dfile.encoding=UTF-8 -Xms512m -Xmx512m\"' > /jakduk/api/jakduk-api-1.0.0.conf"
else
	echo "WARN : jakduk-api-1.0.0.conf already exists"
fi