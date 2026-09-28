package com.example.research_project;

import com.example.research_project.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void init() {
        System.out.println("Main.init(): Initialising database...");
        Database.initialise();
    }

    @Override
    public void start(Stage stage) {
        try {
            URL fxmlLocation = getClass().getResource("/com/example/research_project/fxml/main.fxml");
            if (fxmlLocation == null) {
                fxmlLocation = getClass().getResource("/com/example/researchassistant/fxml/main.fxml");
            }

            if (fxmlLocation == null) {
                throw new IOException("Cannot find main.fxml - check the resources folder.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Scene scene = new Scene(loader.load(), 1100, 680);

            // Register and apply current theme and font size
            com.example.research_project.util.ThemeManager.registerScene(scene);

            stage.setTitle("AI Research Assistant");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setScene(scene);
            stage.show();

            System.out.println("Main.start(): UI loaded successfully.");

        } catch (IOException e) {
            System.err.println("ERROR: Failed to load main.fxml");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * stop() is called when the user closes the application.
     * Good place to clean up resources in the future.
     */
    @Override
    public void stop() {
        System.out.println("Main.stop(): Application closing.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}