package aa0ndrey.dependency_inversion_guide.step_14.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_14.core.order.Order;
import aa0ndrey.dependency_inversion_guide.step_14.application.order.CreateOrderRepositoryObserverImpl;
import aa0ndrey.dependency_inversion_guide.step_14.core.order.OrderRepository;
import lombok.RequiredArgsConstructor;

import static java.lang.String.format;

@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final CreateOrderRepositoryObserverImpl observer;

    @Override
    public void create(Order order) {
        String sql = format(
                "insert into order (id, user_id, product_id) values (%s, %s, %s)",
                order.getId(),
                order.getUserId(),
                order.getProductId()
        );

        observer.afterSqlCreated(sql);

        executeSql(sql);
    }

    private void executeSql(String sql) {
        //отправка sql запроса
    }
}
