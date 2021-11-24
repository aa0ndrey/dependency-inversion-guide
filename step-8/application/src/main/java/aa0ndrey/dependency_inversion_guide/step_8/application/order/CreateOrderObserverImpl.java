package aa0ndrey.dependency_inversion_guide.step_8.application.order;

import aa0ndrey.dependency_inversion_guide.step_8.application.time_span_manager.TimeSpanManager;
import aa0ndrey.dependency_inversion_guide.step_8.application.transaction_manager.TransactionManager;
import aa0ndrey.dependency_inversion_guide.step_8.core.order.CreateOrderObserver;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManager transactionManager;
    private final TimeSpanManager timeSpanManager;

    @Override
    public void onStart() {
        timeSpanManager.startTimeSpan("Создание заказа");
        transactionManager.begin();
    }

    @Override
    public void onEnd() {
        transactionManager.commit();
    }

    @Override
    public void onFinally() {
        try {
            if (transactionManager.isActive()) {
                transactionManager.rollback();
            }
        } finally {
            if (timeSpanManager.isActive()) {
                timeSpanManager.stopTimeSpan();
            }
        }
    }
}
