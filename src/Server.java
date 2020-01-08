import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private int port;
    private int listeningInterval;
    private volatile boolean stop;
    private int maxClient =20;
    private String team ="YBETC-Team                      ";
    private AtomicInteger numOfClient = new AtomicInteger(0);

    public Server(int port, int listeningInterval) {
        this.port = port;
        this.listeningInterval = listeningInterval;
    }

    public void start() {
        new Thread(() -> {
            runServer();
        }).start();
    }

    private void runServer() {
        try {
            // datagram socket
            DatagramSocket serverSocket = new DatagramSocket(port);
         //   serverSocket.setSoTimeout(listeningInterval);
            while (!stop) {
                try {
                    DatagramPacket clientPacket = new DatagramPacket(new byte[1024],1024);
                    System.out.println("Waiting for packets to be received");
                    serverSocket.receive(clientPacket);
                    System.out.println("sercer");
                    new Thread(() -> {
                        handleClient(serverSocket,clientPacket);
                    }).start();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                }
            }
            serverSocket.close();
        } catch (IOException e) {
        }
    }

    private void handleClient(DatagramSocket clientSocket, DatagramPacket clientPacket) {
        try {
            System.out.println("at handle client function");
            if(numOfClient.addAndGet(1) < 20) {
                byte[] fromClient = splitBytesArrayToStringArray(clientPacket.getData());
                if(fromClient == null){
                    System.out.println("null");
                    return;
                }
                else {
                    System.out.println("not null");
                    System.out.println(new String(fromClient));
                    clientSocket.send(new DatagramPacket(fromClient,fromClient.length,clientPacket.getAddress(),clientPacket.getPort()));
                    System.out.println("Packet Sent");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stop = true;
    }


    private byte[] splitBytesArrayToStringArray(byte[] bytes) {
        String[] afterParse = new String[6];
        String harta = new String(bytes);
        harta = harta.trim();
        byte[] nameOfGroup = new byte[32];
        for (int i = 0; i < 32; i++) {
            nameOfGroup[i] = bytes[i];
        }
        afterParse[0] = new String(nameOfGroup);

        byte[] type = new byte[1];
        type[0] = bytes[32];
        afterParse[1] = new String(type);
        if (afterParse[1].equals("\1")) { // Discover
            //TODO: maybe change to my group name
            String toSendOffer = afterParse[0] + "\2";
            return toSendOffer.getBytes(StandardCharsets.UTF_8);
        } else if (afterParse[1].equals("\3")) { //Request
            //Get Hash
            byte[] array2 = new byte[40];
            for (int i = 0; i < 40; i++) {
                array2[i] = bytes[i + 33];
            }
            afterParse[2] = new String(array2);

            byte[] originalLength = new byte[1];
            originalLength[0] = bytes[73];
            afterParse[3] = new String(originalLength); //Original Length


            int numOfOriginalString = (harta.length() - 74) / 2;

            byte[] OriginalStringStart = new byte[numOfOriginalString];
            for (int i = 0; i < numOfOriginalString; i++) {
                OriginalStringStart[i] = bytes[i + 74];
            }

            byte[] OriginalStringEnd = new byte[numOfOriginalString];
            for (int i = 0; i < numOfOriginalString; i++) {
                OriginalStringEnd[i] = bytes[i + 74 + numOfOriginalString];
            }
            afterParse[4] = new String(OriginalStringStart);
            afterParse[5] = new String(OriginalStringEnd);


            String theTextWeFound = tryDeHash1(afterParse[4], afterParse[5], afterParse[2]);
            if (theTextWeFound == null) {
                //nack
                System.out.println("Sendong NACK");
                String toSendOffer = team  +"\5"+ afterParse[2]  + afterParse[3] ;
                return toSendOffer.getBytes(StandardCharsets.UTF_8);
            } else {
                //ack
                System.out.println("Sendong ACK");
                String toSendOffer = team + "\4" +afterParse[2] + afterParse[3] + theTextWeFound + "";
                return toSendOffer.getBytes(StandardCharsets.UTF_8);
            }
        }
        return null;
    }


    private String hash(String toHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(toHash.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32){
                hashText.insert(0, "0");
            }
            return hashText.toString();
        }
        catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }


    private String tryDeHash1(String startRange, String endRange, String originalHash) {
        BigInteger start = convertStringToInt1(startRange);
        BigInteger end = convertStringToInt1(endRange);
        int length = startRange.length();
        System.out.println("Original Hash: " + originalHash);
        System.out.println("Original Hash Length: " + originalHash.length());
        for(BigInteger i = start; i.compareTo(end) <= 0 ; i=i.add(new BigInteger("1"))){
            String currentString = converxtIntToString1(i, length);
            String hash = hash(currentString);
            if(originalHash.equals(hash)){
                return currentString;
            }
        }
        System.out.println("Finished calculating");
        return null;    }

    private BigInteger convertStringToInt1(String toConvert) {
        System.out.println("at convertStringToInt1 func");
        char[] charArray = toConvert.toCharArray();
        BigInteger num = new BigInteger("0");
        for(char c : charArray){
            if(c < 'a' || c > 'z'){
                throw new RuntimeException();
            }
            num = num.multiply(new BigInteger("26"));
            int x = c - 'a';
            num = num.add(new BigInteger(Integer.toString(x)));
        }
        return num;
    }

    private String converxtIntToString1(BigInteger toConvert, int length) {
        StringBuilder s = new StringBuilder(length);
        while (toConvert.compareTo(new BigInteger("0")) > 0 ){
            BigInteger c = toConvert.mod(new BigInteger("26"));
            s.insert(0, (char) (c.intValue() + 'a'));
            toConvert = toConvert.divide(new BigInteger("26"));
            length --;
        }
        while (length > 0){
            s.insert(0, 'a');
            length--;
        }
        return s.toString();
    }

//    private String tryDeHash(String startRange, String endRange, String originalHash){
//        int start = convertStringToInt(startRange);
//        System.out.println("Start Integer is:" + start);
//        int end = convertStringToInt(endRange);
//        System.out.println("End Integer is:" + end);
//        int length = startRange.length();
//        System.out.println("Original Hash: " + originalHash);
//        System.out.println("Original Hash Length: " + originalHash.length());
//        for(int i = start; i <= end; i++){
//            String currentString = converxtIntToString(i, length);
//            String hash = hash(currentString);
//            if(originalHash.equals(hash)){
//                System.out.println("Found Hash: " + currentString);
//                return currentString;
//            }
//        }
//        System.out.println("Finished calculating");
//        return null;
//    }
//
//    private int convertStringToInt(String toConvert) {
//        char[] charArray = toConvert.toCharArray();
//        int num = 0;
//        for(char c : charArray){
//            if(c < 'a' || c > 'z'){
//                throw new RuntimeException();
//            }
//            num *= 26;
//            num += c - 'a';
//        }
//        return num;
//    }
//
//    private String converxtIntToString(int toConvert, int length) {
//        StringBuilder s = new StringBuilder(length);
//        while (toConvert > 0 ){
//            int c = toConvert % 26;
//            s.insert(0, (char) (c + 'a'));
//            toConvert /= 26;
//            length --;
//        }
//        while (length > 0){
//            s.insert(0, 'a');
//            length--;
//        }
//        return s.toString();
//    }


}