#!/bin/bash
# CentOS 8 에서 NGINX 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/nginx/install-nginx.sh
# 문서 참고 : https://www.cyberciti.biz/faq/how-to-install-and-use-nginx-on-centos-8/

echo "* Update installed package *"
yum -y upgrade

echo "* Install NGINX *"
yum -y install nginx

echo "* Open port 80 and 443 using firewall-cmd *"
firewall-cmd --permanent --zone=public --add-service=https --add-service=http
firewall-cmd --reload

echo "* SELinux boolean value for httpd network connect to on *"
setsebool -P httpd_can_network_connect on

echo "* Setup hosts *"
if ! grep -q ".jakduk" /etc/hosts; then
	sudo /sbin/runuser -l root -c "echo '192.168.0.8 jenkins.jakduk' >> /etc/hosts"
	sudo /sbin/runuser -l root -c "echo '192.168.0.15 rabbitmq2.jakduk' >> /etc/hosts"
	sudo /sbin/runuser -l root -c "echo '192.168.0.7 dev-api2.jakduk' >> /etc/hosts"
	sudo /sbin/runuser -l root -c "echo '192.168.0.27 web-dev.jakduk' >> /etc/hosts"
else
    echo "WARN : jakduk host config already exists /etc/hosts path"
fi

echo "* Setup conf files *"
NGINX_CONF_DIR=/etc/nginx/conf.d

if [ ! -f $NGINX_CONF_DIR/dev-api.conf ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/nginx/conf.d/dev-api.conf -P $NGINX_CONF_DIR/
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/nginx/conf.d/jenkins.conf -P $NGINX_CONF_DIR/
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/nginx/conf.d/rabbitmq.conf -P $NGINX_CONF_DIR/
else
	echo "WARN : NGINX conf files already exists"
fi

echo "* Enable nginx server *"
systemctl enable nginx
systemctl start nginx
