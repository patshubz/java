import java.util.HashMap;
import java.util.Map;

public class Test {
    private int testsPassed = 0;
    private int totalTests = 0;
    
    public void assertTrue(String message, boolean condition) {
        totalTests++;
        if (condition) {
            testsPassed++;
            System.out.println("✓ PASS: " + message);
        } else {
            System.out.println("✗ FAIL: " + message);
        }
    }
    
    public void testJSONParserBasicFunctionality() {
        System.out.println("\n=== Testing JSON Parser Basic Functionality ===");
        
        String json = "{\"title\": \"Test Song\", \"artist\": \"Test Artist\", \"playing?\": true}";
        Map<String, Object> result = Main.SimpleJSONParser.parseJSON(json);
        
        assertTrue(
            "Should parse title correctly",
            "Test Song".equals(Main.SimpleJSONParser.getString(result, "title", ""))
        );
        
        assertTrue(
            "Should parse artist correctly", 
            "Test Artist".equals(Main.SimpleJSONParser.getString(result, "artist", ""))
        );
        
        assertTrue(
            "Should parse boolean correctly",
            Main.SimpleJSONParser.getBoolean(result, "playing?", false)
        );
    }
    
    public void testJSONParserWithMissingFields() {
        System.out.println("\n=== Testing JSON Parser With Missing Fields ===");
        
        String json = "{\"title\": \"Song Only\"}";
        Map<String, Object> result = Main.SimpleJSONParser.parseJSON(json);
        
        assertTrue(
            "Should handle missing artist field with default",
            "Unknown Artist".equals(Main.SimpleJSONParser.getString(result, "artist", "Unknown Artist"))
        );
        
        assertTrue(
            "Should handle missing boolean field with default",
            !Main.SimpleJSONParser.getBoolean(result, "playing?", false)
        );
    }
    
    public void testJSONParserWithEmptyString() {
        System.out.println("\n=== Testing JSON Parser With Empty/Invalid Input ===");
        
        Map<String, Object> result1 = Main.SimpleJSONParser.parseJSON("");
        assertTrue("Should handle empty string", result1.isEmpty());
        
        Map<String, Object> result2 = Main.SimpleJSONParser.parseJSON(null);
        assertTrue("Should handle null input", result2.isEmpty());
        
        Map<String, Object> result3 = Main.SimpleJSONParser.parseJSON("invalid json");
        assertTrue("Should handle invalid JSON", result3.isEmpty());
    }
    
    public void testTrackDisplayApplicationCreation() {
        System.out.println("\n=== Testing Track Display Application Creation ===");
        
        try {
            Main.TrackDisplayApplication app = new Main.TrackDisplayApplication(false);
            
            assertTrue("Application should be created successfully", app != null);
            assertTrue("Title label should be initialized", app.getTrackTitleLabel() != null);
            assertTrue("Artist label should be initialized", app.getArtistNameLabel() != null);
            assertTrue("Length label should be initialized", app.getSongLengthLabel() != null);
            assertTrue("Image label should be initialized", app.getImageDisplayLabel() != null);
            
            app.dispose(); 
        } catch (Exception e) {
            System.err.println("Error creating application: " + e.getMessage());
            assertTrue("Application creation should not throw exception", false);
        }
    }
    
    public void testTrackDisplayUpdate() {
        System.out.println("\n=== Testing Track Display Update ===");
        
        try {
            Main.TrackDisplayApplication app = new Main.TrackDisplayApplication(false);
            
            Map<String, Object> testTrackData = new HashMap<>();
            testTrackData.put("title", "Test Track Update");
            testTrackData.put("artist", "Test Update Artist");
            testTrackData.put("length", "3:45");
            testTrackData.put("image_path", "");
            testTrackData.put("playing?", true);
            
            app.updateTrackDisplayInformation(testTrackData);
            
            Thread.sleep(100);
            
            String titleText = app.getTrackTitleLabel().getText();
            assertTrue("Title should be updated", titleText.contains("Test Track Update"));
            
            String artistText = app.getArtistNameLabel().getText();
            assertTrue("Artist should be updated", artistText.contains("Test Update Artist"));
            
            String lengthText = app.getSongLengthLabel().getText();
            assertTrue("Length should be updated", lengthText.contains("3:45"));
            
            app.dispose(); // Clean up
        } catch (Exception e) {
            System.err.println("Error testing display update: " + e.getMessage());
            assertTrue("Display update should not throw exception", false);
        }
    }
    
    public void testComplexJSONParsing() {
        System.out.println("\n=== Testing Complex JSON Parsing ===");
        
        String complexJson = "{\"title\": \"Song: With Special, Characters\", \"artist\": \"Artist & Co.\", \"length\": \"4:20\", \"image_path\": \"/path/to/image.jpg\", \"playing?\": false}";
        Map<String, Object> result = Main.SimpleJSONParser.parseJSON(complexJson);
        
        assertTrue(
            "Should handle special characters in title",
            Main.SimpleJSONParser.getString(result, "title", "").contains("Special, Characters")
        );
        
        assertTrue(
            "Should handle ampersand in artist",
            Main.SimpleJSONParser.getString(result, "artist", "").contains("&")
        );
        
        assertTrue(
            "Should parse file path correctly",
            "/path/to/image.jpg".equals(Main.SimpleJSONParser.getString(result, "image_path", ""))
        );
        
        assertTrue(
            "Should parse false boolean correctly",
            !Main.SimpleJSONParser.getBoolean(result, "playing?", true)
        );
    }
    
    public void runAllTests() {
        System.out.println("Starting Track Display Application Test Suite");
        System.out.println("============================================");
        
        testJSONParserBasicFunctionality();
        testJSONParserWithMissingFields();
        testJSONParserWithEmptyString();
        testTrackDisplayApplicationCreation();
        testTrackDisplayUpdate();
        testComplexJSONParsing();
        
        System.out.println("\n============================================");
        System.out.println("Test Results Summary:");
        System.out.println("Tests Passed: " + testsPassed + "/" + totalTests);
        System.out.println("Success Rate: " + String.format("%.1f", (double)testsPassed/totalTests * 100) + "%");
        
        if (testsPassed == totalTests) {
            System.out.println("ALL TESTS PASSED! Code is ready for submission.");
        } else {
            System.out.println("Some tests failed. Please review the implementation.");
        }
    }
    
    public static void main(String[] args) {
        Test testSuite = new Test();
        testSuite.runAllTests();
    }
}