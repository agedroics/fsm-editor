import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindow extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("New diagram");

        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Error");

        ChoiceDialog<String> symbolChoiceDialog = new ChoiceDialog<>();
        symbolChoiceDialog.setTitle("Create transition");
        symbolChoiceDialog.setHeaderText("Choose a symbol");

        TextInputDialog stateNameDialog = new TextInputDialog();
        stateNameDialog.setTitle("Add state");
        stateNameDialog.setHeaderText("Input the name of the state");

        Diagram diagram = new Diagram(symbols -> {
            symbolChoiceDialog.getItems().clear();
            symbolChoiceDialog.getItems().addAll(symbols);
            return symbolChoiceDialog.showAndWait().orElse(null);
        }, () -> stateNameDialog.showAndWait().orElse(null),
        e -> {
            error.setHeaderText(e);
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

        MenuItem setAlphabet = new MenuItem("Set alphabet");
        TextInputDialog setAlphabetDialog = new TextInputDialog(String.join(",", diagram.getAlphabet()));
        setAlphabetDialog.setTitle("Set alphabet");
        setAlphabetDialog.setHeaderText("Input the symbols of the alphabet (comma separated)");
        setAlphabet.setOnAction(e -> setAlphabetDialog.showAndWait().ifPresent(diagram::setAlphabet));

        toolsMenu.getItems().add(setAlphabet);

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
