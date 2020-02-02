#!/bin/bash
# CentOS 8 에서 NGINX 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/nginx/install-nginx.sh
# 문서 참고 : https://www.cyberciti.biz/faq/how-to-install-and-use-nginx-on-centos-8/

echo "* Update installed package *"
yum -y upgrade

echo "* Install NGINX *"
yum -y install nginx

echo "* Enable nginx server *"
systemctl enable nginx
systemctl start nginx

echo "* Open port 80 and 443 using firewall-cmd *"
firewall-cmd --permanent --zone=public --add-service=https --add-service=http
firewall-cmd --reload

echo "* SELinux boolean value for httpd network connect to on *"
setsebool -P httpd_can_network_connect on