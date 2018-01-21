import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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
        Diagram diagram = new Diagram(symbols -> {
            symbolChoiceDialog.getItems().clear();
            symbolChoiceDialog.getItems().addAll(symbols);
            return symbolChoiceDialog.showAndWait().orElse(null);
        });
        ScrollPane diagramContainer = new ScrollPane();
        diagramContainer.setStyle("-fx-background: white;");
        diagramContainer.setContent(diagram);
        diagramContainer.widthProperty().addListener(e -> diagram.updateSize());
        diagramContainer.heightProperty().addListener(e -> diagram.updateSize());

        VBox toolPane = new VBox();
        toolPane.setPadding(new Insets(4));
        toolPane.setSpacing(16);

        Button addState = new Button("Add state");
        TextInputDialog addStateDialog = new TextInputDialog();
        addStateDialog.setTitle("Add state");
        addStateDialog.setHeaderText("Input the name of the state");
        addState.setOnAction(e -> addStateDialog.showAndWait().ifPresent(name -> {
            if (!diagram.addState(name)) {
                error.setHeaderText("A state with this name already exists");
                error.show();
            }
        }));

        Label statePropertiesLbl = new Label("State properties");
        statePropertiesLbl.setDisable(true);
        VBox stateProperties = new VBox(statePropertiesLbl, new Separator());
        stateProperties.setVisible(false);
        stateProperties.setSpacing(8);

        TextField stateName = new TextField();
        Button renameState = new Button("Rename");
        stateName.setOnAction(e -> renameState.fire());
        renameState.setOnAction(e -> {
            if (!diagram.updateSelectedStateName(stateName.getText())) {
                error.setHeaderText("A state with this name already exists");
                error.show();
            }
        });
        HBox stateRenameBox = new HBox(stateName, renameState);
        stateProperties.getChildren().addAll(new Label("Name"), stateRenameBox);

        TextField stateRadius = new TextField();
        Button resizeState = new Button("Resize");
        stateRadius.setOnAction(e -> resizeState.fire());
        resizeState.setOnAction(e -> {
            if (!diagram.setSelectedStateRadius(stateRadius.getText())) {
                error.setHeaderText("Invalid radius");
                error.show();
            }
        });
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
                stateName.setText(s.getName());
                stateRadius.setText(Integer.toString((int) s.getRadius()));
                isAccepting.setSelected(diagram.getSelected().isAccepting());
                setStarting.setVisible(!s.isStarting());
                stateProperties.setVisible(true);
            } else {
                stateProperties.setVisible(false);
            }
        });

        Label transitionListLbl = new Label("Transitions");
        transitionListLbl.setDisable(true);
        VBox transitionList = new VBox(transitionListLbl);
        ListView<Transition> transitionListView = new ListView<>();
        transitionListView.setCellFactory(f -> new TransitionCell());
        diagram.setOnTransitionChange(transitions -> transitionListView.setItems(FXCollections.observableArrayList(transitions)));
        transitionList.setSpacing(8);
        transitionList.getChildren().add(transitionListView);

        toolPane.getChildren().addAll(addState, stateProperties, transitionList);

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
        primaryStage.sizeToScene();
        primaryStage.show();
    }

    static class TransitionCell extends ListCell<Transition> {

        @Override
        protected void updateItem(Transition item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                Text name = new Text(item.getStateFrom().getName());
                setGraphic(name);
            }
        }
    }
}
