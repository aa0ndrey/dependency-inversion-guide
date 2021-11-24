package aa0ndrey.dependency_inversion_guide.step_8.application.time_span_manager;

public interface TimeSpanManager {
    void startTimeSpan(String name);

    boolean isActive();

    void stopTimeSpan();
}
