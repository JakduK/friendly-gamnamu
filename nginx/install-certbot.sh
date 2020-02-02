#!/bin/bash
# CentOS 8 에서 certbot 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/nginx/install-certbot.sh
# 공식 문서 참고 : https://certbot.eff.org/lets-encrypt/centosrhel8-nginx

echo "* Install wget *"
yum -y install wget

echo "* Install Certbot *"
wget https://dl.eff.org/certbot-auto
mv certbot-auto /usr/local/bin/certbot-auto
chown root /usr/local/bin/certbot-auto
chmod 0755 /usr/local/bin/certbot-auto

echo "* Get a certificate and have Certbot edit your Nginx configuration automatically *"
/usr/local/bin/certbot-auto --nginx -d jakduk.com -d api.jakduk.com -d mq.jakduk.com -d jenkins.jakduk.com -d dev-api.jakduk.com -d dev-web.jakduk.com -d www.jakduk.com -d wiki.jakduk.com