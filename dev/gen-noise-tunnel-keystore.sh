#!/bin/bash

export KEYSTORE_PASS=flatline-test

keytool -genkeypair \
  -alias flatline \
  -keyalg RSA \
  -keysize 2048 \
  -dname "CN=flatline, OU=flatline, O=flatline" \
  -validity 36500 \
  -keystore noise-tunnel-keystore.p12 \
  -storetype PKCS12 \
  -storepass "$KEYSTORE_PASS" \
  -keypass   "$KEYSTORE_PASS"
