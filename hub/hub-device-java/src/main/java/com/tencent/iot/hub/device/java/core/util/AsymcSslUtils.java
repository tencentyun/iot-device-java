package com.tencent.iot.hub.device.java.core.util;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.device.CA;
import com.tencent.iot.hub.device.java.utils.Loggor;

/**
 * 异步 SSL 工具类
 */
public class AsymcSslUtils {

    /**
     * 类标记
     */
    public static final String TAG = AsymcSslUtils.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(AsymcSslUtils.class);
    static { Loggor.setLogger(logger); }
    private static String PASSWORD = String.valueOf(new Random(System.currentTimeMillis()).nextInt());

    /**
     * 读取 resouce 文件证书文件及 Key 文件存放在 Android asset 目录
     * 下，通过 AssetManager 读取文件内容获取输入流，通过输入流构造双向
     * 认证 SSLSocketFactory
     *
     * @param clientCrtFileName 客户端证书文件名
     * @param clientPriKeyFileName 客户端私钥文件名
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactoryByAssetsFile(final String clientCrtFileName, final String clientPriKeyFileName) {
        SSLSocketFactory factory = null;
        InputStream clientInputStream = null;
        InputStream keyInputStream = null;
        clientInputStream=AsymcSslUtils.class.getClassLoader().getResourceAsStream(clientCrtFileName);
        keyInputStream=AsymcSslUtils.class.getClassLoader().getResourceAsStream(clientPriKeyFileName);
        factory = getSocketFactoryByStream(clientInputStream, keyInputStream);

        return factory;
    }

    /**
     * 证书文件及 Key 文件存放在 Android 本地存储中，通过 FileInputStream
     * 读取文件内容输入流通过输入流解析构造双向认证 SSLSocketFactory
     *
     * @param clientCrtFileName 客户端证书文件名，要求全路径
     * @param clientPriKeyFileName 客户端私钥文件名，要求全路径
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactoryByFile(final String clientCrtFileName, final String clientPriKeyFileName) {
        InputStream clientInputStream = null;
        InputStream keyInputStream = null;
        SSLSocketFactory factory = null;

        try {
            clientInputStream = new FileInputStream(new File(clientCrtFileName));
            keyInputStream = new FileInputStream(new File(clientPriKeyFileName));

            factory = getSocketFactoryByStream(clientInputStream, keyInputStream);;
        } catch (FileNotFoundException e) {
            Loggor.error(TAG, "getSocketFactory failed, cannot open CRT Files. " + e);
        } finally {
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
     * 获取双向认证 SSLSocketFactory
     *
     * @param clientInput 设备证书文件输入流
     * @param keyInput 设备私钥文件输入流
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactoryByStream(final InputStream clientInput, final InputStream keyInput) {
        return getSocketFactoryByStream(clientInput, keyInput, null);
    }

    /**
     * 获取双向认证 SSLSocketFactory
     *
     * @param clientInput 设备证书文件输入流
     * @param keyInput 设备私钥文件输入流
     * @param customCA 自定义 CA 证书
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactoryByStream(final InputStream clientInput, final InputStream keyInput, String customCA) {
        Security.addProvider(new BouncyCastleProvider());
        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            Loggor.error(TAG, "getSocketFactory failed, create CertificateFactory error. " + e);
        }

        PEMParser parser = null;
        X509Certificate caCert = null;
        X509Certificate clientCert = null;
        PrivateKey privateKey = null;

        // load CA certificate
        {
            ByteArrayInputStream caInput = null;
            if (customCA != null && customCA.length() > 0) {
                caInput = new ByteArrayInputStream(customCA.getBytes(Charset.forName("UTF-8")));
            } else {
                caInput = new ByteArrayInputStream(CA.caCrt.getBytes(Charset.forName("UTF-8")));
            }
            parser = new PEMParser(new InputStreamReader(caInput));
            Object object = null;
            try {
                object = parser.readObject();
            } catch (IOException e) {
                Loggor.error(TAG, "parse CA failed." + e);
                return null;
            }

            if (!(object instanceof X509CertificateHolder)) {
                Loggor.error(TAG, "CA file not X509CertificateHolder.");
                return null;
            }

            X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
            try {
                InputStream caIn = new ByteArrayInputStream(certificateHolder.getEncoded());
                caCert = (X509Certificate) certFactory.generateCertificate(caIn);
                caIn.close();
                parser.close();
            } catch (Exception e) {
                Loggor.error(TAG, "generate CA certtificate failed. " + e);
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
                Loggor.error(TAG, "parse Client CRT failed. " + e);
                return null;
            }

            if (!(object instanceof X509CertificateHolder)) {
                Loggor.error(TAG, "Client CRT file not X509CertificateHolder.");
                return null;
            }

            X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
            try {
                InputStream clientIn = new ByteArrayInputStream(certificateHolder.getEncoded());
                clientCert = (X509Certificate) certFactory.generateCertificate(clientIn);
                clientIn.close();
                parser.close();
            } catch (Exception e) {
                Loggor.error(TAG, "generate Client certtificate failed. " + e);
                return null;
            }
        }

        // load client private key
        {
            try {
                privateKey = getPrivateKey(keyInput, null);
            } catch (Exception e) {
                Loggor.error(TAG,  "generate PrivateKey failed. " + e);
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
            Loggor.error(TAG,  "construct SSLSocketFactory failed. " + e);
            return null;
        }

    }

    /**
     * 获取默认 CA 证书的 SSLSocketFactory
     *
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactory() {
        return getSocketFactory(null);
    }

    /**
     * 获取自定义 CA 证书的 SSLSocketFactory
     *
     * @param customCA 自定义 CA 证书
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactory(String customCA) {
        Security.addProvider(new BouncyCastleProvider());
        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            Loggor.error(TAG, "getSocketFactory failed, create CertificateFactory error. " + e);
        }

        PEMParser parser = null;
        X509Certificate caCert = null;

        // load CA certificate
        {
            ByteArrayInputStream caInput = null;
            if (customCA != null && customCA.length() > 0) {
                caInput = new ByteArrayInputStream(customCA.getBytes(Charset.forName("UTF-8")));
            } else {
                caInput = new ByteArrayInputStream(CA.caCrt.getBytes(Charset.forName("UTF-8")));
            }
            parser = new PEMParser(new InputStreamReader(caInput));
            Object object = null;
            try {
                object = parser.readObject();
            } catch (IOException e) {
                Loggor.error(TAG, "parse CA failed. " + e);
                return null;
            }

            if (!(object instanceof X509CertificateHolder)) {
                Loggor.error(TAG, "CA file not X509CertificateHolder.");
                return null;
            }

            X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
            try {
                InputStream caIn = new ByteArrayInputStream(certificateHolder.getEncoded());
                caCert = (X509Certificate) certFactory.generateCertificate(caIn);
                caIn.close();
                parser.close();
            } catch (Exception e) {
                Loggor.error(TAG, "generate CA certtificate failed. " + e);
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
            Loggor.error(TAG, "construct SSLSocketFactory failed." + e);
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

    /**
     * 从 PEM 格式的公钥字符串中提取 rsa 公钥的前 24 字节
     *
     * @param pemStr PEM 格式的公钥字符串
     * @return rsa 公钥的前 24 字节
     */
    public static byte[] getRSAPublicKeyFromPem(String pemStr) {
        byte[] ret = new byte[24];
        try {
            PemReader pemReader = new PemReader(new StringReader(pemStr));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
            RSAPublicKey pubKey = (RSAPublicKey) factory.generatePublic(pubKeySpec);
            byte[] pubKeyBytes = pubKey.getModulus().toByteArray();
            System.arraycopy(pubKeyBytes, 1, ret, 0, ret.length);
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return ret;
    }
}

