/**
 * Copyright (c) 1998, 2024, Oracle and/or its affiliates. All rights reserved.
 * 
 */

package com.oracle.jcclassic.samples.helloworld;

import javacard.framework.APDU;

import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class HelloWorld extends Applet {
    private byte[] echoBytes;
    private static final short LENGTH_ECHO_BYTES = 256;

    /**
     * Only this class's install method should create the applet object.
     */
    protected HelloWorld() {
        echoBytes = new byte[LENGTH_ECHO_BYTES];
        register();
    }

    /**
     * Installs this applet.
     *
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HelloWorld();
    }

    /**
     * Processes an incoming APDU.
     *
     * @see APDU
     * @param apdu
     *            the incoming APDU
     * @exception ISOException
     *                with the response bytes per ISO 7816-4
     */
   @Override 
   public void process(APDU apdu) {
        byte buffer[] = apdu.getBuffer();

        // check SELECT APDU command
        if (selectingApplet()) {
            return;
        }

        short bytesRead = apdu.setIncomingAndReceive();
        short echoOffset = (short) 0;

        while (bytesRead > 0) {
            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, echoBytes, echoOffset, bytesRead);
            echoOffset += bytesRead;
            bytesRead = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }

        apdu.setOutgoing();
        apdu.setOutgoingLength((short) (echoOffset + ISO7816.OFFSET_CDATA));

        // echo header
        apdu.sendBytes((short) 0, (short) ISO7816.OFFSET_CDATA);
        // echo data
        apdu.sendBytesLong(echoBytes, (short) 0, echoOffset);
    }

}
