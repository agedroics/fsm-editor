import java.io.*;
import java.util.Set;

public class FileHandler {

    public static void save(File file, Diagram diagram) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(diagram.getAlphabet().stream().collect(StringBuilder::new, (b, s) -> b.append(s).append(","),
                StringBuilder::append).toString());
        oos.writeObject(diagram.getStates());
        oos.writeObject(diagram.getTransitions());
        oos.writeObject(diagram.getStartingState().getName());
        oos.close();
        fos.close();
    }

    @SuppressWarnings("unchecked")
    public static void open(File file, Diagram diagram) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        diagram.newDiagram();
        diagram.setAlphabet((String) ois.readObject());
        ((Set<State>) ois.readObject()).forEach(s -> {
            State state = new State(s.getName(), s.getX(), s.getY());
            state.setRadius(s.getRadius());
            state.setAccepting(s.isAccepting());
            diagram.getStates().add(state);
        });
        ((Set<Transition>) ois.readObject()).forEach(t -> {
            State stateFrom = diagram.getStates().stream().filter(s -> s.equals(t.getStateFrom())).findAny().get();
            State stateTo = diagram.getStates().stream().filter(s -> s.equals(t.getStateTo())).findAny().get();
            Transition transition = new Transition(stateFrom, stateTo, t.getSymbols());
            stateFrom.getTransitions().add(transition);
            diagram.getTransitions().add(transition);
        });
        String startingState = ((String) ois.readObject());
        diagram.setStartingState(diagram.getStates().stream().filter(s -> s.getName().equals(startingState)).findAny().orElse(null));
        diagram.fireTransitionChange();
    }
}
