import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StateMachineRunner {

    private Diagram diagram;
    private String word;
    private int pos;
    private Stack<Set<State>> steps = new Stack<>();
    private Consumer<Set<State>> onStep;

    StateMachineRunner(Diagram diagram, Consumer<Set<State>> onStep) {
        this.diagram = diagram;
        this.onStep = onStep;
    }

    private void transitiveClosure(State state, Set<State> states) {
        if (state != null) {
            states.add(state);
            state.getTransitions().stream()
                    .filter(t -> t.getSymbols().contains("ε"))
                    .map(Transition::getStateTo)
                    .filter(s -> !states.contains(s))
                    .forEach(s -> transitiveClosure(s, states));
        }
    }

    public void setUp(String word) {
        steps = new Stack<>();
        this.word = word;
        pos = 0;
        Set<State> step = new HashSet<>();
        transitiveClosure(diagram.getStartingState(), step);
        steps.push(step);
        onStep.accept(step);
    }

    public void stepForward() {
        if (pos < word.length()) {
            Set<State> nextStates = steps.peek().stream()
                    .flatMap(s -> s.getTransitions().stream())
                    .filter(t -> t.getSymbols().contains(word.substring(pos, pos + 1)))
                    .map(Transition::getStateTo)
                    .collect(Collectors.toSet());
            Set<State> step = new HashSet<>();
            nextStates.forEach(s -> transitiveClosure(s, step));
            steps.push(step);
            onStep.accept(step);
            ++pos;
        }
    }

    public void stepBack() {
        if (pos > 0) {
            steps.pop();
            onStep.accept(steps.peek());
            --pos;
        }
    }

    public boolean run(String word) {
        setUp(word);
        while (pos < this.word.length() && !steps.peek().isEmpty()) {
            stepForward();
        }
        Set<State> step = steps.pop();
        steps = new Stack<>();
        return step.stream().anyMatch(State::isAccepting);
    }

    public int getPos() {
        return pos;
    }

    public String getWord() {
        return word;
    }
}
