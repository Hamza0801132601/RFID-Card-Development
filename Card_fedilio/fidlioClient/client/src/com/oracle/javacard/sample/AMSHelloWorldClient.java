/*
 * Copyright (c) 1998, 2024, Oracle and/or its affiliates. All rights reserved.
 */

package com.oracle.javacard.sample;

import java.io.FileInputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.IntStream;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import com.oracle.javacard.ams.AMService;
import com.oracle.javacard.ams.AMServiceFactory;
import com.oracle.javacard.ams.AMSession;
import com.oracle.javacard.ams.config.AID;
import com.oracle.javacard.ams.config.CAPFile;
import com.oracle.javacard.ams.script.APDUScript;
import com.oracle.javacard.ams.script.ScriptFailedException;
import com.oracle.javacard.ams.script.Scriptable;

public class AMSHelloWorldClient {

	static final String mainAID = "aid:A000000151000000";
    static final String appAID = "aid:202020202020"; // AID spécifique de votre applet
	static final String classAID = "aid:202020202000";
	static final String instanceAID = "aid:202020202000";
	static final String libraryAID = "aid:212121212121";
    static final CommandAPDU selectApplication = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID.from(instanceAID).toBytes(), 256);
    static final CommandAPDU checkBalanceAPDU = new CommandAPDU(0x00, 0x0A, 0x00, 0x00, (short)0); 
    static final CommandAPDU addCreditAPDU = new CommandAPDU((byte)0x00, (byte)0x0B,(byte) 0x00,(byte) 0x00, new byte[] {});
    static final CommandAPDU subtractCreditAPDU = new CommandAPDU((byte)0x00, (byte)0x0C, (byte)0x00, (byte)0x00, new byte[] {}); 
    static final CommandAPDU initPointsAPDU = new CommandAPDU(0x00, 0x0D, 0x00, 0x00, (short)0); 

	/**
	 * Launch the sample
	 *
	 * @param args command arguments. Use {@code -cap=<capfile> -props=<propsfile>}
	 */
	public static void main(String[] args) {

		int resultCode = 0;

		try {
			// Chargement du fichier CAP et des propriétés
            CAPFile businessLibFile = CAPFile.from(getArgument(args, "caplib"));
            CAPFile appFile = CAPFile.from(getArgument(args, "capfidelio"));
            Properties props = new Properties();
            props.load(new FileInputStream(getArgument(args, "props")));

            // Création et configuration du service de gestion des applications
            AMService ams = AMServiceFactory.getInstance("GP2.2");
            ams.setProperties(props);
            for (String key : ams.getPropertiesKeys()) {
                System.out.println(key + " = " + ams.getProperty(key));
            }

         // Déploiement de l'application
            AMSession deploy = ams.openSession(mainAID)   // sélection du SD & ouverture du canal sécurisé
            		.load(libraryAID, businessLibFile.getBytes())
                    .load(appAID, appFile.getBytes())  // chargement du fichier d'application
                    .install(appAID,                   // installation de l'application
                    		classAID, instanceAID)
                    .close();
         // Suppression de l'application
            AMSession undeploy = ams.openSession(mainAID) // sélection du SD & ouverture du canal sécurisé
                    .uninstall(instanceAID)      // désinstallation de l'application
                    .unload(appAID)                    // déchargement du code de l'application
                    .close();

            // Script pour déployer une applet, l'utiliser et la désinstaller
            TestScript testScript = new TestScript()
                    .append(deploy)
                    .append(selectApplication)
                    .append(new CommandAPDU(0x00, 0x0A, 0x00, 0x00, new byte[] { }));
                    //.append(undeploy);

         // Terminal vers le simulateur
            CardTerminal t = getTerminal("socket", "127.0.0.1", "9025"); // Ou utilisez "pcsc" pour d'autres types
            
         // Attendre que la carte soit présente
            if (t.waitForCardPresent(1000)) {
                System.out.println("Connected to terminal: " + t.getName());
              if (t.isCardPresent()) {
                    Card c = t.connect("T=1");
                    System.out.println("Connection successful!");
                
                System.out.println(getFormattedATR(c.getATR().getBytes()));
                
                

                // Menu interactif pour l'utilisateur
                
             // Sélection de l'applet Fidelio
                ResponseAPDU selectResponse = c.getBasicChannel().transmit(selectApplication);
                System.out.println("Application selected: SW = " + Integer.toHexString(selectResponse.getSW()));

                if (selectResponse.getSW() == 0x9000) {
                    // Interaction avec l'applet Fidelio
                    interactWithFidelio(c.getBasicChannel());
                } else {
                    System.out.println("Error selecting application.");
                }
                

             // Exécution du script de tests
                List<ResponseAPDU> responses = testScript.run(c.getBasicChannel());
                
                System.out.println("Script executed. Responses:" + responses.size());

                c.disconnect(true);
                
                
              }
              else {
                  System.out.println("No card present in terminal.");
              }
                
            }
            else {
                System.out.println("Failed to connect to terminal.");
                resultCode = -1;
            }

        } catch (NoSuchAlgorithmException | NoSuchProviderException | CardException | ScriptFailedException | IOException e) {
            e.printStackTrace();
            resultCode = -1;
        }
        System.exit(resultCode);
	}
	private static void interactWithFidelio(CardChannel channel) throws CardException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
        	//System.out.println("\nENSIAS FIDELIO APPLICATION /> :");
            System.out.println("\nChoose an option: :");
            System.out.println("1. Check balance");
            System.out.println("2. Add credit");
            System.out.println("3. Subtract credit");
            System.out.println("4. Initialize points");
            System.out.println("5. Exit");

            int choix = scanner.nextInt();
            CommandAPDU command;
            ResponseAPDU response;

            switch (choix) {
                case 1: // Vérifier le solde
                    command = checkBalanceAPDU;
                    break;
                case 2: // Créditer un montant
                    System.out.print("Enter credit amount: ");
                    int creditAmount = scanner.nextInt();
                    command = new CommandAPDU((byte)0x00, (byte)0x0B, (byte)0x00, (byte)0x00, new byte[] {(byte) (creditAmount >> 8), (byte) creditAmount});
                    break;
                case 3: // Débiter un montant
                    System.out.print("Enter debit amount: ");
                    int debitAmount = scanner.nextInt();
                    command = new CommandAPDU((byte)0x00, (byte)0x0C, (byte)0x00, (byte)0x00, new byte[] {(byte) (debitAmount >> 8), (byte) debitAmount});
                    break;
                case 4: // Vérifier le solde
                    command = initPointsAPDU;
                    break;
                case 5: // Quitter
                    System.out.println("Exiting interaction.");
                    return;
                default:
                    System.out.println("Invalid option!");
                    continue;
            }

            // Envoyer la commande APDU et afficher la réponse
            response = channel.transmit(command);
            System.out.println("Réponse : " + bytesToHex(response.getData()) + " SW: " + Integer.toHexString(response.getSW()));
        }
    }
	// Utilitaire pour convertir des octets en chaîne hexadécimale
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
 // Récupérer une valeur pour un argument depuis la ligne de commande
    private static String getArgument(String[] args, String argName) throws IllegalArgumentException {
        String value = null;

        for (String param : args) {
            if (param.startsWith("-" + argName + "=")) {
                value = param.substring(param.indexOf('=') + 1);
            }
        }

        if(value == null || value.length() == 0) {
            throw new IllegalArgumentException("Argument " + argName + " is missing");
        }

        return value;
    }
 // Formater les octets de l'ATR en une chaîne hexadécimale
    private static String getFormattedATR(byte[] ATR) {
        StringBuilder sb = new StringBuilder();
        for (byte b : ATR) {
            sb.append(String.format("%02X ", b));
        }
        return String.format("ATR: [%s]", sb.toString().trim());
    }
 // Obtenir un terminal connecté au simulateur ou un autre type de connexion
    private static CardTerminal getTerminal(String... connectionParams) throws NoSuchAlgorithmException, NoSuchProviderException, CardException {
        TerminalFactory tf;
        String connectivityType = connectionParams[0];
        if (connectivityType.equals("socket")) {
            String ipaddr = connectionParams[1];
            String port = connectionParams[2];
            tf = TerminalFactory.getInstance("SocketCardTerminalFactoryType",
                    List.of(new InetSocketAddress(ipaddr, Integer.parseInt(port))),
                    "SocketCardTerminalProvider");
        } else {
            tf = TerminalFactory.getDefault();
        }
        return tf.terminals().list().get(0);
    }
 // Classe pour gérer les scripts de commandes APDU
    private static class TestScript extends APDUScript{
    	private List<CommandAPDU> commands = new LinkedList<>();
    	private List<ResponseAPDU> responses = new LinkedList<>();
    	private int index = 0;
    	
    	public List<ResponseAPDU> run(CardChannel channel) throws ScriptFailedException {
            return super.run(channel, c -> lookupIndex(c), r -> !isExpected(r));
        }
    	
    	@Override
        public TestScript append(Scriptable<CardChannel, CommandAPDU, ResponseAPDU> other) {
            super.append(other);
            return this;
        }
    	
    	public TestScript append(CommandAPDU apdu, ResponseAPDU expected) {
            super.append(apdu);
            this.commands.add(apdu);
            this.responses.add(expected);
            return this;
        }
    	
    	public TestScript append(CommandAPDU apdu) {
            super.append(apdu);
            return this;
        }

    	private CommandAPDU lookupIndex(CommandAPDU apdu) {
            print(apdu);
            this.index = IntStream.range(0, this.commands.size())
                    .filter(i -> apdu == this.commands.get(i))
                    .findFirst()
                    .orElse(-1);
            return apdu;
        }
    	
    	private boolean isExpected(ResponseAPDU response) {

            ResponseAPDU expected = (index < 0)? response : this.responses.get(index);
            if (!response.equals(expected)) {
                System.out.println("Received: ");
                print(response);
                System.out.println("Expected: ");
                print(expected);
                return false;
            }
            print(response);
            return true;
        }
    	private static void print(CommandAPDU apdu) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%02X%02X%02X%02X %02X[", apdu.getCLA(), apdu.getINS(), apdu.getP1(), apdu.getP2(), apdu.getNc()));
            for (byte b : apdu.getData()) {
                sb.append(String.format("%02X", b));
            }
            sb.append("]");
            System.out.format("[%1$tF %1$tT %1$tL %1$tZ] [APDU-C] %2$s %n", System.currentTimeMillis(), sb.toString());
        }
    	private static void print(ResponseAPDU apdu) {
            byte[] bytes = apdu.getData();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            System.out.format("[%1$tF %1$tT %1$tL %1$tZ] [APDU-R] [%2$s] SW:%3$04X %n", System.currentTimeMillis(), sb.toString(), apdu.getSW());
        }
    }
}
