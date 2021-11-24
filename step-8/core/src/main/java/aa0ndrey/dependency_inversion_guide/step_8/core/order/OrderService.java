package aa0ndrey.dependency_inversion_guide.step_8.core.order;

import aa0ndrey.dependency_inversion_guide.step_8.core.product.ProductRepository;
import aa0ndrey.dependency_inversion_guide.step_8.core.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CreateOrderObserver observer;

    public void create(CreateOrderRequest request) {
        try {
            observer.onStart();

            var user = userRepository.find(request.getUserId());
            var product = productRepository.find(request.getProductId());

            if (user.getBalance() < product.getPrice()) {
                throw new RuntimeException("Недостаточно средств");
            }

            var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
            orderRepository.create(order);

            observer.onEnd();
        } finally {
            observer.onFinally();
        }
    }
}
