package aa0ndrey.dependency_inversion_guide.step_14.application.order;

import aa0ndrey.dependency_inversion_guide.step_14.application.time_span_manager.TimeSpanManager;
import aa0ndrey.dependency_inversion_guide.step_14.application.transaction_manager.TransactionManager;
import aa0ndrey.dependency_inversion_guide.step_14.core.order.CreateOrderRequest;
import aa0ndrey.dependency_inversion_guide.step_14.core.order.OrderCoreService;
import aa0ndrey.dependency_inversion_guide.step_14.core.order.OrderService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderAppService implements OrderService {
    private final OrderCoreService coreService;
    private final TransactionManager transactionManager;
    private final TimeSpanManager timeSpanManager;

    @Override
    public void create(CreateOrderRequest request) {
        try {
            timeSpanManager.startTimeSpan("Создание заказа");
            transactionManager.begin();

            coreService.create(request);

            transactionManager.commit();
        } finally {
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
}
