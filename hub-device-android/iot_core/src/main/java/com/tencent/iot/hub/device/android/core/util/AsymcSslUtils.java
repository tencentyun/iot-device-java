package com.tencent.iot.hub.device.android.core.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;

import com.tencent.iot.hub.device.java.core.util.Asn1Object;
import com.tencent.iot.hub.device.java.core.util.DerParser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class AsymcSslUtils {

    public static final String TAG = "iot.AsymcSslUtils";

    private static String PASSWORD = String.valueOf(new Random(System.currentTimeMillis()).nextInt());

    /**
     * 证书文件及Key文件存放在Android asset目录下，通过AssetManager读取文件内容获取输入流，
     * 通过输入流构造双向认证SSLSocketFactory
     *
     * @param context              Android上下文，可使用进程上下文/Activity
     * @param clientCrtFileName    客户端证书文件名
     * @param clientPriKeyFileName 客户端私钥文件名
     * @return
     */
    public static SSLSocketFactory getSocketFactoryByAssetsFile(Context context, final String clientCrtFileName, final String clientPriKeyFileName) {
        SSLSocketFactory factory = null;

        AssetManager assetManager = context.getAssets();
        if (assetManager == null) {
            return null;
        }

        InputStream clientInputStream = null;
        InputStream keyInputStream = null;
        try {
            clientInputStream = assetManager.open(clientCrtFileName);
            keyInputStream = assetManager.open(clientPriKeyFileName);

            factory = getSocketFactoryByStream(clientInputStream, keyInputStream);;
        } catch (IOException e) {
            TXLog.e(TAG, "getSocketFactory failed, cannot open CRT Files.", e);
        }finally {
            if (clientInputStream != null) {
                try {
                    clientInputStream.close();
                }catch (Exception e) {

                }
            }

            if (keyInputStream != null) {
                try {
                    keyInputStream.close();
                }catch (Exception e) {

                }
            }
        }

        return factory;
    }

    /**
     * 证书文件及Key文件存放在Android 本地存储中，通过FileInputStream读取文件内容输入流
     * 通过输入流解析构造双向认证SSLSocketFactory
     *
     * @param clientCrtFileName    客户端证书文件名，要求全路径
     * @param clientPriKeyFileName 客户端私钥文件名，要求全路径
     * @return
     */
    public static SSLSocketFactory getSocketFactoryByFile(final String clientCrtFileName, final String clientPriKeyFileName) {
        return com.tencent.iot.hub.device.java.core.util.AsymcSslUtils.getSocketFactoryByFile(clientCrtFileName, clientPriKeyFileName);
    }

    /**
     * 获取双向认证SSLSocketFactory
     *
     * @param clientInput 设备证书文件输入流
     * @param keyInput    设备私钥文件输入流
     * @return
     */
    public static SSLSocketFactory getSocketFactoryByStream(final InputStream clientInput, final InputStream keyInput) {
        return com.tencent.iot.hub.device.java.core.util.AsymcSslUtils.getSocketFactoryByStream(clientInput, keyInput);
    }

    public static SSLContext getDefaultSLLContext() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {trustManagers}, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslContext;
    }

    /**
     * 信任管理器
     */
    private static TrustManager trustManagers = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    /**
     * 获取SSLSocketFactory
     *
     * @return
     */
    public static SSLSocketFactory getSocketFactory() {
        return com.tencent.iot.hub.device.java.core.util.AsymcSslUtils.getSocketFactory();
    }

    /**
     * 获取自定义CA证书的SSLSocketFactory
     *
     * @param customCA 自定义CA证书
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSocketFactory(String customCA) {
        return com.tencent.iot.hub.device.java.core.util.AsymcSslUtils.getSocketFactory(customCA);
    }

    private static PrivateKey getPrivateKey(InputStream stream, String algorithm) throws IOException,
            GeneralSecurityException {
        PrivateKey key = null;
        boolean isRSAKey = false;

        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        boolean inKey = false;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (!inKey) {
                if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
                    inKey = true;
                    isRSAKey = line.contains("RSA");
                }
                continue;
            } else {
                if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
                    inKey = false;
                    isRSAKey = line.contains("RSA");
                    break;
                }
                builder.append(line);
            }
        }
        KeySpec keySpec = null;
        byte[] encoded = Base64.decode(builder.toString(), Base64.DEFAULT);
        if (isRSAKey) {
            keySpec = getRSAKeySpec(encoded);
        } else {
            keySpec = new PKCS8EncodedKeySpec(encoded);
        }
        KeyFactory kf = KeyFactory.getInstance((algorithm == null) ? "RSA" : algorithm);
        key = kf.generatePrivate(keySpec);

        return key;
    }

    private static RSAPrivateCrtKeySpec getRSAKeySpec(byte[] keyBytes) throws IOException {

        DerParser parser = new DerParser(keyBytes);

        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE)
            throw new IOException("Invalid DER: not a sequence"); //$NON-NLS-1$

        // Parse inside the sequence
        parser = sequence.getParser();

        parser.read(); // Skip version
        BigInteger modulus = parser.read().getInteger();
        BigInteger publicExp = parser.read().getInteger();
        BigInteger privateExp = parser.read().getInteger();
        BigInteger prime1 = parser.read().getInteger();
        BigInteger prime2 = parser.read().getInteger();
        BigInteger exp1 = parser.read().getInteger();
        BigInteger exp2 = parser.read().getInteger();
        BigInteger crtCoef = parser.read().getInteger();

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1,
                exp2, crtCoef);

        return keySpec;
    }
}

