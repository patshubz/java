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
                            result.put(key, value.replaceAll("\"", ""));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
            
            return result;
        }
        
        public static String getString(Map<String, Object> json, String key, String defaultValue) {
            Object value = json.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        
        public static boolean getBoolean(Map<String, Object> json, String key, boolean defaultValue) {
            Object value = json.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return defaultValue;
        }
    }
    
    public static class TrackDisplayApplication extends JFrame {
        public JLabel imageLabel;
        public JLabel titleLabel;
        public JLabel artistLabel;
        public JLabel lengthLabel;
        private JPanel mainPanel;
        private ExecutorService socketExecutor;
        private volatile boolean isRunning = true;

        public TrackDisplayApplication() {
            this(true);
        }
        
        public TrackDisplayApplication(boolean enableSocket) {
            // Check if running in headless mode
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println("Running in headless mode - GUI components disabled");
                return;
            }
            
            initializeUserInterface();
            if (enableSocket) {
                startSocketConnectionListener();
            }
        }

        private void initializeUserInterface() {
            setTitle("Track Display Application");
            setSize(450, 650);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
            mainPanel.setBackground(Color.BLACK);

            mainPanel.add(Box.createVerticalGlue());

            imageLabel = new JLabel();
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(250, 250));
            mainPanel.add(imageLabel);

            mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));

            titleLabel = new JLabel();
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            titleLabel.setForeground(Color.WHITE);
            mainPanel.add(titleLabel);

            mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

            artistLabel = new JLabel();
            artistLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            artistLabel.setFont(new Font("Arial", Font.PLAIN, 24));
            artistLabel.setHorizontalAlignment(SwingConstants.CENTER);
            artistLabel.setForeground(Color.WHITE);
            mainPanel.add(artistLabel);

            mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

            lengthLabel = new JLabel();
            lengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            lengthLabel.setFont(new Font("Monospaced", Font.PLAIN, 20));
            lengthLabel.setHorizontalAlignment(SwingConstants.CENTER);
            lengthLabel.setForeground(Color.WHITE);
            mainPanel.add(lengthLabel);

            mainPanel.add(Box.createVerticalGlue());

            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            add(mainPanel, constraints);

            getContentPane().setBackground(Color.BLACK);
        }

        private void startSocketConnectionListener() {
            socketExecutor = Executors.newSingleThreadExecutor();
            socketExecutor.execute(() -> {
                while (isRunning) {
                    try (Socket clientSocket = new Socket("localhost", 532);
                         BufferedReader socketReader = new BufferedReader(
                             new InputStreamReader(clientSocket.getInputStream()))) {
                        
                        String jsonDataLine;
                        while ((jsonDataLine = socketReader.readLine()) != null && isRunning) {
                            try {
                                Map<String, Object> trackJsonData = SimpleJSONParser.parseJSON(jsonDataLine);
                                updateTrackDisplayInformation(trackJsonData);
                            } catch (Exception jsonParsingException) {
                                System.err.println("JSON parsing error: " + jsonDataLine);
                            }
                        }
                    } catch (IOException connectionException) {
                        if (isRunning) {
                            System.err.println("Socket connection error: " + connectionException.getMessage());
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            });
        }

        public void updateTrackDisplayInformation(Map<String, Object> trackData) {
            // Skip GUI updates in headless mode
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println("Track update (headless): " + trackData);
                return;
            }
            
            SwingUtilities.invokeLater(() -> {
                try {
                    String songTitle = SimpleJSONParser.getString(trackData, "title", "Unknown Title");
                    String artistName = SimpleJSONParser.getString(trackData, "artist", "Unknown Artist");
                    String songLength = SimpleJSONParser.getString(trackData, "length", "0:00");
                    String imageFilePath = SimpleJSONParser.getString(trackData, "image_path", "");
                    boolean isCurrentlyPlaying = SimpleJSONParser.getBoolean(trackData, "playing?", false);

                    updateImageDisplay(imageFilePath);

                    if (isCurrentlyPlaying) {
                        applyGlowingTextEffect(songTitle, artistName, songLength);
                    } else {
                        applyNormalTextEffect(songTitle, artistName, songLength);
                    }

                    if (mainPanel != null) {
                        mainPanel.revalidate();
                        mainPanel.repaint();
                    }
                } catch (Exception uiUpdateException) {
                    System.err.println("UI update error: " + uiUpdateException.getMessage());
                }
            });
        }

        private void updateImageDisplay(String imageFilePath) {
            if (imageLabel == null) return;
            
            if (imageFilePath != null && !imageFilePath.trim().isEmpty()) {
                try {
                    ImageIcon originalImageIcon = new ImageIcon(imageFilePath);
                    if (originalImageIcon.getIconWidth() > 0 && originalImageIcon.getIconHeight() > 0) {
                        int targetSize = 250;
                        Image scaledImage = originalImageIcon.getImage().getScaledInstance(
                            targetSize, targetSize, Image.SCALE_SMOOTH);
                        imageLabel.setIcon(new ImageIcon(scaledImage));
                        imageLabel.setText("");
                    } else {
                        setDefaultImagePlaceholder();
                    }
                } catch (Exception imageLoadException) {
                    setDefaultImagePlaceholder();
                }
            } else {
                setDefaultImagePlaceholder();
            }
        }

        private void setDefaultImagePlaceholder() {
            if (imageLabel == null) return;
            
            imageLabel.setIcon(null);
            imageLabel.setText("♪ No Image ♪");
            imageLabel.setFont(new Font("Arial", Font.ITALIC, 18));
            imageLabel.setForeground(Color.GRAY);
        }

        private void applyGlowingTextEffect(String title, String artist, String length) {
            if (titleLabel != null) {
                titleLabel.setText(title);
                titleLabel.setForeground(Color.CYAN);
            }
            if (artistLabel != null) {
                artistLabel.setText(artist);
                artistLabel.setForeground(Color.CYAN);
            }
            if (lengthLabel != null) {
                lengthLabel.setText(length);
                lengthLabel.setForeground(Color.CYAN);
            }
        }

        private void applyNormalTextEffect(String title, String artist, String length) {
            if (titleLabel != null) {
                titleLabel.setText(title);
                titleLabel.setForeground(Color.WHITE);
            }
            if (artistLabel != null) {
                artistLabel.setText(artist);
                artistLabel.setForeground(Color.WHITE);
            }
            if (lengthLabel != null) {
                lengthLabel.setText(length);
                lengthLabel.setForeground(Color.WHITE);
            }
        }

        public void stopApplication() {
            isRunning = false;
            if (socketExecutor != null && !socketExecutor.isShutdown()) {
                socketExecutor.shutdown();
            }
        }

        @Override
        public void dispose() {
            stopApplication();
            if (!GraphicsEnvironment.isHeadless()) {
                super.dispose();
            }
        }
    }

    public static void main(String[] args) {
        // Only start GUI if not in headless mode
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                TrackDisplayApplication application = new TrackDisplayApplication();
                application.setVisible(true);
            });
        } else {
            System.out.println("Running in headless mode - use Test class for validation");
        }
    }
}