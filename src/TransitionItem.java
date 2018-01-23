import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

public class TransitionItem {

    private Transition transition;
    private String symbol;

    TransitionItem(Transition transition, String symbol) {
        this.transition = transition;
        this.symbol = symbol;
    }

    public Transition getTransition() {
        return transition;
    }

    public String getSymbol() {
        return symbol;
    }

    public static class TransitionCell extends ListCell<TransitionItem> {

        private Consumer<TransitionItem> onTransitionDelete;

        TransitionCell(Consumer<TransitionItem> onTransitionDelete) {
            this.onTransitionDelete = onTransitionDelete;
            setTextAlignment(TextAlignment.CENTER);
        }

        @Override
        protected void updateItem(TransitionItem item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                BorderPane cellPane = new BorderPane();
                Text name = new Text("δ(" + item.transition.getStateFrom().getName() + ", " + item.symbol +
                        ") = " + item.transition.getStateTo().getName());
                cellPane.setLeft(name);
                BorderPane.setAlignment(name, Pos.CENTER_LEFT);
                Button delete = new Button("Delete");
                delete.setOnAction(e -> onTransitionDelete.accept(item));
                cellPane.setRight(delete);
                setGraphic(cellPane);
            } else {
                setGraphic(null);
            }
        }
    }
}
