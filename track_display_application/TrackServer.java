import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class TrackServer {
    
    private static final String[] SAMPLE_TITLES = {
        "Bohemian Rhapsody", "Stairway to Heaven", "Hotel California", 
        "Imagine", "Sweet Child O' Mine", "Billie Jean", "Hey Jude",
        "Smells Like Teen Spirit", "Purple Haze", "Like a Rolling Stone"
    };
    
    private static final String[] SAMPLE_ARTISTS = {
        "Queen", "Led Zeppelin", "Eagles", "John Lennon", "Guns N' Roses",
        "Michael Jackson", "The Beatles", "Nirvana", "Jimi Hendrix", "Bob Dylan"
    };
    
    private static final String[] SAMPLE_LENGTHS = {
        "5:55", "8:02", "6:30", "3:03", "5:03", "4:54", "7:11", "5:01", "2:50", "6:13"
    };
    
    public static void main(String[] args) {
        int port = 532;
        Random random = new Random();
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🎵 Track Server started on port " + port);
            System.out.println("Waiting for Track Display Application to connect...");
            
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("✅ Client connected: " + clientSocket.getInetAddress());
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    
                    // Send 15 track updates with random data
                    for (int i = 0; i < 15; i++) {
                        int trackIndex = random.nextInt(SAMPLE_TITLES.length);
                        boolean isPlaying = random.nextBoolean();
                        
                        String jsonData = String.format(
                            "{\"title\": \"%s\", \"artist\": \"%s\", \"length\": \"%s\", \"image_path\": \"\", \"playing?\": %s}",
                            SAMPLE_TITLES[trackIndex],
                            SAMPLE_ARTISTS[trackIndex], 
                            SAMPLE_LENGTHS[trackIndex],
                            isPlaying
                        );
                        
                        out.println(jsonData);
                        System.out.println("📤 Sent: " + jsonData);
                        
                        // Wait 2-4 seconds between updates for realistic simulation
                        Thread.sleep(2000 + random.nextInt(2000));
                    }
                    
                    System.out.println("🏁 Finished sending track updates. Client will disconnect.");
                    
                } catch (Exception e) {
                    System.err.println("Error handling client: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}