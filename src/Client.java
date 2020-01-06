

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Client {
    private InetAddress serverIP;
    private int serverPort;
    private ArrayList<DatagramPacket> listOfServers;

    public Client(InetAddress serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.listOfServers = new ArrayList<>();
    }

    public void start(String[] array) {
        try {
            //datagram socket
            DatagramSocket ds = new DatagramSocket();
//            ds.connect(serverIP, 3117);
            ds.setBroadcast(true);
            // converting array to one big str with the correct template
            //DISCOVER
            String str1 = stringBuildHelperForDiscover();
            DatagramPacket dp = new DatagramPacket(str1.getBytes(), str1.length(), serverIP, 3117);
            ds.send(dp);
            // Waiting to OFFER
            System.out.println("Sent Packet");
            long startTimeMillis = System.currentTimeMillis();
            while (true) {
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - startTimeMillis < 20000) {
                        ds.setSoTimeout((int) (20000 - (currentTimeMillis - startTimeMillis)));
                    } else {
                        break;
                    }
                    byte[] buf = new byte[1024];
                    DatagramPacket dpReceived = new DatagramPacket(buf, 1024, serverIP, 3117);
                    System.out.println("Before Receive");
                    ds.receive(dpReceived);
                    System.out.println("After Receive");
                    String strReceived = new String(dpReceived.getData());
                    // we received offer
                    System.out.println(strReceived);
                    if (strReceived.substring(32, 33).equals("\2")) {
                        listOfServers.add(dpReceived);
                    }
//                    break;
                } catch (IOException e) {
                    break;
                }
            }
            // REQUEST
            System.out.println("number of server is:" + listOfServers.size());
            if (listOfServers.size() == 0) {
                System.out.println("there is no available servers");
                ds.close();
                return;
            }
            String[] temp = divideToDomains(Integer.parseInt(array[1]), listOfServers.size());
            System.out.println("Domains divided " + temp[0] + "|" + temp[1]);
            int index = 0;
            for (DatagramPacket data : listOfServers) {
                if (index + 1 < temp.length) {
                    String str2 = stringBuildHelperForRequest(array, temp[index], temp[index + 1]);
                    DatagramPacket dp2 = new DatagramPacket(str2.getBytes(), str2.length(), data.getAddress(), 3117);
                    System.out.println("Sending Packet to:" + data.getAddress().toString() + ":" + data.getPort());
                    ds.send(dp2);
                    index += 2;
                }
            }
            // waiting for ack to nack
            listOfServers.clear();
            boolean found = false;
            while (true) {
                try {
                    byte[] buf = new byte[1024];
                    DatagramPacket dpReceived1 = new DatagramPacket(buf, 1024);
                    ds.receive(dpReceived1);
                    String strReceived = new String(dpReceived1.getData());
                    // we received offer
                    System.out.println("substring is " + strReceived.substring(32, 33));
                    if (strReceived.substring(32, 33).equals("\4")) {
                        System.out.println("The answer is:" + strReceived.substring(74));
                        found = true;
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
            }
            if (!found) {
                System.out.println("All the servers return NACK");
            }
            //close socket
            ds.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String stringBuildHelperForRequest(String[] array, String startRange, String endRange) {
        String result = "YBETC-Team                      ";
        result += "\3";
        int a = Integer.parseInt(array[1]);
        char c = (char) a;
        result += array[0] +c;
        result += startRange + endRange;
        return result;
    }

    /**
     * @return a string with the protocol template
     */
    private String stringBuildHelperForDiscover() {
        String result = "YBETC-Team                      ";
        result += "\1";
        return result;
    }


    public String[] divideToDomains(int stringLength, int numOfServers) {
        String[] domains = new String[numOfServers * 2];

        StringBuilder first = new StringBuilder(); //aaa
        StringBuilder last = new StringBuilder(); //zzz

        for (int i = 0; i < stringLength; i++) {
            first.append("a"); //aaa
            last.append("z"); //zzz
        }

        int total = convertStringToInt(last.toString());
        int perServer = (int) Math.floor(((double) total) / ((double) numOfServers));

        domains[0] = first.toString(); //aaa
        domains[domains.length - 1] = last.toString(); //zzz
        int summer = 0;

        for (int i = 1; i <= domains.length - 2; i += 2) {
            summer += perServer;
            domains[i] = converxtIntToString(summer, stringLength); //end domain of server
            summer++;
            domains[i + 1] = converxtIntToString(summer, stringLength); //start domain of next server
        }
        return domains;
    }

    private String converxtIntToString(int toConvert, int length) {
        StringBuilder s = new StringBuilder(length);
        while (toConvert > 0) {
            int c = toConvert % 26;
            s.insert(0, (char) (c + 'a'));
            toConvert /= 26;
            length--;
        }
        while (length > 0) {
            s.insert(0, 'a');
            length--;
        }
        return s.toString();
    }


    private int convertStringToInt(String toConvert) {
        char[] charArray = toConvert.toCharArray();
        int num = 0;
        for (char c : charArray) {
            if (c < 'a' || c > 'z') {
                throw new RuntimeException();
            }
            num *= 26;
            num += c - 'a';
        }
        return num;
    }
}
