import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.HashMap;
import java.util.Map;

public class Main {
    
    public static class SimpleJSONParser {
        
        public static Map<String, Object> parseJSON(String jsonString) {
            Map<String, Object> result = new HashMap<>();
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return result;
            }
            
            try {
                jsonString = jsonString.trim();
                if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
                    return result;
                }
                
                String content = jsonString.substring(1, jsonString.length() - 1);
                String[] pairs = content.split(",");
                
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("\"", "");
                        String value = keyValue[1].trim();
                        
                        if (value.equals("true") || value.equals("false")) {
                            result.put(key, Boolean.parseBoolean(value));
                        } else if (value.startsWith("\"") && value.endsWith("\"")) {
                            result.put(key, value.substring(1, value.length() - 1));
                        } else {
                            result.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
            
            return result;
        }
        
        public static String getString(Map<String, Object> data, String key, String defaultValue) {
            Object value = data.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        
        public static boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
            Object value = data.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return defaultValue;
        }
    }
    
    public static class TrackDisplayApplication extends JFrame {
        private JLabel imageDisplayLabel;
        private JLabel trackTitleLabel;
        private JLabel artistNameLabel;
        private JLabel songLengthLabel;
        private JPanel mainContentPanel;
        private ExecutorService socketExecutor;
        private volatile boolean isSocketListenerRunning = false;
        private boolean enableSocketConnection;
        
        public TrackDisplayApplication() {
            this(true);
        }
        
        public TrackDisplayApplication(boolean enableSocket) {
            this.enableSocketConnection = enableSocket;
            initializeUserInterface();
            if (enableSocket) {
                startSocketConnectionListener();
            }
        }
        
        private void initializeUserInterface() {
            setTitle("Track Display Application");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(500, 700);
            
            mainContentPanel = new JPanel();
            mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
            mainContentPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
            mainContentPanel.setBackground(Color.BLACK);
            
            // Add vertical glue for centering
            mainContentPanel.add(Box.createVerticalGlue());
            
            // Image display
            imageDisplayLabel = new JLabel();
            imageDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            imageDisplayLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainContentPanel.add(imageDisplayLabel);
            mainContentPanel.add(Box.createRigidArea(new Dimension(0, 25)));
            
            // Track title
            trackTitleLabel = new JLabel();
            trackTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            trackTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
            trackTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            trackTitleLabel.setForeground(Color.WHITE);
            mainContentPanel.add(trackTitleLabel);
            mainContentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            
            // Artist name
            artistNameLabel = new JLabel();
            artistNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            artistNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            artistNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            artistNameLabel.setForeground(Color.WHITE);
            mainContentPanel.add(artistNameLabel);
            mainContentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            
            // Song length
            songLengthLabel = new JLabel();
            songLengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            songLengthLabel.setFont(new Font("Monospaced", Font.PLAIN, 20));
            songLengthLabel.setHorizontalAlignment(SwingConstants.CENTER);
            songLengthLabel.setForeground(Color.WHITE);
            mainContentPanel.add(songLengthLabel);
            
            // Add vertical glue for centering
            mainContentPanel.add(Box.createVerticalGlue());
            
            setLayout(new GridBagLayout());
            add(mainContentPanel, new GridBagConstraints());
            
            setLocationRelativeTo(null);
        }
        
