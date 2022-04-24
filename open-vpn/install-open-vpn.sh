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