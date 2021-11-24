package aa0ndrey.dependency_inversion_guide.step_7.open_telemetry.time_span_manager;

public class TimeSpanManagerImpl {
    public void startTimeSpan(String name) {
        //реализация старта временного отрезка
    }

    public boolean isActive() {
        //реализация, определяющая, что есть промежуток времени, для которого ведется отсчет времени
        throw new UnsupportedOperationException();
    }

    public void stopTimeSpan() {
        //реализация завершения временного отрезка
    }
}
