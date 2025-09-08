package ke.skyworld.mbanking.test;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class DigitalKeyPairGenerator {
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    private String strKeyAlgorithm = "RSA"; //Supports RSA and DSA
    private int intKeySize = 2048;
    /*
    DiffieHellman (1024, 2048, 4096)
    DSA (1024, 2048)
    RSA(1024, 2048, 4096)
    */

    public DigitalKeyPairGenerator() {
        try {
            this.setKeyPairGenerator(this.strKeyAlgorithm, this.intKeySize);
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.DigitalKeyPairGenerator(): " + e.getMessage());
        }
    }

    public DigitalKeyPairGenerator(String theKeyAlgorithm) {
        try {
            this.setKeyPairGenerator(theKeyAlgorithm, this.intKeySize);
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.DigitalKeyPairGenerator(theKeyAlgorithm): " + e.getMessage());
        }
    }

    public DigitalKeyPairGenerator(int theKeySize) {
        try {
            this.setKeyPairGenerator(this.strKeyAlgorithm, theKeySize);
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.DigitalKeyPairGenerator(theKeySize): " + e.getMessage());
        }
    }

    public DigitalKeyPairGenerator(String theKeyAlgorithm, int theKeySize) {
        try {
            setKeyPairGenerator(theKeyAlgorithm, theKeySize);
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.DigitalKeyPairGenerator(theKeyAlgorithm, theKeySize): " + e.getMessage());
        }
    }

    private void setKeyPairGenerator(String theKeyAlgorithm, int theKeySize) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(theKeyAlgorithm);
            keyGen.initialize(theKeySize);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.setKeyPairGenerator(): " + e.getMessage());
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getPrivateKeyString() {
        String strPrivateKey = "";
        try {
            if(privateKey!= null){ strPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());}
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.getPrivateKeyString(): " + e.getMessage());
        }
        return strPrivateKey;
    }

    public String getPublicKeyString() {
        String strPublicKey = "";
        try {
            if(publicKey!= null){ strPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());}
        }catch (Exception e){
            System.err.println("DigitalKeyPairGenerator.getPublicKeyString(): " + e.getMessage());
        }
        return strPublicKey;
    }

    public String getPrivateKeyXmlString() {
        String privateKeyXml = "";
        try {
            if (privateKey != null) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPrivateCrtKeySpec privateKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateCrtKeySpec.class);

                String modulusBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getModulus().toByteArray());
                String publicExponentBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getPublicExponent().toByteArray());
                String privateExponentBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getPrivateExponent().toByteArray());
                String pBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getPrimeP().toByteArray());
                String qBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getPrimeQ().toByteArray());
                String dpBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getPrimeExponentP().toByteArray());
                String dqBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getPrimeExponentQ().toByteArray());
                String inverseQBase64 = Base64.getEncoder().encodeToString(privateKeySpec.getCrtCoefficient().toByteArray());

                privateKeyXml = "<RSAKeyValue>" +
                        "<Modulus>" + modulusBase64 + "</Modulus>" +
                        "<Exponent>" + publicExponentBase64 + "</Exponent>" +
                        "<P>" + pBase64 + "</P>" +
                        "<Q>" + qBase64 + "</Q>" +
                        "<DP>" + dpBase64 + "</DP>" +
                        "<DQ>" + dqBase64 + "</DQ>" +
                        "<InverseQ>" + inverseQBase64 + "</InverseQ>" +
                        "<D>" + privateExponentBase64 + "</D>" +
                        "</RSAKeyValue>";
            }
        } catch (Exception e) {
            System.err.println("Error in getPrivateKeyXmlString(): " + e.getMessage());
        }
        return privateKeyXml;
    }

    public String getPublicKeyXmlString() {
        String publicKeyXml = "";
        try {
            if (publicKey != null) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);

                BigInteger modulus = publicKeySpec.getModulus();
                BigInteger publicExponent = publicKeySpec.getPublicExponent();

                String modulusBase64 = Base64.getEncoder().encodeToString(modulus.toByteArray());
                String publicExponentBase64 = Base64.getEncoder().encodeToString(publicExponent.toByteArray());

                publicKeyXml = "<RSAKeyValue>" +
                        "<Modulus>" + modulusBase64 + "</Modulus>" +
                        "<Exponent>" + publicExponentBase64 + "</Exponent>" +
                        "</RSAKeyValue>";
            }
        } catch (Exception e) {
            System.err.println("Error in getPublicKeyXmlString(): " + e.getMessage());
        }
        return publicKeyXml;
    }

    public static void main(String[] args) {
        //KeySize  = 1024, 2048 , 4096 etc
        //Algorithm = RSA (Ron Rivest, Adi Shamir, and Leonard Adleman) or DSA (Digital Signature Algorithm)
        DigitalKeyPairGenerator keyPairGenerator = new DigitalKeyPairGenerator("RSA", 1024);

        System.out.println("Public KEY\n");
        System.out.println( keyPairGenerator.getPublicKeyString());
        System.out.println("\n-------------------------------------------------------------------------------------");
        System.out.println( keyPairGenerator.getPublicKeyXmlString());
        System.out.println("\n*************************************************************************************");
        System.out.println("Private KEY\n");
        System.out.println( keyPairGenerator.getPrivateKeyString());
        System.out.println("\n-------------------------------------------------------------------------------------");
        System.out.println( keyPairGenerator.getPrivateKeyXmlString());
    }
}