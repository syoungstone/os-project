package GUI;

import Control.OperatingSystem;
import Processes.Template;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class TaskManager extends Application {

    private final int SCENE_WIDTH = 500;
    private final int SCENE_HEIGHT = 400;

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");
        primaryStage.setScene(getStartScene());
        primaryStage.setResizable(false);
        primaryStage.show();
        stage = primaryStage;
    }

    private Scene getStartScene() {
        Label lblStart = new Label("Welcome to Sean Youngstone's OS Simulator!");

        Button btStart = new Button("Boot OS");
        btStart.setOnAction((event) -> {
            stage.setScene(getBootScene());
            OperatingSystem os = OperatingSystem.getInstance();
            os.boot(this);
        });

        FlowPane startPane = new FlowPane(Orientation.VERTICAL);
        startPane.setAlignment(Pos.CENTER);
        startPane.setColumnHalignment(HPos.CENTER);
        startPane.setVgap(20);
        startPane.getChildren().addAll(lblStart, btStart);

        return new Scene(startPane, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private Scene getBootScene() {
        Pane bootPane = new StackPane();
        bootPane.getChildren().add(new Label("Loading templates..."));

        return new Scene(bootPane, SCENE_WIDTH, SCENE_HEIGHT);
    }

    public void requestNumProcesses(List<Template> templates) {
        FlowPane requestPane = new FlowPane(Orientation.VERTICAL);
        requestPane.setAlignment(Pos.CENTER);
        requestPane.setVgap(20);

        Label label = new Label("How many of each process do you wish to run?");
        Label errorLabel = new Label("Error: Not all inputs are integers. Try again.");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);

        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        List<TextField> numTemplatesTextFields = new ArrayList<>();
        for (int i = 0 ; i < templates.size() ; i++) {
            TextField textField = new TextField();
            gridPane.add(new Label(templates.get(i).name() + ":"), 0, i);
            gridPane.add(textField, 1, i);
            numTemplatesTextFields.add(textField);
        }

        Button btSubmit = new Button("Submit");
        btSubmit.setOnAction((event) -> {
            List<Integer> processesPerTemplate = new ArrayList<>();
            try {
                for (TextField numTemplatesTextField : numTemplatesTextFields) {
                    int processes = Integer.parseInt(numTemplatesTextField.getCharacters().toString());
                    processesPerTemplate.add(processes);
                }
                OperatingSystem os = OperatingSystem.getInstance();
                os.createProcesses(templates, processesPerTemplate);
            } catch (NumberFormatException e) {
                errorLabel.setVisible(true);
            }
        });
        gridPane.add(btSubmit, 1, templates.size());
        GridPane.setHalignment(btSubmit, HPos.RIGHT);

        requestPane.getChildren().addAll(label, gridPane, errorLabel);

        stage.setScene(new Scene(requestPane, SCENE_WIDTH, SCENE_HEIGHT));
    }

    public void requestNumCycles() {
        Label label1 = new Label("Choose number of cycles to run before halting:");
        TextField textField = new TextField();

        Label errorLabel = new Label("Error: Not an integer. Try again.");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);

        Button btSubmit = new Button("Submit");
        btSubmit.setOnAction((event) -> {
            try {
                int numCycles = Integer.parseInt(textField.getCharacters().toString());
                OperatingSystem os = OperatingSystem.getInstance();
                os.setMaxCycles(numCycles);
                Thread thread = new Thread(os::runOS);
                thread.start();
                stage.setScene(getRunningScene());
            } catch (NumberFormatException e) {
                errorLabel.setVisible(true);
            }
        });

        Label label2 = new Label("OR:");

        Button btNoLimit = new Button("Run With No Cycle Limit");
        btNoLimit.setOnAction((event) -> {
            OperatingSystem os = OperatingSystem.getInstance();
            os.setMaxCycles(Long.MAX_VALUE);
            Thread thread = new Thread(os::runOS);
            thread.start();
            stage.setScene(getRunningScene());
        });

        FlowPane cyclesPane = new FlowPane(Orientation.VERTICAL);
        cyclesPane.setAlignment(Pos.CENTER);
        cyclesPane.setColumnHalignment(HPos.CENTER);
        cyclesPane.setVgap(20);
        cyclesPane.getChildren().addAll(label1, textField, btSubmit, label2, btNoLimit, errorLabel);

        stage.setScene(new Scene(cyclesPane, SCENE_WIDTH, SCENE_HEIGHT));
    }

    private Scene getRunningScene() {

        Label runningLabel = new Label("OS now running...");

        Pane runningPane = new StackPane();
        runningPane.getChildren().add(runningLabel);

        return new Scene(runningPane, SCENE_WIDTH, SCENE_HEIGHT);
    }

}
