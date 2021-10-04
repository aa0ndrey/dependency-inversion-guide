package aa0ndrey.dependency_inversion_guide.step_3.core.order;

import aa0ndrey.dependency_inversion_guide.step_3.core.product.ProductRepository;
import aa0ndrey.dependency_inversion_guide.step_3.core.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final List<CreateOrderObserver> observers;

    public void create(CreateOrderRequest request) {
        var startEvent = new CreateOrderEvents.Start(request);
        observers.forEach(observer -> observer.onStart(startEvent));

        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);

        var endEvent = new CreateOrderEvents.End(
                request,
                user,
                product,
                order
        );
        observers.forEach(observer -> observer.onEnd(endEvent));
    }
}
