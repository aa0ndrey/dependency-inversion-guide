package aa0ndrey.dependency_inversion_guide.step_8.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_8.core.order.Order;
import aa0ndrey.dependency_inversion_guide.step_8.core.order.OrderRepository;

public class OrderRepositoryImpl implements OrderRepository {
    @Override
    public void create(Order order) {
        //реализация insert into order (id, user_id, product_id) values (?, ?, ?)
    }
}
