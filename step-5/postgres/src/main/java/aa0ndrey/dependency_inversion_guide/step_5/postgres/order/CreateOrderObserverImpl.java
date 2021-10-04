package aa0ndrey.dependency_inversion_guide.step_5.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_5.core.order.CreateOrderContext;
import aa0ndrey.dependency_inversion_guide.step_5.core.order.CreateOrderObserver;
import aa0ndrey.dependency_inversion_guide.step_5.postgres.transaction_manager.TransactionManagerImpl;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;
    private final ThreadLocal<Long> transactionId = new ThreadLocal<>();

    @Override
    public void onStart(CreateOrderContext context) {
        transactionId.set(transactionManagerImpl.begin());
    }

    @Override
    public void onEnd(CreateOrderContext context) {
        transactionManagerImpl.commit(transactionId.get());
    }
}
