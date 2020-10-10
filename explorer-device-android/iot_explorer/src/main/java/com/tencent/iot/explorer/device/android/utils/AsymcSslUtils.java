package com.tencent.iot.explorer.device.android.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;

import com.tencent.iot.explorer.device.android.device.CA;
import com.tencent.iot.hub.device.java.core.util.Asn1Object;
import com.tencent.iot.hub.device.java.core.util.DerParser;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

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

    /**
     * 获取SSLSocketFactory
     *
     * @return
     */
    public static SSLSocketFactory getSocketFactory() {
        return com.tencent.iot.hub.device.java.core.util.AsymcSslUtils.getSocketFactory();
    }
}

