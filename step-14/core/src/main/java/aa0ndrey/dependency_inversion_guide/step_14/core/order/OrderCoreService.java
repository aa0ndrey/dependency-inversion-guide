package aa0ndrey.dependency_inversion_guide.step_14.core.order;

import aa0ndrey.dependency_inversion_guide.step_14.core.user.UserRepository;
import aa0ndrey.dependency_inversion_guide.step_14.core.product.ProductRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class OrderCoreService implements OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
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
