package ke.skyworld.mbanking.test;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DigitalSignature {

    private String strKeyAlgorithm = "RSA"; // Supported DSA and RSA
    private String strHashAlgorithm = "SHA256withRSA"; //e.g DSA =  SHA1withDSA, SHA224withDSA, SHA256withDSA, SHA384withDSA, and SHA512withDSA
                                                        // RSA = SHA256withRSA
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

    public DigitalSignature(){
        try {
            this.setAlgorithms(this.strKeyAlgorithm, this.strHashAlgorithm);
        }catch (Exception e){
            System.err.println("DataEncryption.DataEncryption(): " + e.getMessage());
        }
    }

    public DigitalSignature(String theKeyAlgorithm, String theHashAlgorithm){
        try {
            this.setAlgorithms(theKeyAlgorithm, theHashAlgorithm);
        }catch (Exception e){
            System.err.println("DataEncryption.DataEncryption(): " + e.getMessage());
        }
    }

    private void setAlgorithms(String theKeyAlgorithm, String theHashAlgorithm) {
        try {
            this.strKeyAlgorithm = theKeyAlgorithm;
            this.strHashAlgorithm = theHashAlgorithm;
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

    public String createSignature(String thePrivateKey, String theData) {
        String strSignature = "";
        try {
            PrivateKey privateKey = getPrivateKey(thePrivateKey);
            //Creating a Signature object
            Signature sign = Signature.getInstance(this.strHashAlgorithm);

            //Initialize the signature
            sign.initSign(privateKey);

            byte[] bytes = theData.getBytes();

            //Adding data to the signature
            sign.update(bytes);

            //Calculating the signature
            byte[] signature = sign.sign();

            strSignature =  new String(Base64.getEncoder().encode(signature));
        }catch (Exception e){
            System.err.println(e.getMessage());
        }finally {

        }

        return strSignature;
    }

    public boolean verifySignature(String thePublicKey, String theSignature, String theData){
        boolean bSignatureVeified = false;
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(theSignature);
            PublicKey publicKey = getPublicKey(thePublicKey);

            //Creating a Signature object
            Signature sign = Signature.getInstance(this.strHashAlgorithm);

            //Initializing the signature
            sign.initVerify(publicKey);
            sign.update(theData.getBytes());

            //Verifying the signature
            bSignatureVeified = sign.verify(signatureBytes);

        }catch (Exception e){
            System.err.println(e.getMessage());
        }finally {

        }

        return bSignatureVeified;
    }

    public static void main(String[] args) {
        try {
            //DO NOT USE THESE KEYS FOR PRODUCTION - TESTING ONLY. GENERATE KETS USING DigitalKeyPairGenerator.


            //DSA KEYS
            String publicKey = "MIIDQjCCAjUGByqGSM44BAEwggIoAoIBAQCPeTXZuarpv6vtiHrPSVG28y7FnjuvNxjo6sSWHz79NgbnQ1GpxBgzObgJ58KuHFObp0dbhdARrbi0eYd1SYRpXKwOjxSzNggooi/6JxEKPWKpk0U0CaD+aWxGWPhL3SCBnDcJoBBXsZWtzQAjPbpUhLYpH51kjviDRIZ3l5zsBLQ0pqwudemYXeI9sCkvwRGMn/qdgYHnM423krcw17njSVkvaAmYchU5Feo9a4tGU8YzRY+AOzKkwuDycpAlbk4/ijsIOKHEUOThjBopo33fXqFD3ktm/wSQPtXPFiPhWNSHxgjpfyEc2B3KI8tuOAdl+CLjQr5ITAV2OTlgHNZnAh0AuvaWpoV499/e5/pnyXfHhe8ysjO65YDAvNVpXQKCAQAWplxYIEhQcE51AqOXVwQNNNo6NHjBVNTkpcAtJC7gT5bmHkvQkEq9rI837rHgnzGC0jyQQ8tkL4gAQWDt+coJsyB2p5wypifyRz6Rh5uixOdEvSCBVEy1W4AsNo0fqD7UielOD6BojjJCilx4xHjGjQUntxyaOrsLC+EsRGiWOefTznTbEBplqiuH9kxoJts+xy9LVZmDS7TtsC98kOmkltOlXVNb6/xF1PYZ9j897buHOSXC8iTgdzEpbaiH7B5HSPh++1/et1SEMWsiMt7lU92vAhErDR8C2jCXMiT+J67ai51LKSLZuovjntnhA6Y8UoELxoi34u1DFuHvF9veA4IBBQACggEAWJEOK3ONCFSqFkz0EyoGFvJLZGhKGn9iyp161PCPk17aVGnM0unnTI8bkCf1u68H6bUdeBB1wf2KKnlcOR1fQSPGWZ2kMRq4LCCaavXP2Avu8ri46JH4l/F9YTjK0TdzhIr/s5JSK9MdaIgviURYH8DJSGtHA1QiDZu6Hg+RECviZXyWkQ8rTR0HgJLGXeZ74IcsuDrbm18BD2oGfLl/OkLc4haaPd6frrRqjXy5i/UvLoQt8E1QArknM8E+nfPuWJONGMpeb94tK0mxpL2rR3FR23aMToV7L89DQK3IBvQcfru5uEV7oUh9L1uvdRjKeh4/sTKhmLki9G68HzFUlg==";
            String privateKey = "MIICXQIBADCCAjUGByqGSM44BAEwggIoAoIBAQCPeTXZuarpv6vtiHrPSVG28y7FnjuvNxjo6sSWHz79NgbnQ1GpxBgzObgJ58KuHFObp0dbhdARrbi0eYd1SYRpXKwOjxSzNggooi/6JxEKPWKpk0U0CaD+aWxGWPhL3SCBnDcJoBBXsZWtzQAjPbpUhLYpH51kjviDRIZ3l5zsBLQ0pqwudemYXeI9sCkvwRGMn/qdgYHnM423krcw17njSVkvaAmYchU5Feo9a4tGU8YzRY+AOzKkwuDycpAlbk4/ijsIOKHEUOThjBopo33fXqFD3ktm/wSQPtXPFiPhWNSHxgjpfyEc2B3KI8tuOAdl+CLjQr5ITAV2OTlgHNZnAh0AuvaWpoV499/e5/pnyXfHhe8ysjO65YDAvNVpXQKCAQAWplxYIEhQcE51AqOXVwQNNNo6NHjBVNTkpcAtJC7gT5bmHkvQkEq9rI837rHgnzGC0jyQQ8tkL4gAQWDt+coJsyB2p5wypifyRz6Rh5uixOdEvSCBVEy1W4AsNo0fqD7UielOD6BojjJCilx4xHjGjQUntxyaOrsLC+EsRGiWOefTznTbEBplqiuH9kxoJts+xy9LVZmDS7TtsC98kOmkltOlXVNb6/xF1PYZ9j897buHOSXC8iTgdzEpbaiH7B5HSPh++1/et1SEMWsiMt7lU92vAhErDR8C2jCXMiT+J67ai51LKSLZuovjntnhA6Y8UoELxoi34u1DFuHvF9veBB8CHQCYElO9VzCIAov1ua6gCaDzmDrGnzROGB/c9Fui";

            /*
            //RSA KEYS
            String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuTpQSnFbK9Kcw/HrBrujoBMFZuV2Tk2lCxlVDLwZUo95RB7sI+SNSxS5l1Rmp6ej89Rcvq8bnOSNkBwsfLRwy9Ho2hdcFLFBnE9qUSDtOaF7unRIrdp+7c20gEvBwZbOGlZ/hvNnp+X6f5Ih9SkIDhkyb5tDb5BVsXzotmTvtjTF7Pn1mHCevqXStpGAyg5gvleNdtnwZRz+NiGE52Jrh+QbNwuIk+J2gjZLIm9gaL47znziRD87R3nwym7PtRkJmUheucfCqwltIiCVKUiGN9a/N/B1m5pkpSpFxY8fNGluyzUeKN1WOfQ4ppKkgnamZSSe1jpnrem6ud+57FWLJwIDAQAB";
            String privateKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQC5OlBKcVsr0pzD8esGu6OgEwVm5XZOTaULGVUMvBlSj3lEHuwj5I1LFLmXVGanp6Pz1Fy+rxuc5I2QHCx8tHDL0ejaF1wUsUGcT2pRIO05oXu6dEit2n7tzbSAS8HBls4aVn+G82en5fp/kiH1KQgOGTJvm0NvkFWxfOi2ZO+2NMXs+fWYcJ6+pdK2kYDKDmC+V4122fBlHP42IYTnYmuH5Bs3C4iT4naCNksib2BovjvOfOJEPztHefDKbs+1GQmZSF65x8KrCW0iIJUpSIY31r838HWbmmSlKkXFjx80aW7LNR4o3VY59DimkqSCdqZlJJ7WOmet6bq537nsVYsnAgMBAAECggEBAInoKsNiJnk4M9OHmG7I4Yha4Ri9GxotoQXkdHgoy8m2XJklVLdcgP43jf5nro6AuAbgQ7UM9sgXOnbjIxQt3BVEUdOetMYLO0g/sG56z2SrIE2wKjSWiw5Oq2ciAD48I3Nkl0pIixJsndbBXFsFk6O7ituhg9HaapLc5v51pCvJUyHHUoZAs25A414FSZGtdFNgB4589US8Lbzk0bBeHl9ZlCc5F0Zat2RyVS3b6L7N8lm2pwMkiOZtgF82/HrqxAMHCj2esUBrVfyma2rcUkzruVpCHO55yICzchcLftUMCplDfOLXyXoCyC6623tOj3brDnonWZ38oK8yCpNTeAECgYEA/AYZbpG9DHiXxWcckGP4khRkVNXugbPHVKmeZgNRRUEuhCkteRwUlzuc0TEYLzEYVtk9vB4bMuZUZZkud7LJ+jopMruU+2zBbR2Ryrnj4KhGCD2Fa7fx09RhqO4T/X0SFX0RthtIXeY+ZElUEY7+vZ3ogHOxhVG00gheyPPm0gECgYEAvCZubIiJW/zlNwFSYIB9ratSo/hge+/FfBZQqATWgp5DkyAjzi7QdG3S+v8MorpYq20X8WlqEuuEWDlCzP89Ac4aCLji9pfAteJZIPRXjkUl7NQvIJffo4he/trqFhj0MOO01wa59N2Gc8Ry9FYqjNcrzhs8K6BeV7jC1tCBjScCgYEAunE8sTqps6cnCEdurzb19gOV8djN+C/qf6x33RoJLoW5BtZ7qIPoi7TzHyMFtCvKyL0v2ZIgJviUaRD4el7O4wnR3pE89v+O3M0qROJePZ3fKUtx/611/nK4yMNQEJBNJ4594s2uLKEfeQtyYQb7V4WRMi3kny9B6Lt+e4VMNgECgYAclSoJSuSzGMQpiwXcqyhsja5MQptFuLMjmrA+Fh4QUcKqQyOtWudICLcYck6VGgIGaNhOUFlQ7n54eJpxUgUOlSTXVl8EXyFjgDNSEkyKzG5qgAF4zhmnWjw0M0WEfd3631zFnbv6Ov0F8T4VaFGme7mn4fNOHVq3sNXIlHxZ2QKBgQDGvXynD3XUymYiC5Ss7/Pt89C0FrSkGNbzCVtOWyMg7CfezKxtjW0YTucJBUBjjujpkxxu0rg71/8pYgqF/6WG0kwXcvRivwI6uRZ2x7goxmM19dfpi8jHEZ7UI4ROHrTxUehxPOAP/9YCtdqALdd9bS/iVJqo9Hold8Ba0afp+A==";
            */

            String strKeyAlgorithm = "DSA"; // Supported DSA and RSA
            String strHashAlgorithm = "SHA256withDSA"; //e.g SHA256withDSA, SHA256withRSA
            DigitalSignature theDigitalSignature = new DigitalSignature(strKeyAlgorithm, strHashAlgorithm);

            String strData = "{\"network_interfaces\":[{\"interface_name\":\"vmnet8\",\"interface_addresses\":[{\"host_address\":\"fe80:0:0:0:250:56ff:fec0:8%vmnet8\",\"canonical_hostname\":\"fe80:0:0:0:250:56ff:fec0:8%vmnet8\",\"host_name\":\"fe80:0:0:0:250:56ff:fec0:8%vmnet8\",\"broadcast\":\"null\",\"network_prefix_length\":\"64\"},{\"host_address\":\"192.168.153.1\",\"canonical_hostname\":\"192.168.153.1\",\"host_name\":\"192.168.153.1\",\"broadcast\":\"/192.168.153.255\",\"network_prefix_length\":\"24\"}],\"hardware_address\":\"00-50-56-C0-00-08\",\"virtual\":\"NO\",\"sub_interfaces\":{}},{\"interface_name\":\"vmnet1\",\"interface_addresses\":[{\"host_address\":\"fe80:0:0:0:250:56ff:fec0:1%vmnet1\",\"canonical_hostname\":\"fe80:0:0:0:250:56ff:fec0:1%vmnet1\",\"host_name\":\"fe80:0:0:0:250:56ff:fec0:1%vmnet1\",\"broadcast\":\"null\",\"network_prefix_length\":\"64\"},{\"host_address\":\"192.168.221.1\",\"canonical_hostname\":\"192.168.221.1\",\"host_name\":\"192.168.221.1\",\"broadcast\":\"/192.168.221.255\",\"network_prefix_length\":\"24\"}],\"hardware_address\":\"00-50-56-C0-00-01\",\"virtual\":\"NO\",\"sub_interfaces\":{}},{\"interface_name\":\"wlp4s0\",\"interface_addresses\":[{\"host_address\":\"fe80:0:0:0:1698:2d7c:87e:c405%wlp4s0\",\"canonical_hostname\":\"fe80:0:0:0:1698:2d7c:87e:c405%wlp4s0\",\"host_name\":\"fe80:0:0:0:1698:2d7c:87e:c405%wlp4s0\",\"broadcast\":\"null\",\"network_prefix_length\":\"64\"},{\"host_address\":\"10.3.1.51\",\"canonical_hostname\":\"10.3.1.51\",\"host_name\":\"10.3.1.51\",\"broadcast\":\"/10.3.1.255\",\"network_prefix_length\":\"24\"}],\"hardware_address\":\"44-85-00-C5-43-C4\",\"virtual\":\"NO\",\"sub_interfaces\":{}},{\"interface_name\":\"lo\",\"interface_addresses\":[{\"host_address\":\"0:0:0:0:0:0:0:1%lo\",\"canonical_hostname\":\"ip6-localhost\",\"host_name\":\"ip6-localhost\",\"broadcast\":\"null\",\"network_prefix_length\":\"128\"},{\"host_address\":\"127.0.0.1\",\"canonical_hostname\":\"localhost\",\"host_name\":\"localhost\",\"broadcast\":\"null\",\"network_prefix_length\":\"8\"}],\"hardware_address\":\"44-85-00-C5-43-C4\",\"virtual\":\"NO\",\"sub_interfaces\":{}}]}\n";
            //SIGNATURES
            String strSignature = theDigitalSignature.createSignature(privateKey, strData);
            //String strSignature = "MD0CHQCisu/0JW4nqP+NAUo4q7viSRBs+/uzCZzL71kiAhxFHfuWDMdqbDGnuYjhJiDOhVdIxqYH6OtTZ6lc";
            System.out.println("Signature = " + strSignature);
            strData = "{\"network_interfaces\":[{\"interface_name\":\"vmnet8\",\"interface_addresses\":[{\"host_address\":\"fe80:0:0:0:250:56ff:fec0:8%vmnet8\",\"canonical_hostname\":\"fe80:0:0:0:250:56ff:fec0:8%vmnet8\",\"host_name\":\"fe80:0:0:0:250:56ff:fec0:8%vmnet8\",\"broadcast\":\"null\",\"network_prefix_length\":\"64\"},{\"host_address\":\"192.168.153.1\",\"canonical_hostname\":\"192.168.153.1\",\"host_name\":\"192.168.153.1\",\"broadcast\":\"/192.168.153.255\",\"network_prefix_length\":\"24\"}],\"hardware_address\":\"00-50-56-C0-00-08\",\"virtual\":\"NO\",\"sub_interfaces\":{}},{\"interface_name\":\"vmnet1\",\"interface_addresses\":[{\"host_address\":\"fe80:0:0:0:250:56ff:fec0:1%vmnet1\",\"canonical_hostname\":\"fe80:0:0:0:250:56ff:fec0:1%vmnet1\",\"host_name\":\"fe80:0:0:0:250:56ff:fec0:1%vmnet1\",\"broadcast\":\"null\",\"network_prefix_length\":\"64\"},{\"host_address\":\"192.168.221.1\",\"canonical_hostname\":\"192.168.221.1\",\"host_name\":\"192.168.221.1\",\"broadcast\":\"/192.168.221.255\",\"network_prefix_length\":\"24\"}],\"hardware_address\":\"00-50-56-C0-00-01\",\"virtual\":\"NO\",\"sub_interfaces\":{}},{\"interface_name\":\"wlp4s0\",\"interface_addresses\":[{\"host_address\":\"fe80:0:0:0:1698:2d7c:87e:c405%wlp4s0\",\"canonical_hostname\":\"fe80:0:0:0:1698:2d7c:87e:c405%wlp4s0\",\"host_name\":\"fe80:0:0:0:1698:2d7c:87e:c405%wlp4s0\",\"broadcast\":\"null\",\"network_prefix_length\":\"64\"},{\"host_address\":\"10.3.1.51\",\"canonical_hostname\":\"10.3.1.51\",\"host_name\":\"10.3.1.51\",\"broadcast\":\"/10.3.1.255\",\"network_prefix_length\":\"24\"}],\"hardware_address\":\"44-85-00-C5-43-C4\",\"virtual\":\"NO\",\"sub_interfaces\":{}},{\"interface_name\":\"lo\",\"interface_addresses\":[{\"host_address\":\"0:0:0:0:0:0:0:1%lo\",\"canonical_hostname\":\"ip6-localhost\",\"host_name\":\"ip6-localhost\",\"broadcast\":\"null\",\"network_prefix_length\":\"128\"},{\"host_address\":\"127.0.0.1\",\"canonical_hostname\":\"localhost\",\"host_name\":\"localhost\",\"broadcast\":\"null\",\"network_prefix_length\":\"8\"}],\"hardware_address\":\"44-85-00-C5-43-C4\",\"virtual\":\"NO\",\"sub_interfaces\":{}}]}\n";
            boolean boolVerifySignature = theDigitalSignature.verifySignature(publicKey, strSignature, strData);

            if(boolVerifySignature) {
                System.out.println("Signature verified");
            } else {
                System.out.println("Signature FAILED");
            }





        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }
}