package samrock.api;

@FunctionalInterface
public interface Changer {
    public void changeTo(Change action);
}
