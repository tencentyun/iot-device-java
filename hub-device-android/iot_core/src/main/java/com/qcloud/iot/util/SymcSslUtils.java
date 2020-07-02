package com.qcloud.iot.util;

import com.qcloud.iot.device.CA;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

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



public class SymcSslUtils {

    public static final String TAG = "iot.SymcSslUtils";

    private static String PASSWORD = String.valueOf(new Random(System.currentTimeMillis()).nextInt());

    public static SSLSocketFactory getSocketFactory(String psk) {
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
            ByteArrayInputStream caInput = new ByteArrayInputStream(CA.caCrt.getBytes());
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
            TXLog.e(TAG, "construct SSLSocketFactory failed.", e);
            return null;
        }
    }

}