        private void startSocketConnectionListener() {
            if (!enableSocketConnection) return;
            
            socketExecutor = Executors.newSingleThreadExecutor();
            isSocketListenerRunning = true;
            
            socketExecutor.execute(() -> {
                while (isSocketListenerRunning) {
                    try (Socket clientSocket = new Socket("localhost", 532);
                         BufferedReader socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        
                        System.out.println("Successfully connected to server on port 532");
                        String receivedLine;
                        
                        while (isSocketListenerRunning && (receivedLine = socketReader.readLine()) != null) {
                            try {
                                Map<String, Object> trackData = SimpleJSONParser.parseJSON(receivedLine);
                                updateTrackDisplayInformation(trackData);
                            } catch (Exception jsonException) {
                                System.err.println("Failed to parse JSON data: " + receivedLine);
                                jsonException.printStackTrace();
                            }
                        }
                    } catch (IOException connectionException) {
                        if (isSocketListenerRunning) {
                            System.err.println("Connection error: " + connectionException.getMessage());
                            
                            // Auto-reconnect attempt
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException interruptedException) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            });
        }
        
        public void updateTrackDisplayInformation(Map<String, Object> trackInformation) {
            SwingUtilities.invokeLater(() -> {
                try {
                    String songTitle = SimpleJSONParser.getString(trackInformation, "title", "");
                    String artistName = SimpleJSONParser.getString(trackInformation, "artist", "");
                    String trackLength = SimpleJSONParser.getString(trackInformation, "length", "");
                    String imagePath = SimpleJSONParser.getString(trackInformation, "image_path", "");
                    boolean isCurrentlyPlaying = SimpleJSONParser.getBoolean(trackInformation, "playing?", false);
                    
                    updateImageDisplay(imagePath);
                    updateTextDisplayElements(songTitle, artistName, trackLength, isCurrentlyPlaying);
                    
                    mainContentPanel.revalidate();
                    mainContentPanel.repaint();
                } catch (Exception updateException) {
                    System.err.println("Error updating display: " + updateException.getMessage());
                    updateException.printStackTrace();
                }
            });
        }
        
        private void updateImageDisplay(String imageFilePath) {
            ImageIcon displayIcon = null;
            if (imageFilePath != null && !imageFilePath.trim().isEmpty()) {
                try {
                    ImageIcon originalIcon = new ImageIcon(imageFilePath);
                    if (originalIcon.getIconWidth() > 0 && originalIcon.getIconHeight() > 0) {
                        int imageSize = 250;
                        Image scaledImage = originalIcon.getImage().getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH);
                        displayIcon = new ImageIcon(scaledImage);
                    }
                } catch (Exception imageException) {
                    System.err.println("Failed to load image: " + imageFilePath);
                }
            }
            imageDisplayLabel.setIcon(displayIcon);
        }
        
        private void updateTextDisplayElements(String title, String artist, String length, boolean isPlaying) {
            if (isPlaying) {
                String glowingStyle = "color: #00FFFF; text-shadow: 0 0 15px #00FFFF, 0 0 10px #8FFFFF;";
                trackTitleLabel.setText(String.format(
                    "<html><div style='text-align:center;%s font-size:32pt;'>%s</div></html>", 
                    glowingStyle, escapeHtmlCharacters(title)
                ));
                artistNameLabel.setText(String.format(
                    "<html><div style='text-align:center;%s font-size:24pt;'>%s</div></html>", 
                    glowingStyle, escapeHtmlCharacters(artist)
                ));
                songLengthLabel.setText(String.format(
                    "<html><div style='text-align:center;%s font-size:20pt;'>%s</div></html>", 
                    glowingStyle, escapeHtmlCharacters(length)
                ));
            } else {
                String normalStyle = "color: #CCCCCC;";
                trackTitleLabel.setText(String.format(
                    "<html><div style='text-align:center;%s font-size:32pt;'>%s</div></html>", 
                    normalStyle, escapeHtmlCharacters(title)
                ));
                artistNameLabel.setText(String.format(
                    "<html><div style='text-align:center;%s font-size:24pt;'>%s</div></html>", 
                    normalStyle, escapeHtmlCharacters(artist)
                ));
                songLengthLabel.setText(String.format(
                    "<html><div style='text-align:center;%s font-size:20pt;'>%s</div></html>", 
                    normalStyle, escapeHtmlCharacters(length)
                ));
            }
        }
        
        private static String escapeHtmlCharacters(String inputText) {
            if (inputText == null) return "";
            return inputText.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#39;");
        }
        
        public void stopSocketConnectionListener() {
            isSocketListenerRunning = false;
            if (socketExecutor != null) {
                socketExecutor.shutdownNow();
            }
        }
        
        public boolean isSocketListenerActive() {
            return isSocketListenerRunning;
        }
        
        // For testing purposes
        public JLabel getImageDisplayLabel() { return imageDisplayLabel; }
        public JLabel getTrackTitleLabel() { return trackTitleLabel; }
        public JLabel getArtistNameLabel() { return artistNameLabel; }
        public JLabel getSongLengthLabel() { return songLengthLabel; }
    }
    
    public static void main(String[] args) {
        // Support headless mode for testing
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Running in headless mode - GUI disabled");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set system look and feel: " + e.getMessage());
            }
            
            TrackDisplayApplication application = new TrackDisplayApplication();
            application.setVisible(true);
        });
    }
}