package aa0ndrey.dependency_inversion_guide.step_9.open_telemetry.time_span_manager;

import aa0ndrey.dependency_inversion_guide.step_9.application.time_span_manager.TimeSpanManager;

public class TimeSpanManagerImpl implements TimeSpanManager {
    @Override
    public void startTimeSpan(String name) {
        //реализация старта временного отрезка
    }

    @Override
    public boolean isActive() {
        //реализация, определяющая, что есть промежуток времени, для которого ведется отсчет времени
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopTimeSpan() {
        //реализация завершения временного отрезка
    }
}
