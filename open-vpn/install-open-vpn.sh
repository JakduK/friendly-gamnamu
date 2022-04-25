#!/bin/bash
# CentOS 7 에서 OpenVPN 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/open-vpn/install-open-vpn.sh
# 문서 참고 : https://phoenixnap.com/kb/openvpn-centos

echo "* Update the CentOS repositories and packages. *"
yum update -y

echo "* Enable the EPEL repository. *"
yum install epel-release -y

echo "* Again update the CentOS repositories and packages. *"
yum update -y

echo "* Install OpenVPN. *"
yum install -y openvpn

echo "* Download Easy RSA latest version. *"
wget https://github.com/OpenVPN/easy-rsa/archive/v3.0.8.tar.gz
tar -xf v3.0.8.tar.gz
rm -f v3.0.8.tar.gz
mkdir -p /etc/openvpn/easy-rsa
mv easy-rsa-3.0.8 /etc/openvpn/easy-rsa

echo "* Setup server.conf files *"
OPEN_VPN_CONF_DIR=/etc/openvpn
if [ ! -f $OPEN_VPN_CONF_DIR/server.conf ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/open-vpn/server.conf -P /etc/openvpn
else
	echo "WARN : OpenVPN server.conf files already exists"
fi