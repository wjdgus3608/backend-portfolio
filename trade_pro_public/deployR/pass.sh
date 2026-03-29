#!bin/bash
url="ec2-13-125-15-165"
scp -i "/Users/jung-mac/Downloads/jung2.pem" docker-compose.yml ec2-user@${url}.ap-northeast-2.compute.amazonaws.com:~/workspace/real;

scp -i "/Users/jung-mac/Downloads/jung2.pem" logdbR.jar ec2-user@${url}.ap-northeast-2.compute.amazonaws.com:~/workspace/real;

scp -i "/Users/jung-mac/Downloads/jung2.pem" webappR.jar ec2-user@${url}.ap-northeast-2.compute.amazonaws.com:~/workspace/real;

scp -i "/Users/jung-mac/Downloads/jung2.pem" realtradeapp.jar ec2-user@${url}.ap-northeast-2.compute.amazonaws.com:~/workspace/real;

