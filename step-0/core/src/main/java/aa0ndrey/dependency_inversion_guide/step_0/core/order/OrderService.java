package aa0ndrey.dependency_inversion_guide.step_0.core.order;

import aa0ndrey.dependency_inversion_guide.step_0.postgres.order.OrderRepositoryImpl;
import aa0ndrey.dependency_inversion_guide.step_0.postgres.product.ProductRepositoryImpl;
import aa0ndrey.dependency_inversion_guide.step_0.postgres.user.UserRepositoryImpl;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final UserRepositoryImpl userRepository;
    private final ProductRepositoryImpl productRepository;
    private final OrderRepositoryImpl orderRepository;

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
