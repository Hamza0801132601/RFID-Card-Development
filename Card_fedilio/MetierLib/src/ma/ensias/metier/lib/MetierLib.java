package ma.ensias.metier.lib;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class MetierLib {
	public static void getSolde(APDU apdu, short solde) {
		byte[] buffer = apdu.getBuffer();
		Util.setShort(buffer, (short) 0, solde);
		apdu.setOutgoingAndSend((short) 0, (short) 2);
	}
	public static short addPoints(APDU apdu, short solde) {
		byte[] buffer = apdu.getBuffer();
		return (short) (solde + Util.getShort(buffer, ISO7816.OFFSET_CDATA));
	}
	public static short deductPoints(APDU apdu, short solde) {
		byte[] buffer = apdu.getBuffer();
		short deductPoints= Util.getShort(buffer, ISO7816.OFFSET_CDATA);
		if (solde >= deductPoints) {
			solde -= deductPoints;
		}
		else {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		return solde;
	}
	public static short init(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		return (Util.getShort(buffer, ISO7816.OFFSET_CDATA));
	}
}