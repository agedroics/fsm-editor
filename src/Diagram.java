import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Diagram extends Canvas {

    private static final Font font = Font.font("Arial", 16);

    private final GraphicsContext gc = getGraphicsContext2D();

    private State startingState;
    private Set<String> alphabet = Collections.emptySet();
    private Set<State> states = new HashSet<>();
    private Set<Transition> transitions = new HashSet<>();
    private Consumer<State> onSelectionChange;
    private Consumer<ObservableList<TransitionItem>> onTransitionChange;
    private Supplier<String> stateNameSupplier;
    private Consumer<String> onError;
    private Runnable cancelRunning;

    private State selected;
    private double initialOffsetX;
    private double initialOffsetY;

    private State transitionFrom;

    Diagram(Function<Set<String>, String> transitionSymbolSupplier, Supplier<String> stateNameSupplier, Consumer<String> onError) {
        this.stateNameSupplier = stateNameSupplier;
        this.onError = onError;
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                states.forEach(s -> s.setActive(false));
                if (cancelRunning != null) {
                    cancelRunning.run();
                }
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
                states.forEach(s -> s.setActive(false));
                if (cancelRunning != null) {
                    cancelRunning.run();
                }
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
            } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (selected != null) {
                    toggleSelectedStateAccepting();
                    onSelectionChange.accept(selected);
                } else {
                    addState(e.getX(), e.getY());
                }
            }
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE && selected != null) {
                deleteSelectedState();
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

    public void setStartingState(State startingState) {
        if (this.startingState != null) {
            this.startingState.setStarting(false);
        }
        this.startingState = startingState;
        startingState.setStarting(true);
        update();
    }

    public void addState(double x, double y) {
        String name = stateNameSupplier.get();
        if (name != null) {
            if (name.trim().isEmpty()) {
                onError.accept("State name cannot be empty");
            } else if (states.stream().anyMatch(s -> s.getName().equals(name))) {
                onError.accept("A state with this name already exists");
            } else {
                State state = new State(name, x, y);
                states.add(state);
                if (startingState == null) {
                    setStartingState(state);
                }
                states.forEach(s -> s.setActive(false));
                if (cancelRunning != null) {
                    cancelRunning.run();
                }
                update();
            }
        }
    }

    public void fireTransitionChange() {
        if (onTransitionChange != null) {
            Set<Transition> transitions = selected != null ? selected.getTransitions() : this.transitions;
            onTransitionChange.accept(FXCollections.observableArrayList(transitions.stream()
                    .flatMap(t -> t.getSymbols().stream().map(s -> new TransitionItem(t, s)))
                    .collect(Collectors.toList())));
        }
    }

    private void addTransition(State stateFrom, State stateTo, String symbol) {
        Optional<Transition> existingTransition = stateFrom.getTransitions().stream()
                .filter(t -> t.getStateTo().equals(stateTo)).findAny();
        if (existingTransition.isPresent()) {
            existingTransition.get().getSymbols().add(symbol);
        } else {
            Transition transition = new Transition(stateFrom, stateTo, symbol);
            stateFrom.getTransitions().add(transition);
            transitions.add(transition);
        }
        fireTransitionChange();
        update();
    }

    public Set<String> getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(String alphabet) {
        this.alphabet = Arrays.stream(alphabet.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Set<Transition> removableTransitions = new HashSet<>();
        transitions.forEach(t -> {
            t.getSymbols().removeIf(s -> !this.alphabet.contains(s) && !s.equals("ε"));
            removableTransitions.add(t);
        });
        transitions.removeAll(removableTransitions);
        removableTransitions.forEach(t -> t.getStateFrom().getTransitions().remove(t));
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
        fireTransitionChange();
        update();
    }

    public void setOnSelectionChange(Consumer<State> onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }

    public void renameSelectedState(String name) {
        if (name != null) {
            if (name.trim().isEmpty()) {
                onError.accept("State name cannot be empty");
            } else if (states.stream().anyMatch(s -> s.getName().equals(name))) {
                onError.accept("A state with this name already exists");
            } else {
                selected.setName(name);
                update();
            }
        }
    }

    public void toggleSelectedStateAccepting() {
        selected.setAccepting(!selected.isAccepting());
        update();
    }

    public void resizeSelectedState(String radius) {
        try {
            int newRadius = Integer.parseInt(radius);
            if (newRadius <= 0) {
                onError.accept("Radius cannot be less or equal to 0");
            }
            selected.setRadius(Integer.parseInt(radius));
            update();
        } catch (NumberFormatException e) {
            onError.accept("Invalid number format");
        }
    }

    private void setTransitionFrom(State state) {
        if (this.transitionFrom != null) {
            this.transitionFrom.setDrawingTransition(false);
        }
        this.transitionFrom = state;
        if (state != null) {
            state.setDrawingTransition(true);
            setSelected(null);
        }
        update();
    }

    public void setOnTransitionChange(Consumer<ObservableList<TransitionItem>> onTransitionChange) {
        this.onTransitionChange = onTransitionChange;
    }

    public void deleteSelectedState() {
        Set<Transition> removables = transitions.stream()
                .filter(t -> t.getStateFrom().equals(selected) || t.getStateTo().equals(selected))
                .collect(Collectors.toSet());
        transitions.removeAll(removables);
        removables.forEach(t -> t.getStateFrom().getTransitions().remove(t));
        fireTransitionChange();
        states.remove(selected);
        if (startingState == selected) {
            startingState = null;
        }
        states.forEach(s -> s.setActive(false));
        setSelected(null);
        if (cancelRunning != null) {
            cancelRunning.run();
        }
    }

    public void deleteTransition(TransitionItem t) {
        t.getTransition().getSymbols().remove(t.getSymbol());
        if (t.getTransition().getSymbols().isEmpty()) {
            t.getTransition().getStateFrom().getTransitions().remove(t.getTransition());
            transitions.remove(t.getTransition());
        }
        fireTransitionChange();
        states.forEach(s -> s.setActive(false));
        update();
        if (cancelRunning != null) {
            cancelRunning.run();
        }
    }

    public Set<State> getStates() {
        return states;
    }

    public Set<Transition> getTransitions() {
        return transitions;
    }

    public State getStartingState() {
        return startingState;
    }

    public void newDiagram() {
        cancelRunning.run();
        startingState = null;
        setSelected(null);
        states = new HashSet<>();
        transitions = new HashSet<>();
        onTransitionChange.accept(FXCollections.emptyObservableList());
        update();
    }

    public Set<String> findNonDeterministicStates() {
        return states.stream().filter(s -> {
            List<String> symbols = s.getTransitions().stream()
                    .flatMap(t -> t.getSymbols().stream())
                    .collect(Collectors.toList());
            Set<String> symbolSet = new HashSet<>(symbols);
            return symbolSet.contains("ε") || symbols.size() > symbolSet.size() || !symbolSet.equals(alphabet);
        }).map(State::getName).collect(Collectors.toSet());
    }

    public void setActiveStates(Set<State> activeStates) {
        activeStates.forEach(s -> s.setActive(true));
        states.stream().filter(s -> !activeStates.contains(s)).forEach(s -> s.setActive(false));
        setSelected(null);
    }

    public void setCancelRunning(Runnable cancelRunning) {
        this.cancelRunning = cancelRunning;
    }
}
