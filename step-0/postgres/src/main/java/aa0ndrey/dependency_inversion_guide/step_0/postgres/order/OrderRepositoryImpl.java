package aa0ndrey.dependency_inversion_guide.step_0.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_0.core.order.Order;

public class OrderRepositoryImpl {
    public void create(Order order) {
        //реализация insert into order (id, user_id, product_id) values (?, ?, ?)
    }
}
