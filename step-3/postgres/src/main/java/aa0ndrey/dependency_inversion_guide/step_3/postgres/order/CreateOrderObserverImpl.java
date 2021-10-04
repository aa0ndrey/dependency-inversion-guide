package aa0ndrey.dependency_inversion_guide.step_3.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_3.core.order.CreateOrderEvents;
import aa0ndrey.dependency_inversion_guide.step_3.core.order.CreateOrderObserver;
import aa0ndrey.dependency_inversion_guide.step_3.postgres.transaction_manager.TransactionManagerImpl;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;

    @Override
    public void onStart(CreateOrderEvents.Start event) {
        transactionManagerImpl.begin();
    }

    @Override
    public void onEnd(CreateOrderEvents.End event) {
        transactionManagerImpl.commit();
    }
}
