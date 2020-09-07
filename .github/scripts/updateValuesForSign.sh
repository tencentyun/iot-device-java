#!/bin/sh

key_id=$KEY_ID_OF_SIGN
password=$PASSWORD_OF_SIGN
root_path=$(pwd)
sed -i 's#MY_KEY_ID#'$key_id'#g' gradle.properties
sed -i 's#MY_PASSWORD#'$password'#g' gradle.properties
sed -i 's#MY_KEY_RING_FILE#'$root_path'/secring.gpg#g' gradle.properties