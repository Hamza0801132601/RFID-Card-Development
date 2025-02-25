package ma.ensias.fidelio;

import javacard.framework.*;
import ma.ensias.metier.lib.MetierLib;

public class FidelioApplet extends Applet {
    // Codes des instructions
    private static final byte INS_GET = (byte) 0x0A;
    private static final byte INS_ADD = (byte) 0x0B;
    private static final byte INS_SUB = (byte) 0x0C;
    private static final byte INS_INIT = (byte) 0x0D;

    // Solde initialisé à 0
    private short solde = 0;

    // Méthode d'installation de l'applet
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new FidelioApplet(bArray, bOffset, bLength);
    }

    // Constructeur de l'applet
    protected FidelioApplet(byte[] bArray, short bOffset, byte bLength) {
        // Enregistrement de l'applet dans la plate-forme
        register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    // Méthode principale pour traiter les commandes APDU
    @Override
    public void process(APDU apdu) {
        // Vérifie si l'applet est en cours de sélection
        if (selectingApplet()) {
            return;
        }

        // Récupération du buffer APDU
        byte[] buffer = apdu.getBuffer();

        // Vérification de la classe CLA
        if (buffer[ISO7816.OFFSET_CLA] == 0x00) {
            // Gestion des instructions INS
            switch (buffer[ISO7816.OFFSET_INS] & 0xFF) {
                case INS_GET:
                    MetierLib.getSolde(apdu, solde);
                    break;

                case INS_ADD:
                    solde = MetierLib.addPoints(apdu, solde);
                    break;

                case INS_SUB:
                    solde = MetierLib.deductPoints(apdu, solde);
                    break;

                case INS_INIT:
                    solde = MetierLib.init(apdu);
                    break;

                default:
                    // Instruction non reconnue
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                    break;
            }
        } else {
            // Classe CLA non supportée
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
    }
}
