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
        gc.setFill(Color.BLACK);
        gc.setLineWidth(1);
        if (stateFrom.equals(stateTo)) {
            double x = stateFrom.getX() + stateFrom.getRadius() + stateFrom.getRadius() * Math.cos(-2 * Math.PI / 3);
            double y = stateFrom.getY() + stateFrom.getRadius() + stateFrom.getRadius() * Math.sin(-2 * Math.PI / 3);
            gc.save();
            gc.translate(x, y);
            gc.beginPath();
            gc.moveTo(0, 0);
            gc.arcTo(stateFrom.getRadius() / 2, -stateFrom.getRadius(), stateFrom.getRadius(), 0, stateFrom.getRadius() / 2);
            gc.lineTo(stateFrom.getRadius(), 0);
            gc.translate(stateFrom.getRadius(), 0);
            gc.rotate(-25);
            gc.lineTo(- 4, -8);
            gc.moveTo(0, 0);
            gc.lineTo(4, -8);
            gc.stroke();
            gc.closePath();
            gc.restore();
            gc.fillText(String.join(", ", symbols), x + stateFrom.getRadius() / 2, y - stateFrom.getRadius() / 2 - 12);
        } else {
            double x1, y1, x2, y2;
            double angle = -Math.atan2(stateTo.getX() + stateTo.getRadius() - stateFrom.getX() - stateFrom.getRadius(),
                    stateTo.getY() + stateTo.getRadius() - stateFrom.getY() - stateFrom.getRadius());
            if (stateTo.getTransitions().stream().anyMatch(t -> t.getStateTo().equals(stateFrom))) {
                x1 = stateFrom.getX() + stateFrom.getRadius() + stateFrom.getRadius() * Math.cos(angle + Math.PI / 3);
                y1 = stateFrom.getY() + stateFrom.getRadius() + stateFrom.getRadius() * Math.sin(angle + Math.PI / 3);
                x2 = stateTo.getX() + stateTo.getRadius() + stateTo.getRadius() * Math.cos(angle - Math.PI / 3);
                y2 = stateTo.getY() + stateTo.getRadius() + stateTo.getRadius() * Math.sin(angle - Math.PI / 3);
            } else {
                x1 = stateFrom.getX() + stateFrom.getRadius() + stateFrom.getRadius() * Math.cos(angle + Math.PI / 2);
                y1 = stateFrom.getY() + stateFrom.getRadius() + stateFrom.getRadius() * Math.sin(angle + Math.PI / 2);
                x2 = stateTo.getX() + stateTo.getRadius() + stateTo.getRadius() * Math.cos(angle - Math.PI / 2);
                y2 = stateTo.getY() + stateTo.getRadius() + stateTo.getRadius() * Math.sin(angle - Math.PI / 2);
            }
            gc.strokeLine(x1, y1, x2, y2);

            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            angle = -Math.atan2(x2 - x1, y2 - y1);
            gc.save();
            gc.translate(x1, y1);
            if (stateTo.getX() - stateFrom.getX() > 0) {
                gc.rotate(Math.toDegrees(angle + Math.PI / 2));
                gc.fillText(String.join(", ", symbols), length / 2, -16);
                gc.strokeLine(length, 0, length - 8, 4);
                gc.strokeLine(length, 0, length - 8, -4);
            } else {
                gc.rotate(Math.toDegrees(angle - Math.PI / 2));
                gc.fillText(String.join(", ", symbols), -length / 2, 16);
                gc.strokeLine(-length, 0, -length + 8, 4);
                gc.strokeLine(-length, 0, -length + 8, -4);
            }
            gc.restore();
        }
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
