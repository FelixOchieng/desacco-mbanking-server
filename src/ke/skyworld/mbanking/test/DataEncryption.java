package ke.skyworld.mbanking.test;

import ke.skyworld.lib.mbanking.utils.Crypto;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DataEncryption {

    private String strKeyAlgorithm = "RSA"; // Supported DSA and RSA
    private String strEncryptionAlgorithm = "RSA/ECB/PKCS1Padding"; //e.g RSA = RSA/ECB/PKCS1Padding & DSA = AES/ECB/PKCS5Padding / AES/CBC/PKCS5Padding

    /*
    Cipher
    The algorithms are specified as transformations in getInstance. Implementations must support the key sizes in parentheses.

    AES/CBC/NoPadding (128)
    AES/CBC/PKCS5Padding (128)
    AES/ECB/NoPadding (128)
    AES/ECB/PKCS5Padding (128)
    AES/GCM/NoPadding (128)
    DES/CBC/NoPadding (56)
    DES/CBC/PKCS5Padding (56)
    DES/ECB/NoPadding (56)
    DES/ECB/PKCS5Padding (56)
    DESede/CBC/NoPadding (168)
    DESede/CBC/PKCS5Padding (168)
    DESede/ECB/NoPadding (168)
    DESede/ECB/PKCS5Padding (168)
    RSA/ECB/PKCS1Padding (1024, 2048)
    RSA/ECB/OAEPWithSHA-1AndMGF1Padding (1024, 2048)
    RSA/ECB/OAEPWithSHA-256AndMGF1Padding (1024, 2048)
     */

    public DataEncryption(){
        try {
            this.setAlgorithms(this.strKeyAlgorithm, this.strEncryptionAlgorithm);
        }catch (Exception e){
            System.err.println("DataEncryption.DataEncryption(): " + e.getMessage());
        }
    }

    public DataEncryption(String theKeyAlgorithm, String theEncryptionAlgorithm){
        try {
            this.setAlgorithms(theKeyAlgorithm, theEncryptionAlgorithm);
        }catch (Exception e){
            System.err.println("DataEncryption.DataEncryption(): " + e.getMessage());
        }
    }

    private void setAlgorithms(String theKeyAlgorithm, String theEncryptionAlgorithm) {
        try {
            this.strKeyAlgorithm = theKeyAlgorithm;
            this.strEncryptionAlgorithm = theEncryptionAlgorithm;
        }catch (Exception e){
            System.err.println("DataEncryption.setAlgorithms(): " + e.getMessage());
        }
    }

    public PublicKey getPublicKey(String thePublicKey){
        PublicKey publicKey = null;
        X509EncodedKeySpec keySpec = null;
        try{
            keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(thePublicKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance(this.strKeyAlgorithm);
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (Exception e) {
            System.err.println("DataEncryption.getPublicKey(): " + e.getMessage());
        }finally {
            keySpec = null;
        }
        return publicKey;
    }

    public PrivateKey getPrivateKey(String thePrivateKey){
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = null;
        try {
            keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(thePrivateKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance(this.strKeyAlgorithm);
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            System.err.println("DataEncryption.getPrivateKey(): " + e.getMessage());
        }finally {
            keySpec = null;
        }

        return privateKey;
    }

    public String encrypt(String thePublicKey, String theData) {
        String strEncryptedData = "";
        try {
            Cipher cipher = Cipher.getInstance(this.strEncryptionAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(thePublicKey));
            byte[] encryptedDataBytes =  cipher.doFinal(theData.getBytes());
            strEncryptedData =  new String( Base64.getEncoder().encode( encryptedDataBytes ) ) ;
        } catch (Exception e) {
            System.err.println("DataEncryption.encrypt(): " + e.getMessage());
        }finally {

        }

        return strEncryptedData;
    }

    public String decrypt(String thePrivateKey, String theEncryptedData) {
        String strData = "";
        try {
            byte[] encryptedDataBytes = Base64.getDecoder().decode( theEncryptedData );
            Cipher cipher = Cipher.getInstance(this.strEncryptionAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(thePrivateKey));
            strData = new String ( cipher.doFinal(encryptedDataBytes) ) ;
        } catch (Exception e) {
            System.err.println("DataEncryption.decrypt(): " + e.getMessage());
        }finally {

        }

        return strData;
    }

    public static void main(String[] args) {
        try {
            //DO NOT USE THESE KEYS FOR PRODUCTION - TESTING ONLY. GENERATE KEYS USING DigitalKeyPairGenerator.

            //RSA KEYS
            //String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCLZjAxRepA++MPOkIAYFDzVQn0CZFFsgJvNNvMxv/3VGUhT4wgJRi7wWW9bcOfEx+KUrOJ5Ntk+SwRTE8fHzNqeHY1jMZv1cGDPGd5HOt4lTeAeAuc5w2xKdYcA9nBy5yaF9ST3fsRtH3W6Yd8yKTCtr0VRrK5soe/FhcNj3OreQIDAQAB";
            //String privateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAItmMDFF6kD74w86QgBgUPNVCfQJkUWyAm8028zG//dUZSFPjCAlGLvBZb1tw58TH4pSs4nk22T5LBFMTx8fM2p4djWMxm/VwYM8Z3kc63iVN4B4C5znDbEp1hwD2cHLnJoX1JPd+xG0fdbph3zIpMK2vRVGsrmyh78WFw2Pc6t5AgMBAAECgYAV4XlpEo9dmewfJMbdZkDuoQeJ7cKGzRVCvWpO72GYQgAJxYG6Pfhu6EF/BW6yVn8Z6DdUSr8BT3dSU8cLnIsmTI7V6Qahj3+mHVQ0m7siCOeQ4v1unCai9eBd5xBP47O0ypwgowjoHKEbfjXqmPVmai81OMotAqYEtjs3fKcOcQJBANLP7n+fhasi/53Z19ZuqOx1hZoU0xT4BxiXxwk3SJ7eZqqu/iEctiIz2QDoJiY8vxACphbrcQs31XZ9k+EaNHUCQQCpR4z1fz6gJkaTOOU9b4DLlTczg22HmGlToW6//5jaoUV17vq4YCiEcnP72VO50uoN3UOpVei/hnpnFe+klKp1AkBtP5qeguH3pWSIvjsPgjuChwjcp8wYAs1Snl1kVkUJJ8JW0+cY69MreGkOAkC68iIlvump2Qu1P5MdG8kGD9l9AkBwtnCYAhq73eB+JJMW/gh/BaLUzP0AHS58fe/VLaYkZN9wMCiG3Zf84Ixzs/g4scQgaSSlOsuXwz37J7MCHe8FAkAaeLPMXbhGwsgdYyGjGaji/LTalJ4g4XWsQ2HkIH2Y7DXW2ptivfGHicvO/JhiFyQf7b/o11ZSr8VVRstTiziE";

            String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgI4n+ZCWsalONbDipWZUPVNXAPBEk7/gW7pKxTlpy0SHTvufQr2Sqp5xt+4iC5pUVCozjLa5iCAG77NbJy2b2ccmmV0yLkyFVh+0SyMUw7HyG5l38ydT28Qgb5fq/KXLrNWh7rziQo1PATh+ah1RNUqQ8K0+/f0YUFSI6brGC7v/At8YcDGgAK5Wc8pNMNw8KwUq4ydmfJrB4blOTUXmFLR+8TBTgIaSq17aQGKonDIDn8Fpo0W19l64ZtxufBdt9QUJQVTbaZlwEF8CgoT0YVNNA1fOe7WBw5v4WTKwiOYlhGIHrbvQoc2UTkCxlYETlOoNzoydvdzETlvCTzD4OQIDAQAB";
            String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCAjif5kJaxqU41sOKlZlQ9U1cA8ESTv+BbukrFOWnLRIdO+59CvZKqnnG37iILmlRUKjOMtrmIIAbvs1snLZvZxyaZXTIuTIVWH7RLIxTDsfIbmXfzJ1PbxCBvl+r8pcus1aHuvOJCjU8BOH5qHVE1SpDwrT79/RhQVIjpusYLu/8C3xhwMaAArlZzyk0w3DwrBSrjJ2Z8msHhuU5NReYUtH7xMFOAhpKrXtpAYqicMgOfwWmjRbX2Xrhm3G58F231BQlBVNtpmXAQXwKChPRhU00DV857tYHDm/hZMrCI5iWEYgetu9ChzZROQLGVgROU6g3OjJ293MROW8JPMPg5AgMBAAECggEAKC9Ho6Vk1ag8wF/ImTdBgeH5QlvYs+64rTOwh7IItfd37GKPSAeGPztEBOC1V7coQR7n+kZr/Hc2c9s21SpWm167XBlbxEk7LM7ARvRyWzHWonX4ntNeTUYjdX6hf9Q7tI0bD0uP1onhdlo4eecsKWJjqDgfhrmEzid+nME7gxMRRpcsOTXhkYrsAgOXLkL8GQUZBIou2NhsTwmKzNkVz/gc4WD6b20LnS6TJdjHvjfy1DT+AEd3bdWyYUWsoyZQAio1MTpbPxvpGh49OOp60QenSilGYM2iLzERyD3PNc3Hx1bl+56Wq1OVD7phnNjsnX47hIo08EAm8VXHLq7VuQKBgQDD0nJYfiMrgMqBVV605Apo3Q2Cbu/DfqG0aZV3/GR3AD5+q5/b2llgpRvbLrgE7EmvvFddc73D41dMXzpdwtm9/+SU0jci2Qmmvv/UTqcXbvCWfh7JkO8rP8c19chu2r+YR2IMx48OytmQEsV/0H7mbuf39unaT2yySjTA9RGajwKBgQCoD8D3BkyaJU904qSQNEy8IuPdBKKQIn8MhrzRwyxtvfy52UXXcOYNod0fQRqDAly7AZUoSVrplebNygAdKN8TWPHgYmzqtOlbhlCEnOO9XLk7zwTgs6QJIJgwnVprBeMzoCMtoLaJyDq3SdvmOfFJvCDp5UKfgt4/iMeNQVrEtwKBgA2pNMjvo8x5I6d6KS09a2x9X1/mFVvyDZ3kb8T7GpcisTltB63ywaF4Y0UbMUNGqK1V2lJurKJpzcFKM2wvF7mljHDFaYtI0N+NG5PYGNgNqUMWcVdmgQjnXiJpjx4MrKkW8cQqd9R0WlEuvhB4nyG8QvqNgyrzt4WIn72GW0AJAoGAacfQqysZ2AQX6PgmoGVqzxge2CRstdAgq5+7BUSVmFV21vt8zEfRZU82QM/XghJgj4xFd+AECvZBGdJFFBV/o0veol8RMwG/x83YrD+b0LqmFJEO/ufTHbOYVzETkj1YbkwjGDsJ6dtPqcIhWN2rk7+H7/BPaNsUTGUpRS2Xli0CgYBv1IITYDpWFKSA7ntEV8AYC0IA7Qs4WXuM8sNGSETbUoetuzShjWwLLKKIoFYH9GyDICGhfnHBQbeeo6sFWiVwi1yBQoPaZfAf+26ZvxZsJTsgCKB4efzQ7DuSlSBekh/wbPA2rrNdeaT7M4cBnm+rccPGZJrTecd8eWqEvY99bw==";

            //ENCRYPTION
            //DataEncryption theDSAUtil = new DataEncryption(); //- Use Defaults

            String strKeyAlgorithm = "RSA"; // Supported DSA and RSA
            String strEncryptionAlgorithm = "RSA/ECB/PKCS1Padding"; //e.g RSA/ECB/PKCS1Padding DSA = AES/ECB/PKCS5Padding,
            DataEncryption theDSAUtil = new DataEncryption(strKeyAlgorithm, strEncryptionAlgorithm);

            /*String strData = "123456";
            String encryptedStringFromJava = theDSAUtil.encrypt(publicKey, strData);
            System.out.println("Encrypted: " + new String( Base64.getDecoder().decode(encryptedStringFromJava) ));
            System.out.println("Encrypted Base64: " + encryptedStringFromJava);*/

            /*Crypto crypto = new Crypto();
            String strHashed = crypto.hash("SHA-512", crypto.hash("MD5", encryptedString));
            System.out.println("Hashed: "+strHashed);*/

            String encryptedStringFromOutside = "HlT/CRBNmRUy+gruevDog2yaU6fnVV++DTz7Sv8c/1F67jxkzDr2SqxM+QrCBI8fdTss6hEDJG1TeuWtkyqNAPT/u0ZheHI8JQuXfFkOFcANO3kisSMKLIQ2hgOMY6lCrENgL5SgoFNYVUCFrNru6MXFjvfPuiD4mhQbwO+Zqv5ldhlLB8QAPcbGYcPBzWBqYxjZYNLC2jshYKysKxZLJGtZvQ+dvEiuzvFsjE8MBjcYX3QpiHbQWWdX6RkfHIdjbBfjFQ3V0Y/T97Pk8DdYS971tv6WZAEQJbj7DxU/l0pFGzhHconaqv1X4GWEgvN/Dqld7Z1jFzkUQhBem0tafA==";

            String decryptedString = theDSAUtil.decrypt(privateKey, encryptedStringFromOutside);
            System.out.println("Clear Text: " + decryptedString);

            System.out.println();
            System.out.println();


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }
}