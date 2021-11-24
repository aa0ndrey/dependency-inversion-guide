package aa0ndrey.dependency_inversion_guide.step_7.open_telemetry.order;

import aa0ndrey.dependency_inversion_guide.step_7.core.order.CreateOrderObserver;
import aa0ndrey.dependency_inversion_guide.step_7.open_telemetry.time_span_manager.TimeSpanManagerImpl;
import lombok.RequiredArgsConstructor;

public class CreateOrderObserverImpl {
    @RequiredArgsConstructor
    public static class OnStart implements CreateOrderObserver {
        private final TimeSpanManagerImpl timeSpanManager;

        @Override
        public void onStart() {
            timeSpanManager.startTimeSpan("Создание заказа");
        }
    }

    @RequiredArgsConstructor
    public static class OnFinally implements CreateOrderObserver {
        private final TimeSpanManagerImpl timeSpanManager;

        @Override
        public void onFinally() {
            if (timeSpanManager.isActive()) {
                timeSpanManager.stopTimeSpan();
            }
        }
    }
}
