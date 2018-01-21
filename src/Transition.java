import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public class Transition implements Drawable {

    private State stateFrom;
    private State stateTo;
    private Set<String> symbols = new HashSet<>();

    Transition(State stateFrom, State stateTo, String symbol) {
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        symbols.add(symbol);
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        double x1 = stateFrom.getX() + stateFrom.getRadius();
        double y1 = stateFrom.getY() + stateFrom.getRadius();
        double x2 = stateTo.getX() + stateTo.getRadius();
        double y2 = stateTo.getY() + stateTo.getRadius();
        double xc = x1 + (x2 - x1) / 2;
        double yc = y1 + (y2 - y1) / 2;
        gc.strokeLine(x1, y1, x2, y2);
        gc.setFill(Color.BLACK);
        gc.fillText(String.join(", ", symbols), xc, yc);
    }

    public State getStateFrom() {
        return stateFrom;
    }

    public State getStateTo() {
        return stateTo;
    }

    public Set<String> getSymbols() {
        return symbols;
    }
}
