package aa0ndrey.dependency_inversion_guide.step_6.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_6.core.order.CreateOrderContext;
import aa0ndrey.dependency_inversion_guide.step_6.core.order.CreateOrderObserver;
import aa0ndrey.dependency_inversion_guide.step_6.postgres.product.ProductRepositoryImpl;
import aa0ndrey.dependency_inversion_guide.step_6.postgres.transaction_manager.TransactionManagerImpl;
import aa0ndrey.dependency_inversion_guide.step_6.postgres.user.UserRepositoryImpl;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;
    private final ThreadLocal<Long> transactionId = new ThreadLocal<>();
    private final UserRepositoryImpl userRepository;
    private final ProductRepositoryImpl productRepository;
    private final OrderRepositoryImpl orderRepository;

    @Override
    public void onStart(CreateOrderContext context) {
        transactionId.set(transactionManagerImpl.begin());
        var request = context.getRequest();
        context.setUser(userRepository.find(request.getUserId()));
        context.setProduct(productRepository.find(request.getProductId()));
    }

    @Override
    public void onEnd(CreateOrderContext context) {
        transactionManagerImpl.commit(transactionId.get());
        orderRepository.create(context.getCreatedOrder());
    }
}
