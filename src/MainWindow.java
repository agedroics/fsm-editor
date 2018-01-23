import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class MainWindow extends Application {

    private File file;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        setUserAgentStylesheet(STYLESHEET_MODENA);
        primaryStage.setTitle("New diagram");

        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Error");
        error.setHeaderText(null);

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setHeaderText(null);

        ChoiceDialog<String> symbolChoiceDialog = new ChoiceDialog<>();
        symbolChoiceDialog.setTitle("Create transition");
        symbolChoiceDialog.setHeaderText(null);
        symbolChoiceDialog.setContentText("Symbol:");

        TextInputDialog stateNameDialog = new TextInputDialog();
        stateNameDialog.setTitle("Add state");
        stateNameDialog.setHeaderText(null);
        stateNameDialog.setContentText("Name:");

        Diagram diagram = new Diagram(symbols -> {
            symbolChoiceDialog.getItems().clear();
            symbolChoiceDialog.getItems().addAll(symbols);
            return symbolChoiceDialog.showAndWait().orElse(null);
        }, () -> stateNameDialog.showAndWait().orElse(null),
        e -> {
            error.setContentText(e);
            error.show();
        });
        ScrollPane diagramContainer = new ScrollPane();
        diagramContainer.setStyle("-fx-background: white;");
        diagramContainer.setContent(diagram);
        diagramContainer.widthProperty().addListener(e -> diagram.updateSize());
        diagramContainer.heightProperty().addListener(e -> diagram.updateSize());
        diagramContainer.focusedProperty().addListener(e -> diagram.requestFocus());

        VBox toolPane = new VBox();
        toolPane.setPadding(new Insets(4));
        toolPane.setSpacing(16);

        HBox actions = new HBox();
        Button addState = new Button("Add state");
        addState.setOnAction(e -> diagram.addState(24, 24));
        Button deleteState = new Button("Delete state");
        deleteState.setOnAction(e -> diagram.deleteSelectedState());
        deleteState.setVisible(false);
        Button setStarting = new Button("Set starting");
        setStarting.setOnAction(e -> {
            diagram.setStartingState(diagram.getSelected());
            setStarting.setVisible(false);
        });
        setStarting.setVisible(false);
        actions.setSpacing(4);
        actions.getChildren().addAll(addState, deleteState, setStarting);
        toolPane.getChildren().add(actions);

        StateMachineRunner runner = new StateMachineRunner(diagram, diagram::setActiveStates);
        VBox runControls = new VBox();
        HBox wordContainer = new HBox();
        TextField word = new TextField();
        word.setPrefWidth(162);
        Button run = new Button("Run");
        run.setPrefWidth(70);
        word.setOnAction(e -> run.fire());
        wordContainer.getChildren().addAll(word, run);
        runControls.setSpacing(4);
        Label runnerLbl = new Label("Runner");
        runnerLbl.setDisable(true);

        GridPane moveControls = new GridPane();
        moveControls.setHgap(4);
        Button stepBack = new Button("<");
        stepBack.setDisable(true);
        Button reload = new Button("Start simulation");
        Button stepForward = new Button(">");
        Label nextSymbol = new Label();
        stepBack.setOnAction(e -> {
            runner.stepBack();
            stepForward.setDisable(false);
            nextSymbol.setText("Remaining input: " + runner.getWord().substring(runner.getPos()));
            if (runner.getPos() == 0) {
                stepBack.setDisable(true);
            }
        });
        reload.setOnAction(e -> {
            if (diagram.getStartingState() == null) {
                error.setContentText("No starting state specified");
                error.show();
            } else {
                reload.setText("Reload");
                runner.setUp(word.getText());
                stepBack.setDisable(true);
                if (!runner.getWord().isEmpty()) {
                    nextSymbol.setText("Remaining input: " + runner.getWord().substring(runner.getPos()));
                    stepForward.setDisable(false);
                } else {
                    nextSymbol.setText("End of input reached");
                }
            }
        });
        stepForward.setDisable(true);
        stepForward.setOnAction(e -> {
            stepBack.setDisable(false);
            runner.stepForward();
            if (runner.getPos() == runner.getWord().length()) {
                nextSymbol.setText("End of input reached");
                stepForward.setDisable(true);
            } else {
                nextSymbol.setText("Remaining input: " + runner.getWord().substring(runner.getPos()));
            }
        });
        Runnable cancelSteps = () -> {
            stepBack.setDisable(true);
            reload.setText("Start simulation");
            stepForward.setDisable(true);
            nextSymbol.setText("");
        };
        diagram.setCancelRunning(cancelSteps);
        run.setOnAction(e -> {
            cancelSteps.run();
            info.setTitle("Run result");
            if (diagram.getStartingState() == null) {
                error.setContentText("No starting state specified");
                error.show();
            } else if (runner.run(word.getText())) {
                info.setContentText("The state machine accepts the word");
                info.show();
            } else {
                info.setContentText("The state machine does not accept the word");
                info.show();
            }
        });
        moveControls.add(stepBack, 0, 0);
        moveControls.add(reload, 1, 0);
        GridPane.setHgrow(reload, Priority.ALWAYS);
        reload.setMaxWidth(300);
        moveControls.add(stepForward, 2, 0);

        moveControls.setPrefWidth(232);

        runControls.getChildren().addAll(runnerLbl, new Separator(), new Label("Word"), wordContainer, nextSymbol, moveControls);
        toolPane.getChildren().add(runControls);

        Label statePropertiesLbl = new Label("State properties");
        statePropertiesLbl.setDisable(true);
        VBox stateProperties = new VBox(statePropertiesLbl, new Separator());
        stateProperties.setVisible(false);
        stateProperties.setSpacing(4);

        TextField stateName = new TextField();
        stateName.setPrefWidth(162);
        Button renameState = new Button("Rename");
        renameState.setPrefWidth(70);
        stateName.setOnAction(e -> renameState.fire());
        renameState.setOnAction(e -> diagram.renameSelectedState(stateName.getText()));
        HBox stateRenameBox = new HBox(stateName, renameState);
        stateProperties.getChildren().addAll(new Label("Name"), stateRenameBox);

        TextField stateRadius = new TextField();
        stateRadius.setPrefWidth(162);
        Button resizeState = new Button("Resize");
        resizeState.setPrefWidth(70);
        stateRadius.setOnAction(e -> resizeState.fire());
        resizeState.setOnAction(e -> diagram.resizeSelectedState(stateRadius.getText()));
        HBox stateResizeBox = new HBox(stateRadius, resizeState);
        stateProperties.getChildren().addAll(new Label("Radius"), stateResizeBox);

        CheckBox isAccepting = new CheckBox("Accepting");
        isAccepting.setOnAction(e -> diagram.toggleSelectedStateAccepting());
        stateProperties.getChildren().add(isAccepting);
        toolPane.getChildren().add(stateProperties);

        diagram.setOnSelectionChange(s -> {
            if (s != null) {
                deleteState.setVisible(true);
                stateName.setText(s.getName());
                stateRadius.setText(Integer.toString((int) s.getRadius()));
                isAccepting.setSelected(diagram.getSelected().isAccepting());
                setStarting.setVisible(!s.isStarting());
                stateProperties.setVisible(true);
            } else {
                deleteState.setVisible(false);
                setStarting.setVisible(false);
                stateProperties.setVisible(false);
            }
        });

        Label transitionListLbl = new Label("Transitions");
        transitionListLbl.setDisable(true);
        VBox transitionList = new VBox(transitionListLbl);
        ListView<TransitionItem> transitionListView = new ListView<>();
        transitionListView.setPrefWidth(232);
        transitionListView.setCellFactory(f -> new TransitionItem.TransitionCell(diagram::deleteTransition));
        transitionListView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE && transitionListView.getSelectionModel().getSelectedItem() != null) {
                diagram.deleteTransition(transitionListView.getSelectionModel().getSelectedItem());
            }
        });
        diagram.setOnTransitionChange(transitionListView::setItems);
        transitionList.setSpacing(4);
        transitionList.getChildren().addAll(transitionListView);
        toolPane.getChildren().add(transitionList);

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu toolsMenu = new Menu("Tools");

        MenuItem newDiagram = new MenuItem("New");
        newDiagram.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newDiagram.setOnAction(e -> {
            diagram.newDiagram();
            file = null;
            primaryStage.setTitle("New diagram");
        });

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Finite state machine", "*.fsm");
        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setSelectedExtensionFilter(extensionFilter);

        MenuItem saveAs = new MenuItem("Save As...");
        saveAs.setOnAction(e -> {
            fileChooser.setTitle("Save");
            file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                try {
                    FileHandler.save(file, diagram);
                    primaryStage.setTitle(file.getName());
                } catch (IOException ex) {
                    error.setContentText(ex.getMessage());
                    error.show();
                }
            }
        });

        MenuItem save = new MenuItem("Save");
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        save.setOnAction(e -> {
            if (file != null) {
                try {
                    FileHandler.save(file, diagram);
                } catch (IOException ex) {
                    error.setContentText(ex.getMessage());
                    error.show();
                }
            } else {
                saveAs.fire();
            }
        });

        MenuItem open = new MenuItem("Open");
        open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        open.setOnAction(e -> {
            file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    FileHandler.open(file, diagram);
                    primaryStage.setTitle(file.getName());
                } catch (Exception ex) {
                    error.setContentText(ex.getMessage());
                    error.show();
                    newDiagram.fire();
                }
            }
        });

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> primaryStage.close());

        fileMenu.getItems().addAll(newDiagram, open, save, saveAs, new SeparatorMenuItem(), exit);

        MenuItem setAlphabet = new MenuItem("Set alphabet");
        TextInputDialog setAlphabetDialog = new TextInputDialog();
        setAlphabetDialog.setTitle("Set alphabet");
        setAlphabetDialog.setHeaderText("Input the symbols of the alphabet (comma separated)");
        setAlphabet.setOnAction(e -> {
            setAlphabetDialog.getEditor().setText(String.join(",", diagram.getAlphabet()));
            setAlphabetDialog.showAndWait().ifPresent(diagram::setAlphabet);
        });

        MenuItem testDeterminism = new MenuItem("Test determinism");
        testDeterminism.setOnAction(e -> {
            Set<String> nonDeterministicStates = diagram.findNonDeterministicStates();
            info.setTitle("Test determinism");
            if (!nonDeterministicStates.isEmpty()) {
                info.setContentText("The state machine is non-deterministic at these states: " +
                        String.join(", ", nonDeterministicStates));
                info.show();
            } else {
                info.setContentText("The state machine is deterministic");
                info.show();
            }
        });

        toolsMenu.getItems().addAll(setAlphabet, testDeterminism);

        menuBar.getMenus().addAll(fileMenu, toolsMenu);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(menuBar);
        mainLayout.setRight(toolPane);
        mainLayout.setCenter(diagramContainer);
        primaryStage.setScene(new Scene(mainLayout, 800 ,600));
        primaryStage.show();
        diagram.updateSize();
    }
}
