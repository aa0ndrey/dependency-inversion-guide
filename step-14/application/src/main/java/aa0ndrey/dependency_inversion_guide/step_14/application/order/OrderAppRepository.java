package aa0ndrey.dependency_inversion_guide.step_14.application.order;

import aa0ndrey.dependency_inversion_guide.step_14.application.time_span_manager.TimeSpanManager;
import aa0ndrey.dependency_inversion_guide.step_14.core.order.Order;
import aa0ndrey.dependency_inversion_guide.step_14.core.order.OrderRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderAppRepository implements OrderRepository {
    private final OrderRepository orderRepository;
    private final TimeSpanManager timeSpanManager;

    public void create(Order order) {
        try {
            timeSpanManager.startTimeSpan("Вставка заказа в таблицу");
            orderRepository.create(order);
        } finally {
            if (timeSpanManager.isActive()) {
                timeSpanManager.stopTimeSpan();
            }
        }
    }
}
