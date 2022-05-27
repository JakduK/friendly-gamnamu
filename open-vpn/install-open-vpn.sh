#!/bin/bash
# CentOS 7 에서 OpenVPN 설치를 위한 스크립트
# 스크립트 얻는법 : wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/open-vpn/install-open-vpn.sh
# 문서 참고
# https://www.digitalocean.com/community/tutorials/how-to-set-up-and-configure-an-openvpn-server-on-centos-7
# https://phoenixnap.com/kb/openvpn-centos
# https://github.com/OpenVPN/easy-rsa/

echo "* Update the CentOS repositories and packages. *"
yum update -y

echo "* Enable the EPEL repository. *"
yum install epel-release -y

echo "* Again update the CentOS repositories and packages. *"
yum update -y

echo "* Install OpenVPN. *"
yum install -y openvpn

echo "* Download Easy RSA latest version. *"
wget https://github.com/OpenVPN/easy-rsa/archive/v3.1.0.tar.gz
tar -xf v3.1.0.tar.gz
rm -f v3.1.0.tar.gz
mv easy-rsa-3.1.0 /etc/openvpn/easy-rsa

echo "* Setup server.conf file. *"
OPEN_VPN_CONF_DIR=/etc/openvpn

if [ ! -f $OPEN_VPN_CONF_DIR/server.conf ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/open-vpn/server.conf -P $OPEN_VPN_CONF_DIR
else
	echo "WARN : OpenVPN server.conf file already exists"
fi

echo "* Setup vars file. *"
if [ ! -f $OPEN_VPN_CONF_DIR/easy-rsa/easyrsa3 ]; then
  wget https://raw.githubusercontent.com/JakduK/friendly-gamnamu/master/open-vpn/vars -P $OPEN_VPN_CONF_DIR/easy-rsa/easyrsa3
else
	echo "WARN : OpenVPN vars file already exists"
fi

echo "* Create key for HMAC firewall *"
openvpn --genkey --secret /etc/openvpn/myvpn.tlsauth

echo "* Building the certificate authority. *"
cd /etc/openvpn/easy-rsa/easyrsa3
# CA 는 직접 입력 필요 e.g. openvpn.jakduk.dev
./easyrsa build-ca nopass

echo "* Create a key and certificate for the server. *"
# PEM pass phrase는 직접 입력 필요
./easyrsa build-server-full server

echo "* Generate a Diffie-Hellman key exchange file. *"
./easyrsa gen-dh

echo "* Create a certificate and key for client1. *"
# /etc/openvpn/easy-rsa/easyrsa3 디렉터리에서 실행해야 함
# PEM pass phrase는 직접 입력 필요
./easyrsa build-client-full client1

echo "* Copy key and certificate files to /etc/openvpn *"
cp /etc/openvpn/easy-rsa/easyrsa3/pki/ca.crt /etc/openvpn/easy-rsa/easyrsa3/pki/dh.pem /etc/openvpn
cp /etc/openvpn/easy-rsa/easyrsa3/pki/issued/server.crt /etc/openvpn/easy-rsa/easyrsa3/pki/dh.pem /etc/openvpn
cp /etc/openvpn/easy-rsa/easyrsa3/pki/private/ca.key /etc/openvpn/easy-rsa/easyrsa3/pki/private/server.key /etc/openvpn

echo "* Modify Firewall *"
firewall-cmd --zone=public --add-service openvpn
firewall-cmd --zone=public --add-service openvpn --permanent
#firewall-cmd --add-masquerade
#firewall-cmd --add-masquerade --permanent

echo "* Enable OpenVPN *"
systemctl start openvpn@server.service
# systemd-tty-ask-password-agent 를 실행해서 비밀번호를 넣어야 한다.
systemctl enable openvpn@server.service

