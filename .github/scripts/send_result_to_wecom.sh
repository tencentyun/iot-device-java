#!/bin/sh

color=""
if [ $2 == "success" ]
then
   echo "success"
   color="info"
else
   echo "fail"
   color="warning"
fi

curl "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=$IOT_WECOM_CID_ROBOT_KEY" \
   -H 'Content-Type: application/json' \
   -d '
   {
      "msgtype": "markdown",
      "markdown": {
           "content": "**repo: \"'"$1"'\"**\n
           >result:<font color=\"'"$color"'\">\"'"$2"'\"</font>\n
           >[action](https://github.com/tencentyun/iot-device-java/actions)"\n
      }
   }'
