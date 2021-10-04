package aa0ndrey.dependency_inversion_guide.step_6.core.order;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final List<CreateOrderObserver> observers;

    public void create(CreateOrderContext context) {
        observers.forEach(observer -> observer.onStart(context));

        var user = context.getUser();
        var product = context.getProduct();

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        context.setCreatedOrder(order);

        observers.forEach(observer -> observer.onEnd(context));
    }
}
