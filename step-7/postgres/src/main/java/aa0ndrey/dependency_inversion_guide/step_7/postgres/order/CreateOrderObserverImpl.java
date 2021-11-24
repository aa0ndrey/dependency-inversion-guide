package aa0ndrey.dependency_inversion_guide.step_7.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_7.core.order.CreateOrderObserver;
import aa0ndrey.dependency_inversion_guide.step_7.postgres.transaction_manager.TransactionManagerImpl;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;

    @Override
    public void onStart() {
        transactionManagerImpl.begin();
    }

    @Override
    public void onEnd() {
        transactionManagerImpl.commit();
    }

    @Override
    public void onFinally() {
        if (transactionManagerImpl.isActive()) {
            transactionManagerImpl.rollback();
        }
    }
}
