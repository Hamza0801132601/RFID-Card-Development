package ma.ensias.metier.security;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class AccessMetier {
	private static final byte PIN_LENGTH = 4;
    private static final byte MAX_ATTEMPTS = 3;
    private static final short IV_LENGTH = 16;
    private static final short BLOCK_SIZE_AES = 16;
    
    public static boolean validatePin(APDU apdu, OwnerPIN pin) {
        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte) apdu.setIncomingAndReceive();

        if (byteRead != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Ajouter un délai après des tentatives échouées
        if (pin.getTriesRemaining() < MAX_ATTEMPTS) {
            try {
                for(short i = 0; i < 100; i++) {
                    // Simple délai
                }
            } catch (Exception e) {
                // Ignorer les exceptions de délai
            }
        }

        return pin.check(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH);
    }

    public static void updatePin(APDU apdu, OwnerPIN pin) {
        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte) apdu.setIncomingAndReceive();

        if (byteRead != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        pin.update(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH);
    }

    public static void authorizeAccess(APDU apdu, AESKey privateKey, boolean isAuthenticated) {
        if (!isAuthenticated) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte) apdu.setIncomingAndReceive();

        if (byteRead % BLOCK_SIZE_AES != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Générer un IV aléatoire
        byte[] iv = new byte[IV_LENGTH];
        RandomData random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        random.generateData(iv, (short)0, IV_LENGTH);

        // Initialiser le chiffrement
        Cipher cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        cipher.init(privateKey, Cipher.MODE_ENCRYPT, iv, (short)0, (short)iv.length);

        // Chiffrer les données
        byte[] response = new byte[byteRead];
        cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, byteRead, response, (short)0);

        // Copier l'IV suivi des données chiffrées dans la réponse
        Util.arrayCopyNonAtomic(iv, (short)0, buffer, (short)0, IV_LENGTH);
        Util.arrayCopyNonAtomic(response, (short)0, buffer, IV_LENGTH, byteRead);
        
        apdu.setOutgoingAndSend((short)0, (short)(IV_LENGTH + byteRead));
    }

    public static void setupPin(OwnerPIN pin) {
        byte[] defaultPin = {(byte)1, (byte)2, (byte)3, (byte)4};
        pin.update(defaultPin, (short)0, PIN_LENGTH);
    }

    public static void setupKey(AESKey privateKey) {
        byte[] defaultKeyValue = {
            (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, 
            (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07,
            (byte)0x08, (byte)0x09, (byte)0x0A, (byte)0x0B, 
            (byte)0x0C, (byte)0x0D, (byte)0x0E, (byte)0x0F
        };
        privateKey.setKey(defaultKeyValue, (short)0);
    }
}
