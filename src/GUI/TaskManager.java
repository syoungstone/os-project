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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class TaskManager extends Application {

    private final int INITIAL_SCENE_WIDTH = 300;
    private final int INITIAL_SCENE_HEIGHT = 300;

    private final int RUNNING_SCENE_WIDTH = 800;
    private final int RUNNING_SCENE_HEIGHT = 550;

    private boolean halted;
    private Label statusLabel;
    private Label timeLabel;
    private Label numCyclesLabel;
    private Label runningLabel;
    private Label readyLabel;
    private Label ioLabel;
    private Label resourcesLabel;
    private Label criticalLabel;
    private Label terminatedLabel;
    private Label processorStatsLabel;
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

        return new Scene(startPane, INITIAL_SCENE_WIDTH, INITIAL_SCENE_HEIGHT);
    }

    private Scene getBootScene() {
        Pane bootPane = new StackPane();
        bootPane.getChildren().add(new Label("Loading templates..."));

        return new Scene(bootPane, INITIAL_SCENE_WIDTH, INITIAL_SCENE_HEIGHT);
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

        stage.setScene(new Scene(requestPane, INITIAL_SCENE_WIDTH, INITIAL_SCENE_HEIGHT));
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

        stage.setScene(new Scene(cyclesPane, INITIAL_SCENE_WIDTH, INITIAL_SCENE_HEIGHT));
    }

    private Scene getRunningScene() {

        halted = false;

        FlowPane runningPane = new FlowPane(Orientation.VERTICAL);
        runningPane.setAlignment(Pos.CENTER);
        runningPane.setVgap(20);
        runningPane.setColumnHalignment(HPos.CENTER);

        Label topLabel = new Label("OS Simulator");
        topLabel.setStyle("-fx-font-size: 18");

        GridPane centralPane = new GridPane();
        centralPane.setAlignment(Pos.CENTER);
        centralPane.setHgap(10);
        centralPane.setVgap(10);

        Label statusHeaderLabel = new Label("Status:");
        Label timeHeaderLabel = new Label("Elapsed Time:");
        Label numCyclesHeaderLabel = new Label("Elapsed Cycles:");

        statusLabel = new Label("OS is running");
        timeLabel = new Label("00 min, 00 sec, 000 ms");
        numCyclesLabel = new Label("0");

        centralPane.add(statusHeaderLabel, 0, 0);
        centralPane.add(statusLabel, 1, 0);
        centralPane.add(timeHeaderLabel, 0, 1);
        centralPane.add(timeLabel, 1, 1);
        centralPane.add(numCyclesHeaderLabel, 0, 2);
        centralPane.add(numCyclesLabel, 1, 2);

        GridPane.setHalignment(statusHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(numCyclesHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(timeHeaderLabel, HPos.RIGHT);

        Label statsLabel = new Label("Process Stats:");
        statsLabel.setStyle("-fx-font-size: 18");

        GridPane statsPane = new GridPane();
        statsPane.setAlignment(Pos.CENTER);
        statsPane.setHgap(10);
        statsPane.setVgap(10);

        Label runningHeaderLabel = new Label("Running:");
        Label readyHeaderLabel = new Label("In Ready Queue:");
        Label ioHeaderLabel = new Label("Executing I/O Cycles:");
        Label resourcesHeaderLabel = new Label("Waiting on Resources:");
        Label criticalHeaderLabel = new Label("Waiting on Critical Section:");
        Label terminatedHeaderLabel = new Label("Terminated:");

        runningLabel = new Label("0");
        readyLabel = new Label("0");
        ioLabel = new Label("0");
        resourcesLabel = new Label("0");
        criticalLabel = new Label("0");
        terminatedLabel = new Label("0");

        statsPane.add(runningHeaderLabel, 0, 0);
        statsPane.add(runningLabel, 1, 0);
        statsPane.add(readyHeaderLabel, 0, 1);
        statsPane.add(readyLabel, 1, 1);
        statsPane.add(ioHeaderLabel, 0, 2);
        statsPane.add(ioLabel, 1, 2);
        statsPane.add(resourcesHeaderLabel, 0, 3);
        statsPane.add(resourcesLabel, 1, 3);
        statsPane.add(criticalHeaderLabel, 0, 4);
        statsPane.add(criticalLabel, 1, 4);
        statsPane.add(terminatedHeaderLabel, 0, 5);
        statsPane.add(terminatedLabel, 1, 5);

        GridPane.setHalignment(runningHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(readyHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(ioHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(resourcesHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(criticalHeaderLabel, HPos.RIGHT);
        GridPane.setHalignment(terminatedHeaderLabel, HPos.RIGHT);

        Label processesLabel = new Label("Processes in Processor:");
        processesLabel.setStyle("-fx-font-size: 18");

        processPane = new GridPane();
        processPane.setAlignment(Pos.CENTER);
        processPane.setHgap(10);
        processPane.setVgap(10);

        Label processorStatsHeaderLabel = new Label("CPU Statistics:");
        processorStatsHeaderLabel.setStyle("-fx-font-size: 18");
        processorStatsLabel = new Label();

        VBox status = new VBox();
        status.setSpacing(10);
        status.setAlignment(Pos.TOP_RIGHT);
        status.getChildren().addAll(statsLabel, statsPane);

        VBox processes = new VBox();
        processes.setSpacing(10);
        processes.setAlignment(Pos.TOP_CENTER);
        processes.getChildren().addAll(processesLabel, processPane);

        VBox processorStats = new VBox();
        processorStats.setSpacing(10);
        processorStats.getChildren().addAll(processorStatsHeaderLabel, processorStatsLabel);

        HBox hbox = new HBox();
        hbox.setAlignment(Pos.TOP_CENTER);
        hbox.setSpacing(50);
        hbox.getChildren().addAll(status, processes, processorStats);

        btToggleExecution = new Button("Halt Execution");
        btToggleExecution.setOnAction((event) -> {            if (halted) {
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

        runningPane.getChildren().addAll(topLabel, centralPane, btToggleExecution, hbox);

        return new Scene(runningPane, RUNNING_SCENE_WIDTH, RUNNING_SCENE_HEIGHT);
    }

    public void updateRunningScene(
            List<PCB> runningProcesses,
            long elapsedMs,
            long elapsedCycles,
            int numRunning,
            int numReady,
            int numIo,
            int numResources,
            int numCritical,
            int numTerminated,
            String processorStats
    ) {
        Platform.runLater(() -> {

            int min = (int) (elapsedMs / (1000 * 60));
            int sec = (int) ((elapsedMs % (1000 * 60)) / (1000));
            int ms = (int) (elapsedMs % 1000);
            String timeString = String.format("%d min, %02d sec, %03d ms", min, sec, ms);

            timeLabel.setText(timeString);
            numCyclesLabel.setText(Long.toString(elapsedCycles));
            runningLabel.setText(Integer.toString(numRunning));
            readyLabel.setText(Integer.toString(numReady));
            ioLabel.setText(Integer.toString(numIo));
            resourcesLabel.setText(Integer.toString(numResources));
            criticalLabel.setText(Integer.toString(numCritical));
            terminatedLabel.setText(Integer.toString(numTerminated));
            processorStatsLabel.setText(processorStats);

            processPane.getChildren().clear();
            processPane.add(new Label("PID"), 0, 0);
            processPane.add(new Label("Priority     "), 1, 0);
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

            stage.setScene(new Scene(runningPane, INITIAL_SCENE_WIDTH, INITIAL_SCENE_HEIGHT));
        });
    }

}
