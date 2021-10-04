package aa0ndrey.dependency_inversion_guide.step_2.core.order;

import aa0ndrey.dependency_inversion_guide.step_2.core.product.ProductRepository;
import aa0ndrey.dependency_inversion_guide.step_2.core.transaction_manager.TransactionManager;
import aa0ndrey.dependency_inversion_guide.step_2.core.user.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TransactionManager transactionManager;

    public void create(CreateOrderRequest request) {
        transactionManager.begin();

        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);

        transactionManager.commit();
    }
}
