package aa0ndrey.dependency_inversion_guide.step_4.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_4.core.order.Order;
import aa0ndrey.dependency_inversion_guide.step_4.core.order.OrderRepository;

public class OrderRepositoryImpl implements OrderRepository {
    @Override
    public void create(Order order) {
        //реализация insert into order (id, user_id, product_id) values (?, ?, ?)
    }
}
