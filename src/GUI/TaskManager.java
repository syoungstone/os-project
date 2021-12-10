package GUI;

import Control.OperatingSystem;
import Processes.PCB;
import Processes.Priority;
import Processes.Template;
import javafx.application.Application;
import javafx.application.Platform;
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
    private final int SCENE_HEIGHT = 600;

    private boolean halted;
    private Label statusLabel;
    private Label numCyclesLabel;
    private Button btToggleExecution;
    private GridPane processPane;

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
                stage.setScene(getRunningScene());
                Thread thread = new Thread(os::runOS);
                thread.start();
            } catch (NumberFormatException e) {
                errorLabel.setVisible(true);
            }
        });

        Label label2 = new Label("OR:");

        Button btNoLimit = new Button("Run With No Cycle Limit");
        btNoLimit.setOnAction((event) -> {
            OperatingSystem os = OperatingSystem.getInstance();
            os.setMaxCycles(Long.MAX_VALUE);
            stage.setScene(getRunningScene());
            Thread thread = new Thread(os::runOS);
            thread.start();
        });

        FlowPane cyclesPane = new FlowPane(Orientation.VERTICAL);
        cyclesPane.setAlignment(Pos.CENTER);
        cyclesPane.setColumnHalignment(HPos.CENTER);
        cyclesPane.setVgap(20);
        cyclesPane.getChildren().addAll(label1, textField, btSubmit, label2, btNoLimit, errorLabel);

        stage.setScene(new Scene(cyclesPane, SCENE_WIDTH, SCENE_HEIGHT));
    }

    private Scene getRunningScene() {

        halted = false;

        FlowPane runningPane = new FlowPane(Orientation.VERTICAL);
        runningPane.setAlignment(Pos.CENTER);
        runningPane.setVgap(20);
        runningPane.setColumnHalignment(HPos.CENTER);

        Label statsLabel = new Label("Operating System Stats:");

        GridPane statsPane = new GridPane();
        statsPane.setAlignment(Pos.CENTER);
        statsPane.setHgap(10);
        statsPane.setVgap(10);

        Label statusHeaderLabel = new Label("STATUS");
        Label numCyclesHeaderLabel = new Label("ELAPSED CYCLES");
        statusLabel = new Label("OS is running");
        numCyclesLabel = new Label("0");

        statsPane.add(statusHeaderLabel, 0, 0);
        statsPane.add(statusLabel, 1, 0);
        statsPane.add(numCyclesHeaderLabel, 0, 1);
        statsPane.add(numCyclesLabel, 1, 1);

        GridPane.setHalignment(statusHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(numCyclesHeaderLabel, HPos.RIGHT);

        Label processesLabel = new Label("Currently Executing Processes:");

        processPane = new GridPane();
        processPane.setAlignment(Pos.CENTER);
        processPane.setHgap(10);
        processPane.setVgap(10);

        btToggleExecution = new Button("Halt Execution");
        btToggleExecution.setOnAction((event) -> {
            if (halted) {
                halted = false;
                btToggleExecution.setText("Halt Execution");
                statusLabel.setText("OS is running");
                // OperatingSystem.getInstance().resume();
            } else {
                halted = true;
                btToggleExecution.setText("Resume Execution");
                statusLabel.setText("OS halted by user");
                // OperatingSystem.getInstance().halt();
            }
        });

        runningPane.getChildren().addAll(statsLabel, statsPane, processesLabel, processPane, btToggleExecution);

        return new Scene(runningPane, SCENE_WIDTH, SCENE_HEIGHT);
    }

    public void updateRunningScene(List<PCB> runningProcesses, long elapsedCycles) {
        Platform.runLater(() -> {
            numCyclesLabel.setText(Long.toString(elapsedCycles));
            processPane.getChildren().clear();
            processPane.add(new Label("PID"), 0, 0);
            processPane.add(new Label("Priority"), 1, 0);
            processPane.add(new Label("Template File Name"), 2, 0);
            if (runningProcesses.size() > 0) {
                for (int i = 0; i < runningProcesses.size(); i++) {
                    PCB p = runningProcesses.get(i);
                    processPane.add(new Label(Integer.toString(p.getPid())), 0, i + 1);
                    Priority priority = p.getPriority();
                    String priorityString;
                    if (priority == Priority.HIGH) {
                        priorityString = "HIGH";
                    } else if (priority == Priority.MEDIUM) {
                        priorityString = "MEDIUM";
                    } else {
                        priorityString = "LOW";
                    }
                    processPane.add(new Label(priorityString), 1, i + 1);
                    processPane.add(new Label(p.getTemplateName()), 2, i + 1);
                }
            }
            for (int i = runningProcesses.size() ; i < 8 ; i++) {
                processPane.add(new Label("---"), 0, i + 1);
                processPane.add(new Label("---"), 1, i + 1);
                processPane.add(new Label("---"), 2, i + 1);
            }
        });
    }

    public void setHalted() {
        Platform.runLater(() -> {
            halted = true;
            statusLabel.setText("OS halted, max cycles reached");
            btToggleExecution.setText("Resume Execution");
        });
    }

    public void setCompleted() {
        Platform.runLater(() -> {
            FlowPane runningPane = new FlowPane(Orientation.VERTICAL);
            runningPane.setAlignment(Pos.CENTER);
            runningPane.setVgap(20);

            Label completedLabel = new Label("All processes terminated. Goodbye!");

            runningPane.getChildren().addAll(completedLabel);

            stage.setScene(new Scene(runningPane, SCENE_WIDTH, SCENE_HEIGHT));
        });
    }

}
