#!/bin/sh

# 创建libs目录
mkdir explorer/explorer-device-tme/libs/

# 拉取 tme sdk
git clone https://$GIT_ACCESS_TOKEN@github.com/tencentyun/iot-thirdparty-java.git

# 解密
gpg -d --passphrase "$DECRYPT_PASSPHRASE" --batch --quiet iot-thirdparty-java/tme/ultimatelib-v1.0-200024-release.aar.asc > explorer/explorer-device-tme/libs/ultimatelib-v1.0-200024-release.aar
gpg -d --passphrase "$DECRYPT_PASSPHRASE" --batch --quiet iot-thirdparty-java/tme/ultimatetv-v0.9.1-arm-release.aar.asc > explorer/explorer-device-tme/libs/ultimatetv-v0.9.1-arm-release.aar

# 启用tme
sed -i 's#//-##g' settings.gradle

# sdkdemo 启用tme
sed -i 's#com/tencent/iot/explorer/device/tme/##g' explorer/device-android-demo/build.gradle

