package ke.skyworld.mbanking;

import ke.skyworld.security.encryption.cryptography.asymmetric.DigitalKeyPairGenerator;

public class Main {
    public static void main(String[] args) {
        DigitalKeyPairGenerator digitalKeyPairGenerator = new DigitalKeyPairGenerator();

        System.out.println("Private key string: " + digitalKeyPairGenerator.getPrivateKeyString());
        System.out.println("Public key string: " + digitalKeyPairGenerator.getPublicKeyString());
    }
}
