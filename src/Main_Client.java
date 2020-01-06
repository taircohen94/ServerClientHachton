
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main_Client {
    public static void main(String[] args) {
        ConnectToServer();
    }

    private static void ConnectToServer() {
        try {
            Client client = new Client(InetAddress.getByName("255.255.255.255"), 3117);
            String[] res = inputFromClient();
            client.start(res);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static String[] inputFromClient() {
        boolean flag = true;
        String[] res = new String[2];
        String hash = "";
        String stringLength = "";
        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        while (flag) {
            System.out.println("Welcome to YBETC-Team. Please enter the hash:");
            hash = myObj.nextLine();
            // Read user input
            if (hash.length() != 40) {
                System.out.println("Please enter valid argument");
                continue;
            }
            flag = false;
        }
        res[0] = hash;
        flag = true;
        while (flag) {
            System.out.println("Please enter the input string length:");
            stringLength = myObj.nextLine();
            if (stringLength.length() >= 10 || stringLength.length() < 0) {
                System.out.println("Please enter valid argument");
                continue;
            }
            flag = false;
        }
        res[1] = stringLength;
        return res;
    }
}
