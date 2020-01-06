import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
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
            serverSocket.setSoTimeout(listeningInterval);
            while (!stop) {
                try {
                    DatagramPacket clientPacket = new DatagramPacket(new byte[1024],1024);
                    serverSocket.receive(clientPacket);
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
            if(numOfClient.addAndGet(1) < 20) {
                byte[] fromClient = splitBytesArrayToStringArray(clientPacket.getData());
                if(fromClient == null){
                    return;
                }
                else {
                    clientSocket.send(new DatagramPacket(fromClient,fromClient.length,clientPacket.getAddress(),clientPacket.getPort()));
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
        byte[] nameOfGroup = new byte[32];
        for (int i = 0; i < 32; i++) {
            nameOfGroup[i] = bytes[i];
        }
        afterParse[0] = new String(nameOfGroup);

        byte[] type = new byte[1];
        type[0] = bytes[32];
        afterParse[1] = new String(type);
        if (afterParse[1].equals("1")) { // Discover
            //TODO: maybe change to my group name
            String toSendOffer = afterParse[0] + "2";
            return toSendOffer.getBytes();
        } else if (afterParse[1].equals("3")) { //Request
            //Get Hash
            byte[] array2 = new byte[40];
            for (int i = 0; i < 40; i++) {
                array2[i] = bytes[i + 33];
            }
            afterParse[2] = new String(array2);

            byte[] originalLength = new byte[1];
            originalLength[0] = bytes[73];
            afterParse[3] = new String(originalLength); //Original Length


            int numOfOriginalString = (bytes.length - 74) / 2;

            byte[] OriginalStringStart = new byte[numOfOriginalString];
            for (int i = 0; i < numOfOriginalString; i++) {
                OriginalStringStart[i] = bytes[i + 74];
            }

            byte[] OriginalStringEnd = new byte[numOfOriginalString];
            for (int i = 0; i < numOfOriginalString; i++) {
                OriginalStringStart[i] = bytes[i + 74 + numOfOriginalString];
            }
            afterParse[4] = new String(OriginalStringStart);
            afterParse[5] = new String(OriginalStringEnd);


            String theTextWeFound = tryDeHash(afterParse[4], afterParse[5], afterParse[2]);
            if (theTextWeFound == null) {
                //nack
                String toSendOffer = team + afterParse[1] + "5"+afterParse[3];
                return toSendOffer.getBytes();
            } else {
                //ack
                String toSendOffer = team + afterParse[1] + "4" + afterParse[3] + "" + theTextWeFound + "";
                return toSendOffer.getBytes();
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

    private String tryDeHash(String startRange, String endRange, String originalHash){
        int start = convertStringToInt(startRange);
        int end = convertStringToInt(endRange);
        int length = startRange.length();
        for(int i = start; i <= end; i++){
            String currentString = converxtIntToString(i, length);
            String hash = hash(currentString);
            if(originalHash.equals(hash)){
                return currentString;
            }
        }
        return null;
    }

    private int convertStringToInt(String toConvert) {
        char[] charArray = toConvert.toCharArray();
        int num = 0;
        for(char c : charArray){
            if(c < 'a' || c > 'z'){
                throw new RuntimeException();
            }
            num *= 26;
            num += c - 'a';
        }
        return num;
    }


    private String converxtIntToString(int toConvert, int length) {
        StringBuilder s = new StringBuilder(length);
        while (toConvert > 0 ){
            int c = toConvert % 26;
            s.insert(0, (char) (c + 'a'));
            toConvert /= 26;
            length --;
        }
        while (length > 0){
            s.insert(0, 'a');
            length--;
        }
        return s.toString();
    }

    public String [] divideToDomains (int stringLength, int numOfServers){
        String [] domains = new String[numOfServers * 2];

        StringBuilder first = new StringBuilder(); //aaa
        StringBuilder last = new StringBuilder(); //zzz

        for(int i = 0; i < stringLength; i++){
            first.append("a"); //aaa
            last.append("z"); //zzz
        }

        int total = convertStringToInt(last.toString());
        int perServer = (int) Math.floor (((double)total) /  ((double)numOfServers));

        domains[0] = first.toString(); //aaa
        domains[domains.length -1 ] = last.toString(); //zzz
        int summer = 0;

        for(int i = 1; i <= domains.length -2; i += 2){
            summer += perServer;
            domains[i] = converxtIntToString(summer, stringLength); //end domain of server
            summer++;
            domains[i + 1] = converxtIntToString(summer, stringLength); //start domain of next server
        }

        return domains;
    }
}