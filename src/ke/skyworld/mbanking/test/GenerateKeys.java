package ke.skyworld.mbanking.test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class GenerateKeys {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        // To get the keys in base64 similar to C#
        String publicKeyBase64 = "BgIAAACkAABSU0ExAAgAAAEAAQAZGUKaKbn3mKiGAoGH9p6g8tqlyfKicqspijZan1vNOi0sCNfPxU7hawbOI/7LGqC/YDnNlumKpUtKYwuT4DX0UineMjg8b9meNhTSUYyYNd/UXIa8nzsLLRQGIqxpl1pyaVS/vanKkeWlAcV2X9oPQIhcEiOkCJ/axPRQRVqWJNg7AFA0uVdOLXjQLbFSu3hx7vDq1abcAewBphUoyoGwP8m4RCFbisCFmrm5U007qgr70DZKPvjrK733xYoqObQ893vr6pMyNzDkI5Pl3XHGu4XdzprmdQFUpAzLsrvwHlmkZcwwisfdQ1I3pFi4IsQBYE5rgL1qdbGTh2SXRY/M";
        String privateKeyBase64 = "BwIAAACkAABSU0EyAAgAAAEAAQAZGUKaKbn3mKiGAoGH9p6g8tqlyfKicqspijZan1vNOi0sCNfPxU7hawbOI/7LGqC/YDnNlumKpUtKYwuT4DX0UineMjg8b9meNhTSUYyYNd/UXIa8nzsLLRQGIqxpl1pyaVS/vanKkeWlAcV2X9oPQIhcEiOkCJ/axPRQRVqWJNg7AFA0uVdOLXjQLbFSu3hx7vDq1abcAewBphUoyoGwP8m4RCFbisCFmrm5U007qgr70DZKPvjrK733xYoqObQ893vr6pMyNzDkI5Pl3XHGu4XdzprmdQFUpAzLsrvwHlmkZcwwisfdQ1I3pFi4IsQBYE5rgL1qdbGTh2SXRY/M97MIW1kbYNrL0oU3nSDyGuFd+E2XVBAfKE0h5JEtDHKzVZs4ZIXDI6olaEkbO5LS6f7JtqSAFxlXVaRkvMRdJ/WRP9d62OBRnsRPeatyoyfloie5+yWPkjsxiNe7BVXnPzUutRqFnPDtI9RfTsgTzasa/Oj2pJftSWRc2D9BM95vNz57v210f79WLNpiFseQItyWGChDo8Xm5QLjYjEj5oF3QNVVeM0dYbJ7uXGHVgSQnGln8vOONryCRfDuMWDLHdACgCLapvC7h2jqNPOvJMsFTW0iKJgf1UcbHeNtG0QlDE+18lCoCkIFKk2pZ7TD8eolHG0/k0T1Vgj8fxKt66d65ZHFRSFij3bAeqMobPNbdn5XU9nLBpGfyjWdBlmrV+OX3rXh/BrBLmb4TRuUjylehceZHjlO/rSeD7pXxJ5/VmUwvq90NJ2fV9XcLIGbTUpE+FK40oIppJHWdiM7NYbAluUPj6cfyjB+TSLlIW1Qnc+TgznK+bn+GyniMoZVC3SkZNxRypgV+8XJduBY59CiopWtJmu0TYlpIc0+ygNsZJ0GE7W08VUS6t33ngylFMVsxuLt4PCKzX8lUhXr1I1h5bPThq86YAtEzdI2CcZfSWr7xd6aCU6q1KiXHfifb/8zQfdVlMxxEUIeS0mu8nQq4gLeCPEqKq/jdmAOsYSz1pwMo7jr3pz0pVzEVRyetNs6d339YcVObaK6qydAFffZKx1VWZGgK35wJJcWOO9RjdcCjEe3OLltI7JVijbEzJu9aXFFQ4FxHGIlvwIfkN5k2++n58xLLwNgBaE6wDVwZgRE4RDIUwCt8VTgoS1b07nfj1iMmLRn/M1ZJSROkwGfcRZWW7npH81vTsxksW/6rWAEg09imcjtLNe3mIG/b/ZjE6/LpQBldMNmdJJ9VZkN0CLpZfRMracKv8QsJm7agmgTAGIuK9QmVhe/1j4dfeir00sIa5ZjA/4LvXK4ZTeaX0SKWbaAyXOX7QwU35Ve1k3a+c+pjuZJqAgfltQql2dBwVq0Ua3oPyDO5aPMOl26Zjy5SSkVHMWzTBOQqwgRMU/yWQNbbtNeZxOg02gnS2ASx9hhrFKnaLpz6MbhPiRx4G1eLhqYk1FwbsAHeDI+u8v76TBACr9NbZOtuHIbbLqDpwIoks0RBla9wsST7g5G+nlF75WtULXA1h5Boos=";

        /*String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());*/

        // Encryption
        String message = "Hello, world!";
        String encryptedMessage = encrypt(message, publicKeyBase64);

        // Decryption
        String decryptedMessage = decrypt(encryptedMessage, privateKeyBase64);

        System.out.println("Original Message: " + message);
        System.out.println("Encrypted Message: " + encryptedMessage);
        System.out.println("Decrypted Message: " + decryptedMessage);
    }

    public static PublicKey getPublicKey(String base64PublicKey){
        PublicKey publicKey = null;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encrypt(String message, String publicKeyBase64) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyBase64);

        Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encryptedMessageBytes = encryptCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedMessageBytes);
    }

    public static String decrypt(String encryptedMessageBase64, String privateKeyBase64) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(spec);

        Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedMessageBytes = Base64.getDecoder().decode(encryptedMessageBase64);
        byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);
        return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
    }
}
