import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public class State implements Drawable {

    private String name;
    private boolean isAccepting = false;
    private boolean isStarting = false;
    private boolean isSelected = false;
    private boolean drawingTransition = false;
    private double x = 24;
    private double y = 24;
    private double radius = 24;
    private Set<Transition> transitions = new HashSet<>();

    State(String name) {
        this.name = name;
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (drawingTransition) {
            gc.setStroke(Color.GREEN);
        } else if (isSelected) {
            gc.setStroke(Color.BLUE);
        } else {
            gc.setStroke(Color.BLACK);
        }
        gc.setLineWidth(1);
        gc.strokeOval(x, y, radius * 2, radius * 2);
        if (isAccepting) {
            gc.strokeOval(x + 4, y + 4, radius * 2 - 8, radius * 2 - 8);
        }
        if (drawingTransition) {
            gc.setFill(Color.GREEN);
        } else if (isSelected) {
            gc.setFill(Color.BLUE);
        } else {
            gc.setFill(Color.BLACK);
        }
        gc.fillText(name, x + radius, y + radius);
        if (isStarting) {
            double y = this.y + radius;
            gc.strokeLine(x - 16, y, x, y);
            gc.strokeLine(x - 8, y - 4, x, y);
            gc.strokeLine(x - 8, y + 4, x, y);
        }
    }

    public boolean intersects(double x, double y) {
        return Math.sqrt(Math.pow(x - this.x - radius, 2) + Math.pow(y - this.y - radius, 2)) <= radius;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAccepting() {
        return isAccepting;
    }

    public void setAccepting(boolean accepting) {
        isAccepting = accepting;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Set<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(Set<Transition> transitions) {
        this.transitions = transitions;
    }

    public boolean isStarting() {
        return isStarting;
    }

    public void setStarting(boolean starting) {
        isStarting = starting;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isDrawingTransition() {
        return drawingTransition;
    }

    public void setDrawingTransition(boolean drawingTransition) {
        this.drawingTransition = drawingTransition;
    }
}
