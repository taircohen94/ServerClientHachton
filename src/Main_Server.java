

import java.util.Scanner;

public class Main_Server {
    public static void main(String[] args){
        StartingServer();
    }

    private static void StartingServer() {
        Server server = new Server(
                3117,
                1000);
        server.start();
        StartCLI();
        server.stop();
    }

    private static void StartCLI(){
        System.out.println("Server started!");
        System.out.println("Enter 'exit' to close server.");
        Scanner reader = new Scanner(System.in);

        do
        {
            System.out.print(">>");
        } while (!reader.next().toLowerCase().equals("exit"));
    }
}