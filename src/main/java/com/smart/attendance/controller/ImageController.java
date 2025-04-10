package com.smart.attendance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smart.attendance.config.AppProperties;
import com.smart.attendance.utils.ImageUtils;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private AppProperties appProperties;

    private static final Logger LOGGER = Logger.getLogger(ImageController.class.getName());

    private String IMAGE_DIRECTORY = ""; // ✅ Set your image directory

    @PostConstruct
    public void init() {
        IMAGE_DIRECTORY = appProperties.getImagesDir();
        LOGGER.info("✔️ Image directory loaded: " + IMAGE_DIRECTORY);
    }

    // ✅ Get all images' URLs
    @GetMapping
    public ResponseEntity<List<String>> getAllImages() {
        File folder = new File(IMAGE_DIRECTORY);
        List<String> imageUrls = new ArrayList<>();

        // Log the attempt to access the image directory
        LOGGER.info("📂 Accessing image directory: " + IMAGE_DIRECTORY);

        if (!folder.exists() || !folder.isDirectory()) {
            LOGGER.warning("❌ Image directory not found: " + IMAGE_DIRECTORY);
            return ResponseEntity.badRequest().body(List.of("❌ Image directory not found!"));
        }

        File[] files = folder.listFiles();
        if (files != null) {
            // Log the number of files found in the directory
            LOGGER.info("📄 Found " + files.length + " files in the image directory.");

            for (File file : files) {
                if (file.isFile() && isImage(file.getName())) {
                    imageUrls.add("/images/view/" + file.getName()); // ✅ Serve images via API
                    LOGGER.info("✅ Added image to list: " + file.getName());
                }
            }
        }

        return ResponseEntity.ok(imageUrls);
    }

    // ✅ Endpoint to serve images
    @GetMapping("/view/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        LOGGER.info("🔍 Requesting image: " + filename);
        return ImageUtils.serveImage(IMAGE_DIRECTORY + filename);
    }

    // ✅ Helper function to check image file extensions
    private boolean isImage(String filename) {
        String lower = filename.toLowerCase();
        boolean isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        if (!isImage) {
            LOGGER.warning("❌ Invalid image file: " + filename);
        }
        return isImage;
    }
}
