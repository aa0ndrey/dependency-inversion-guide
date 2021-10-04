package aa0ndrey.dependency_inversion_guide.step_5.core.order;

import aa0ndrey.dependency_inversion_guide.step_5.core.product.ProductRepository;
import aa0ndrey.dependency_inversion_guide.step_5.core.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final List<CreateOrderObserver> observers;

    public void create(CreateOrderContext context) {
        observers.forEach(observer -> observer.onStart(context));

        var request = context.getRequest();
        var user = userRepository.find(request.getUserId());
        context.setUser(user);
        var product = productRepository.find(request.getProductId());
        context.setProduct(product);

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);
        context.setCreatedOrder(order);

        observers.forEach(observer -> observer.onEnd(context));
    }
}
