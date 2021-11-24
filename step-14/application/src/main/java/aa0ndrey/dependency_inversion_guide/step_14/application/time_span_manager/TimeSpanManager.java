package aa0ndrey.dependency_inversion_guide.step_14.application.time_span_manager;

public interface TimeSpanManager {
    void startTimeSpan(String name);

    boolean isActive();

    void addEvent(String name);

    void stopTimeSpan();
}
