package planka.graph.driver.utils;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 2022/6/18 4:57 PM
 *
 * @author penghs
 **/
public class NanoID {
    private static final SecureRandom DEFAULT_NUMBER_GENERATOR = new SecureRandom();
    private static final char[] DEFAULT_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int DEFAULT_SIZE = 21;

    private NanoID() {
    }

    public static String random() {
        return randomNanoId(ThreadLocalRandom.current(), DEFAULT_ALPHABET, DEFAULT_SIZE);
        //return randomNanoId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, DEFAULT_SIZE);
    }

    public static String random(int size) {
        return randomNanoId(ThreadLocalRandom.current(), DEFAULT_ALPHABET, size);
    }

    public static String randomNanoId(Random random, char[] alphabet, int size) {
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null.");
        } else if (alphabet == null) {
            throw new IllegalArgumentException("alphabet cannot be null.");
        } else if (alphabet.length != 0 && alphabet.length < 256) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be greater than zero.");
            } else {
                int mask = (2 << (int) Math.floor(Math.log(alphabet.length - 1.0) / Math.log(2.0))) - 1;
                int step = (int) Math.ceil(1.6 * mask * size / alphabet.length);
                StringBuilder idBuilder = new StringBuilder();

                while (true) {
                    byte[] bytes = new byte[step];
                    random.nextBytes(bytes);
                    for (int i = 0; i < step; ++i) {
                        int alphabetIndex = bytes[i] & mask;
                        if (alphabetIndex < alphabet.length) {
                            idBuilder.append(alphabet[alphabetIndex]);
                            if (idBuilder.length() == size) {
                                return idBuilder.toString();
                            }
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("alphabet must contain between 1 and 255 symbols.");
        }
    }

}
