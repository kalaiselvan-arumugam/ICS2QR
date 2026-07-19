# MyMeetings QR Generator (PC App)

A lightweight, standalone desktop application built with Java 17 and JavaFX that acts as a companion to the MyMeetings Android App. It allows users to drag and drop Microsoft Teams `.ics` calendar invitations and automatically generates highly compressed QR codes that can be scanned by the mobile application.

## 🚀 Features

* **Advanced Minification Engine**: Extracts only the crucial data from bulky `.ics` files (Meeting Title, Start/End Times, Recurrence Rules, and Microsoft Teams join URLs). It compresses this data using GZIP and Base64 encoding to produce a high-efficiency, low-density QR payload.
* **Modern Minimalist UI**: Features an immersive monochromatic dark mode design (`#121212`), custom window controls, and perfect transparent rounded corners without OS-level window frames.
* **Interactive Drag-and-Drop**: Provides a seamless drag-and-drop dropzone interface. Simply drop an `.ics` file onto the app to generate a QR code instantly.
* **Background Execution (System Tray)**: Hides completely from the Windows Taskbar. It docks quietly in the System Tray with a custom gray calendar icon. Closing the window minimizes the app to the tray, keeping it active in the background until explicitly exited via the tray menu.
* **QR Export**: Displays live compression statistics (bytes saved) and offers a one-click "Save Image" button to export the generated QR code to a PNG file.

## 🛠️ Technology Stack

* **Java 17**
* **JavaFX 17** (UI Framework)
* **ZXing 3.5.3** (Zebra Crossing - Core QR Code Generation)
* **Maven** (Build Tool & Dependency Management)

## 📦 Build Instructions

This project uses the Maven Assembly Plugin to build a self-contained executable "fat JAR" containing all required JavaFX and ZXing dependencies.

To compile and package the application, run the following command in the project root directory:

```bash
mvn clean package
```

This will generate the executable JAR file located at:
`target/MyMeetings.jar`

## 🏃 Running the Application

You can run the application directly from the terminal:

```bash
java -jar target/MyMeetings.jar
```

Alternatively, you can double-click the `MyMeetings.jar` file from your file explorer to launch it.

### Usage Instructions
1. Launch the application (the UI will appear, but no taskbar icon will be present).
2. Drag and drop any `.ics` calendar invite into the dashed dropzone area (or click to browse).
3. The app will immediately compress the file and display a QR code.
4. Click **Save Image** to export the QR code as a `.png` file, or simply scan it directly from the screen using the MyMeetings Android App.
5. Click **Clear** to process a new file.
6. Click the **✕** button to hide the window. The app will remain running in your System Tray. Right-click the tray icon and select "Exit" to close the application entirely.
