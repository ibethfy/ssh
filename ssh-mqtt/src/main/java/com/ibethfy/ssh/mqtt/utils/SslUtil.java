package com.ibethfy.ssh.mqtt.utils;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SslUtil {

    /**
     * 创建 SSLSocketFactory 工厂
     *
     * @param caCrtFile 服务端 CA 证书
     * @param crtFile   客户端 CRT 文件
     * @param keyFile   客户端 Key 文件
     * @param password  SSL 密码，随机
     * @return {@link SSLSocketFactory}
     * @throws Exception 异常
     */
    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile, final String password) throws Exception {
        InputStream caInputStream = null;
        InputStream crtInputStream = null;
        InputStream keyInputStream = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // load CA certificate
            caInputStream = new ClassPathResource(caCrtFile).getInputStream();
            X509Certificate caCert = null;
            while (caInputStream.available() > 0) {
                caCert = (X509Certificate) cf.generateCertificate(caInputStream);
            }
            // load client certificate
            crtInputStream = new ClassPathResource(crtFile).getInputStream();
            X509Certificate cert = null;
            while (crtInputStream.available() > 0) {
                cert = (X509Certificate) cf.generateCertificate(crtInputStream);
            }

            // load client private key
            keyInputStream = new ClassPathResource(keyFile).getInputStream();
            PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream));
            Object object = pemParser.readObject();
            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
//            KeyPair key;
            PrivateKey privateKey;
            if (object instanceof PEMEncryptedKeyPair) {
                System.out.println("Encrypted key - we will use provided password");
                privateKey = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
//                privateKey = key.getPrivate();
            } else if (object instanceof PrivateKeyInfo) {
                System.out.println("only private key");
                privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                System.out.println("Unencrypted key - no password needed");
                privateKey = converter.getKeyPair((PEMKeyPair) object).getPrivate();
//                privateKey = key.getPrivate();
            }
            pemParser.close();

            // CA certificate is used to authenticate server
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(caKs);

            // client key and certificates are sent to server so it can authenticate
            // us
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certificate", cert);
            ks.setKeyEntry("private-key", privateKey, password.toCharArray(), new java.security.cert.Certificate[]{cert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return context.getSocketFactory();

        } finally {
            if (null != caInputStream) {
                caInputStream.close();
            }
            if (null != crtInputStream) {
                crtInputStream.close();
            }
            if (null != keyInputStream) {
                keyInputStream.close();
            }
        }
    }

}
