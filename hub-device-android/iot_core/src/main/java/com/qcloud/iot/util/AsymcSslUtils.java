package com.qcloud.iot.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;

import com.qcloud.iot.device.CA;

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
import java.util.Arrays;
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
        InputStream clientInputStream = null;
        InputStream keyInputStream = null;
        SSLSocketFactory factory = null;

        try {
            clientInputStream = new FileInputStream(new File(clientCrtFileName));
            keyInputStream = new FileInputStream(new File(clientPriKeyFileName));

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
     * 获取双向认证SSLSocketFactory
     *
     * @param clientInput 设备证书文件输入流
     * @param keyInput    设备私钥文件输入流
     * @return
     */
    public static SSLSocketFactory getSocketFactoryByStream(final InputStream clientInput, final InputStream keyInput) {
        Security.addProvider(new BouncyCastleProvider());
        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            TXLog.e(TAG, "getSocketFactory failed, create CertificateFactory error.", e);
        }

        PEMParser parser = null;
        X509Certificate caCert = null;
        X509Certificate clientCert = null;
        PrivateKey privateKey = null;

        // load CA certificate
        {
            ByteArrayInputStream caInput = new ByteArrayInputStream(CA.caCrt.getBytes(Charset.forName("UTF-8")));
            parser = new PEMParser(new InputStreamReader(caInput));
            Object object = null;
            try {
                object = parser.readObject();
            } catch (IOException e) {
                TXLog.e(TAG, "parse CA failed.", e);
                return null;
            }

            if (!(object instanceof X509CertificateHolder)) {
                TXLog.e(TAG, "CA file not X509CertificateHolder.");
                return null;
            }

            X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
            try {
                InputStream caIn = new ByteArrayInputStream(certificateHolder.getEncoded());
                caCert = (X509Certificate) certFactory.generateCertificate(caIn);
                caIn.close();
                parser.close();
            } catch (Exception e) {
                TXLog.e(TAG, "generate CA certtificate failed.", e);
                return null;
            }

        }

        // load client certificate
        {
            parser = new PEMParser(new InputStreamReader(clientInput));
            Object object = null;
            try {
                object = parser.readObject();
            } catch (IOException e) {
                TXLog.e(TAG, "parse Client CRT failed.", e);
                return null;
            }

            if (!(object instanceof X509CertificateHolder)) {
                TXLog.e(TAG, "Client CRT file not X509CertificateHolder.");
                return null;
            }

            X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
            try {
                InputStream clientIn = new ByteArrayInputStream(certificateHolder.getEncoded());
                clientCert = (X509Certificate) certFactory.generateCertificate(clientIn);
                clientIn.close();
                parser.close();
            } catch (Exception e) {
                TXLog.e(TAG, "generate Client certtificate failed.", e);
                return null;
            }
        }

        // load client private key
        {
            try {
                privateKey = getPrivateKey(keyInput, null);
            } catch (Exception e) {
                TXLog.e(TAG, "generate PrivateKey failed.", e);
                return null;
            }
        }

        try {
            // CA certificate is used to authenticate server
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            // client key and certificates are sent to server so it can authenticate us
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certificate", clientCert);
            ks.setKeyEntry("private-key", privateKey, PASSWORD.toCharArray(), new java.security.cert.Certificate[]{clientCert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, PASSWORD.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (Exception e) {
            TXLog.e(TAG, "construct SSLSocketFactory failed.", e);
            return null;
        }

    }

    /**
     * 获取SSLSocketFactory
     *
     * @return
     */
    public static SSLSocketFactory getSocketFactory() {
        Security.addProvider(new BouncyCastleProvider());
        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            TXLog.e(TAG, "getSocketFactory failed, create CertificateFactory error.", e);
        }

        PEMParser parser = null;
        X509Certificate caCert = null;

        // load CA certificate
        {
            ByteArrayInputStream caInput = new ByteArrayInputStream(CA.caCrt.getBytes(Charset.forName("UTF-8")));
            parser = new PEMParser(new InputStreamReader(caInput));
            Object object = null;
            try {
                object = parser.readObject();
            } catch (IOException e) {
                TXLog.e(TAG, "parse CA failed.", e);
                return null;
            }

            if (!(object instanceof X509CertificateHolder)) {
                TXLog.e(TAG, "CA file not X509CertificateHolder.");
                return null;
            }

            X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
            try {
                InputStream caIn = new ByteArrayInputStream(certificateHolder.getEncoded());
                caCert = (X509Certificate) certFactory.generateCertificate(caIn);
                caIn.close();
                parser.close();
            } catch (Exception e) {
                TXLog.e(TAG, "generate CA certtificate failed.", e);
                return null;
            }

        }


        try {
            // CA certificate is used to authenticate server
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);


            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (Exception e) {
            TXLog.e(TAG, "construct SSLSocketFactory failed.", e);
            return null;
        }

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

