package com.mymeetings.qr.generator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

public class Main extends Application {

    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;

    // UI Panel References
    private VBox dropzoneView;
    private VBox qrPreviewView;
    
    // QR State Variables
    private String minifiedPayload = null;
    private String originalFileName = "";
    private long originalSize = 0;

    // View Components
    private ImageView qrImageView;
    private Label statsLabel;
    private Label filenameLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Prevent JavaFX from exiting when main stage is hidden
        Platform.setImplicitExit(false);

        // Hide app from taskbar using invisible Utility Stage trick
        Stage dummyStage = new Stage(StageStyle.UTILITY);
        dummyStage.setOpacity(0);
        dummyStage.setWidth(0);
        dummyStage.setHeight(0);
        dummyStage.show();

        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(dummyStage);
        stage.setTitle("MyMeetings QR Linker");

        // Set up the UI layout container
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #121212; -fx-border-color: #333333; -fx-border-width: 1px; -fx-background-radius: 12px; -fx-border-radius: 12px;");
        root.setPrefSize(320, 440);

        // 1. Custom Title Bar Layout
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 14, 10, 14));
        titleBar.setStyle("-fx-background-color: #000000; -fx-background-radius: 12px 12px 0px 0px;");

        // Draggable window handlers
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        Label titleLabel = new Label("MyMeetings Linker v1.0.0");
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Spacer to push window actions right
        StackPane spacer = new StackPane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        String btnCloseNormalStyle = "-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;";
        String btnCloseHoverStyle = "-fx-background-color: #333333; -fx-text-fill: #FFFFFF; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;";
        btnClose.setStyle(btnCloseNormalStyle);
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnCloseHoverStyle));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(btnCloseNormalStyle));
        btnClose.setOnAction(e -> stage.hide()); // Hide to system tray

        titleBar.getChildren().addAll(titleLabel, spacer, btnClose);

        // 2. Main Content Card Panes
        StackPane contentPane = new StackPane();
        contentPane.setPadding(new Insets(20));

        // Create Panel 1: Drag-and-Drop Selector
        createDropzonePanel();

        // Create Panel 2: QR Code Preview Box
        createPreviewPanel();

        contentPane.getChildren().addAll(dropzoneView, qrPreviewView);
        qrPreviewView.setVisible(false); // Initially hide preview

        root.getChildren().addAll(titleBar, contentPane);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT); // Support transparent curved window edges
        stage.setScene(scene);

        // Set up the System Tray Icon
        setupSystemTray();

        // Show window initially in screen center
        stage.show();
    }

    private void createDropzonePanel() {
        dropzoneView = new VBox(16);
        dropzoneView.setAlignment(Pos.CENTER);
        dropzoneView.setPrefSize(280, 340);
        dropzoneView.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #555555; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-cursor: hand;");

        Label iconLabel = new Label("📅");
        iconLabel.setStyle("-fx-font-size: 40px;");

        Label promptLabel = new Label("Drag & Drop invite .ics file");
        promptLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label subPromptLabel = new Label("or Click to browse computer");
        subPromptLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 11px;");

        dropzoneView.getChildren().addAll(iconLabel, promptLabel, subPromptLabel);

        // Drag and drop event handlers
        dropzoneView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                dropzoneView.setStyle("-fx-background-color: #2D2D2D; -fx-border-color: #888888; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
            }
            event.consume();
        });

        dropzoneView.setOnDragExited(event -> {
            dropzoneView.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #555555; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
            event.consume();
        });

        dropzoneView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                processIcsFile(file);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Click to browse
        dropzoneView.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select iCalendar Invite (.ics)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Calendar Files", "*.ics"));
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                processIcsFile(file);
            }
        });
    }

    private void createPreviewPanel() {
        qrPreviewView = new VBox(14);
        qrPreviewView.setAlignment(Pos.CENTER);
        qrPreviewView.setPrefSize(280, 340);

        // Bounding wrapper card for the QR code image
        VBox qrWrapper = new VBox();
        qrWrapper.setPadding(new Insets(0));
        qrWrapper.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-alignment: center;");
        qrWrapper.setPrefSize(260, 260);
        qrWrapper.setMaxSize(260, 260);

        qrImageView = new ImageView();
        qrImageView.setFitWidth(260);
        qrImageView.setFitHeight(260);
        qrWrapper.getChildren().add(qrImageView);

        filenameLabel = new Label();
        filenameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px;");
        filenameLabel.setWrapText(true);

        statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 11px;");

        // Operation Action buttons
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER);

        Button btnSave = new Button("Save Image");
        String btnSaveNormalStyle = "-fx-background-color: #333333; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: hand;";
        String btnSaveHoverStyle = "-fx-background-color: #555555; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: hand;";
        btnSave.setStyle(btnSaveNormalStyle);
        btnSave.setOnMouseEntered(e -> btnSave.setStyle(btnSaveHoverStyle));
        btnSave.setOnMouseExited(e -> btnSave.setStyle(btnSaveNormalStyle));
        btnSave.setOnAction(e -> saveQrCodeToFile());

        Button btnReset = new Button("Clear");
        String btnResetNormalStyle = "-fx-background-color: #1E1E1E; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: hand;";
        String btnResetHoverStyle = "-fx-background-color: #2D2D2D; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-cursor: hand;";
        btnReset.setStyle(btnResetNormalStyle);
        btnReset.setOnMouseEntered(e -> btnReset.setStyle(btnResetHoverStyle));
        btnReset.setOnMouseExited(e -> btnReset.setStyle(btnResetNormalStyle));
        btnReset.setOnAction(e -> {
            qrPreviewView.setVisible(false);
            dropzoneView.setVisible(true);
            minifiedPayload = null;
        });

        actionsRow.getChildren().addAll(btnSave, btnReset);

        qrPreviewView.getChildren().addAll(qrWrapper, filenameLabel, statsLabel, actionsRow);
    }

    private void processIcsFile(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            originalFileName = file.getName();
            originalSize = file.length();

            // Run compression & minifier
            minifiedPayload = Minifier.minifyAndEncode(content);
            
            // Build the QR matrix image preview
            javafx.scene.image.WritableImage qrImage = QrCodeGenerator.generateQrImage(minifiedPayload, 260, 260);
            qrImageView.setImage(qrImage);

            // Compute statistics
            double saving = 100.0 - (((double) minifiedPayload.length() / (double) originalSize) * 100.0);
            DecimalFormat df = new DecimalFormat("#.#");
            
            filenameLabel.setText(originalFileName);
            statsLabel.setText(String.format("Original: %s KB  ->  Minified: %s bytes\n(%s%% bytes saved)",
                    df.format((double) originalSize / 1024.0),
                    minifiedPayload.length(),
                    df.format(saving)));

            // Swap UI Panels
            dropzoneView.setVisible(false);
            qrPreviewView.setVisible(true);

        } catch (Exception e) {
            showErrorToast("Failed to parse or minify iCalendar file.");
        }
    }

    private void saveQrCodeToFile() {
        if (minifiedPayload == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save QR Code Image");
        fileChooser.setInitialFileName(originalFileName.replace(".ics", "_QR.png"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                QrCodeGenerator.saveQrToFile(minifiedPayload, 350, 350, file.toPath());
                showSuccessToast("QR code image saved successfully.");
            } catch (Exception e) {
                showErrorToast("Failed to save QR code image.");
            }
        }
    }

    private void setupSystemTray() {
        if (!java.awt.SystemTray.isSupported()) {
            return;
        }

        java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
        java.awt.Image trayIconImage = createTrayIconImage();

        java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(trayIconImage, "MyMeetings QR Generator");
        trayIcon.setImageAutoSize(true);

        // Toggle window visibility on tray icon click
        trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    Platform.runLater(() -> {
                        if (stage.isShowing()) {
                            stage.hide();
                        } else {
                            stage.show();
                            stage.toFront();
                        }
                    });
                }
            }
        });

        // Tray Context Menu Options
        java.awt.PopupMenu menu = new java.awt.PopupMenu();
        
        java.awt.MenuItem showItem = new java.awt.MenuItem("Open Generator");
        showItem.addActionListener(e -> Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        }));
        
        java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            Platform.exit();
            System.exit(0);
        });

        menu.add(showItem);
        menu.addSeparator();
        menu.add(exitItem);
        trayIcon.setPopupMenu(menu);

        try {
            tray.add(trayIcon);
        } catch (java.awt.AWTException e) {
            // Ignore
        }
    }

    private java.awt.Image createTrayIconImage() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw dark gray background
        g2.setColor(new java.awt.Color(0x33, 0x33, 0x33));
        g2.fillRoundRect(0, 0, 16, 16, 5, 5);
        
        // Draw inner calendar binder page corners in white
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRoundRect(3, 3, 10, 10, 2, 2);
        
        // Draw grid details in dark gray
        g2.setColor(new java.awt.Color(0x33, 0x33, 0x33));
        g2.fillRect(5, 5, 2, 2);
        g2.fillRect(9, 5, 2, 2);
        g2.fillRect(5, 9, 2, 2);
        g2.fillRect(9, 9, 1, 1);
        
        g2.dispose();
        return image;
    }

    private void showSuccessToast(String msg) {
        Platform.runLater(() -> {
            // A simple terminal confirmation or platform popup fallback
            System.out.println("SUCCESS: " + msg);
        });
    }

    private void showErrorToast(String msg) {
        Platform.runLater(() -> {
            System.err.println("ERROR: " + msg);
        });
    }
}
