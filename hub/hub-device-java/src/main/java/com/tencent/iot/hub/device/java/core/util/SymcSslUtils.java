package com.tencent.iot.hub.device.java.core.util;

import com.tencent.iot.hub.device.java.core.device.CA;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * SSL 工具类
 */
public class SymcSslUtils {

    public static final String TAG = SymcSslUtils.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(SymcSslUtils.class);
    static { Loggor.setLogger(logger); }

    private static String PASSWORD = String.valueOf(new Random(System.currentTimeMillis()).nextInt());

    /**
     * 获取 socket SSL
     * @param psk 密钥
     * @return {@link SSLSocketFactory}
     */
    public static SSLSocketFactory getSocketFactory(String psk) {
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
            ByteArrayInputStream caInput = new ByteArrayInputStream(CA.caCrt.getBytes());
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

        try {
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            InputStream pskInput = new ByteArrayInputStream(psk.getBytes());
            KeyStore pskStore = KeyStore.getInstance(KeyStore.getDefaultType());
            pskStore.load(pskInput, null);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(pskStore, PASSWORD.toCharArray());

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return context.getSocketFactory();

        } catch (Exception e) {
            Loggor.error(TAG, "construct SSLSocketFactory failed. " + e);
            return null;
        }
    }

}
