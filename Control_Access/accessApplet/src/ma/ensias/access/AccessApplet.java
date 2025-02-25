package ma.ensias.access;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
import ma.ensias.metier.security.AccessMetier;

public class AccessApplet extends Applet {
    private static final byte INS_VERIFY_PIN = (byte) 0x20;
    private static final byte INS_UPDATE_PIN  = (byte) 0x21;
    private static final byte INS_VALIDATE_ACCESS  = (byte) 0x22;
    
    private static final short SESSION_TIMEOUT = 300; //  5 minutes in seconds
    
    // security objects
    private OwnerPIN pin;
    private AESKey privateKey;
    private boolean isAuthenticated;
    private short sessionTimer;

    private AccessApplet() {
        // Initialization of the PIN
        pin = new OwnerPIN((byte) 3, (byte) 4); // 3 max trials, 4 digit PIN
        AccessMetier.setupPin(pin);

        // Initialization of the AES key
        privateKey = (AESKey) KeyBuilder.buildKey(
            KeyBuilder.TYPE_AES,
            KeyBuilder.LENGTH_AES_128,
            false
        );
        AccessMetier.setupKey(privateKey);
        
        isAuthenticated = false;
        sessionTimer = 0;
        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AccessApplet();
    }

    private void resetSession() {
        isAuthenticated = false;
        sessionTimer = 0;
    }

    public void process(APDU apdu) {
    	
        byte[] buffer = apdu.getBuffer();

        if (selectingApplet()) {
            resetSession();
            return;
        }

        // Check the class of instruction
        if (buffer[ISO7816.OFFSET_CLA] != 0x00) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        // Management of instructions
        switch (buffer[ISO7816.OFFSET_INS]) {
            case INS_VERIFY_PIN:
                isAuthenticated = AccessMetier.validatePin(apdu, pin);
                if (isAuthenticated) {
                    sessionTimer = SESSION_TIMEOUT;
                }
                break;

            case INS_UPDATE_PIN:
                if (!isAuthenticated) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                AccessMetier.updatePin(apdu, pin);
                resetSession();
                break;

            case INS_VALIDATE_ACCESS:
                AccessMetier.authorizeAccess(apdu, privateKey, isAuthenticated);
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }

        // Decresing session timer if authenticated
        if (isAuthenticated && sessionTimer > 0) {
            sessionTimer--;
            if (sessionTimer == 0) {
                resetSession();
            }
        }
    }
}
