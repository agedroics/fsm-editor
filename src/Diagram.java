import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Diagram extends Canvas {

    private static final Font font = Font.font("Arial", 24);

    private final GraphicsContext gc = getGraphicsContext2D();

    private State startingState;
    private Set<String> alphabet = Collections.emptySet();
    private Set<State> states = new HashSet<>();
    private Set<Transition> transitions = new HashSet<>();
    private Consumer<State> onSelectionChange;
    private Consumer<Set<Transition>> onTransitionChange;

    private State selected;
    private double initialOffsetX;
    private double initialOffsetY;

    private State transitionFrom;

    Diagram(Function<Set<String>, String> transitionSymbolSupplier) {
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                setSelected(states.stream()
                        .filter(s -> s.intersects(e.getX(), e.getY()))
                        .findAny().orElse(null));
                if (selected != null) {
                    initialOffsetX = selected.getX() - e.getX();
                    initialOffsetY = selected.getY() - e.getY();
                }
                setTransitionFrom(null);
            }
        });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (selected != null) {
                    if (e.getX() + initialOffsetX < 0) {
                        selected.setX(0);
                    } else {
                        selected.setX(e.getX() + initialOffsetX);
                    }
                    if (e.getY() + initialOffsetY < 0) {
                        selected.setY(0);
                    } else {
                        selected.setY(e.getY() + initialOffsetY);
                    }
                    update();
                }
            }
        });
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                State clickedOn = states.stream()
                        .filter(s -> s.intersects(e.getX(), e.getY()))
                        .findAny().orElse(null);
                if (transitionFrom == null) {
                    setTransitionFrom(clickedOn);
                } else if (clickedOn != null) {
                    Set<String> symbols = new HashSet<>(alphabet);
                    symbols.add("ε");
                    symbols.removeAll(transitionFrom.getTransitions().stream()
                            .filter(t -> t.getStateTo().equals(clickedOn)).findAny()
                            .map(Transition::getSymbols).orElse(Collections.emptySet()));
                    String symbol = transitionSymbolSupplier.apply(symbols);
                    if (symbol != null) {
                        addTransition(transitionFrom, clickedOn, symbol);
                    }
                    setTransitionFrom(null);
                } else {
                    setTransitionFrom(null);
                }
            }
        });
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    private double max(double a, double b) {
        return a > b ? a : b;
    }

    private void update() {
        updateSize();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
        transitions.forEach(t -> t.draw(gc));
        states.forEach(s -> s.draw(gc));
    }

    public void updateSize() {
        double[] bounds = states.stream().reduce(new double[2], (b, s) -> {
            b[0] = max(b[0], s.getX() + 2 * s.getRadius() + 1);
            b[1] = max(b[1], s.getY() + 2 * s.getRadius() + 1);
            return b;
        }, (b1, b2) -> b1);
        setWidth(max(getParent().getLayoutBounds().getWidth(), bounds[0]));
        setHeight(max(getParent().getLayoutBounds().getHeight(), bounds[1]));
    }

    public State getStartingState() {
        return startingState;
    }

    public void setStartingState(State startingState) {
        if (this.startingState != null) {
            this.startingState.setStarting(false);
        }
        this.startingState = startingState;
        startingState.setStarting(true);
        update();
    }

    public boolean addState(String name) {
        if (states.stream().anyMatch(s -> s.getName().equals(name))) {
            return false;
        }
        State state = new State(name);
        states.add(state);
        if (startingState == null) {
            setStartingState(state);
        }
        update();
        return true;
    }

    private void addTransition(State stateFrom, State stateTo, String symbol) {
        stateFrom.getTransitions().stream().filter(t -> t.getStateTo().equals(stateTo)).findAny()
                .ifPresentOrElse(t -> t.getSymbols().add(symbol), () -> {
                    Transition transition = new Transition(stateFrom, stateTo, symbol);
                    stateFrom.getTransitions().add(transition);
                    transitions.add(transition);
                });
        if (onTransitionChange != null) {
            onTransitionChange.accept(transitions);
        }
        update();
    }

    public Set<String> getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(String alphabet) {
        this.alphabet = Set.of(alphabet.split(","));
        for (Transition t : transitions) {
            t.getSymbols().removeIf(s -> !this.alphabet.contains(s) && !s.equals("ε"));
            if (t.getSymbols().isEmpty()) {
                t.getStateFrom().getTransitions().remove(t);
                transitions.remove(t);
            }
        }
        update();
    }

    public State getSelected() {
        return selected;
    }

    private void setSelected(State selected) {
        if (this.selected != null) {
            this.selected.setSelected(false);
        }
        this.selected = selected;
        if (selected != null) {
            selected.setSelected(true);
        }
        if (onSelectionChange != null) {
            onSelectionChange.accept(selected);
        }
        update();
    }

    public void setOnSelectionChange(Consumer<State> onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }

    public boolean updateSelectedStateName(String name) {
        if (states.stream().filter(s -> !s.equals(selected)).anyMatch(s -> s.getName().equals(name))) {
            return false;
        } else {
            selected.setName(name);
            update();
            return true;
        }
    }

    public void toggleSelectedStateAccepting() {
        selected.setAccepting(!selected.isAccepting());
        update();
    }

    public boolean setSelectedStateRadius(String radius) {
        try {
            int newRadius = Integer.parseInt(radius);
            if (newRadius <= 0) {
                return false;
            }
            selected.setRadius(Integer.parseInt(radius));
            update();
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setTransitionFrom(State state) {
        if (this.transitionFrom != null) {
            this.transitionFrom.setDrawingTransition(false);
        }
        this.transitionFrom = state;
        if (state != null) {
            state.setDrawingTransition(true);
        }
        update();
    }

    public Set<Transition> getTransitions() {
        return transitions;
    }

    public void setOnTransitionChange(Consumer<Set<Transition>> onTransitionChange) {
        this.onTransitionChange = onTransitionChange;
    }
}
