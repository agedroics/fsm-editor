import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        actions.setSpacing(4);
        actions.getChildren().addAll(addState, deleteState);

        Label statePropertiesLbl = new Label("State properties");
        statePropertiesLbl.setDisable(true);
        VBox stateProperties = new VBox(statePropertiesLbl, new Separator());
        stateProperties.setVisible(false);
        stateProperties.setSpacing(4);

        TextField stateName = new TextField();
        Button renameState = new Button("Rename");
        stateName.setOnAction(e -> renameState.fire());
        renameState.setOnAction(e -> diagram.renameSelectedState(stateName.getText()));
        HBox stateRenameBox = new HBox(stateName, renameState);
        stateProperties.getChildren().addAll(new Label("Name"), stateRenameBox);

        TextField stateRadius = new TextField();
        Button resizeState = new Button("Resize");
        stateRadius.setOnAction(e -> resizeState.fire());
        resizeState.setOnAction(e -> diagram.resizeSelectedState(stateRadius.getText()));
        HBox stateResizeBox = new HBox(stateRadius, resizeState);
        stateProperties.getChildren().addAll(new Label("Radius"), stateResizeBox);

        CheckBox isAccepting = new CheckBox("Accepting");
        isAccepting.setOnAction(e -> diagram.toggleSelectedStateAccepting());
        stateProperties.getChildren().add(isAccepting);

        Button setStarting = new Button("Set as starting state");
        setStarting.setOnAction(e -> {
            diagram.setStartingState(diagram.getSelected());
            setStarting.setVisible(false);
        });
        stateProperties.getChildren().add(setStarting);

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
                stateProperties.setVisible(false);
            }
        });

        Label transitionListLbl = new Label("Transitions");
        transitionListLbl.setDisable(true);
        VBox transitionList = new VBox(transitionListLbl);
        ListView<TransitionItem> transitionListView = new ListView<>();
        transitionListView.setCellFactory(f -> new TransitionItem.TransitionCell(diagram::deleteTransition));
        transitionListView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE && transitionListView.getSelectionModel().getSelectedItem() != null) {
                diagram.deleteTransition(transitionListView.getSelectionModel().getSelectedItem());
            }
        });
        diagram.setOnTransitionChange(transitionListView::setItems);
        transitionList.setSpacing(4);
        transitionList.getChildren().addAll(transitionListView);

        toolPane.getChildren().addAll(actions, stateProperties, transitionList);

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
        TextInputDialog setAlphabetDialog = new TextInputDialog(String.join(",", diagram.getAlphabet()));
        setAlphabetDialog.setTitle("Set alphabet");
        setAlphabetDialog.setHeaderText("Input the symbols of the alphabet (comma separated)");
        setAlphabet.setOnAction(e -> setAlphabetDialog.showAndWait().ifPresent(diagram::setAlphabet));

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setHeaderText(null);

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
