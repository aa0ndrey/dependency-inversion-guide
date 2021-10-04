package aa0ndrey.dependency_inversion_guide.step_1.core.order;

import aa0ndrey.dependency_inversion_guide.step_1.core.product.ProductRepository;
import aa0ndrey.dependency_inversion_guide.step_1.core.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public void create(CreateOrderRequest request) {
        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);
    }
}
