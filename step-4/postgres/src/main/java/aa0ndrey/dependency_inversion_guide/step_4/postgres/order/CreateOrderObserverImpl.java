package aa0ndrey.dependency_inversion_guide.step_4.postgres.order;

import aa0ndrey.dependency_inversion_guide.step_4.core.order.CreateOrderContext;
import aa0ndrey.dependency_inversion_guide.step_4.core.order.CreateOrderObserver;
import aa0ndrey.dependency_inversion_guide.step_4.postgres.transaction_manager.TransactionManagerImpl;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;

    @Override
    public void onStart(CreateOrderContext context) {
        var transactionId = transactionManagerImpl.begin();
        context.getData().put("transaction-id", transactionId);
    }

    @Override
    public void onEnd(CreateOrderContext context) {
        var transactionId = (Long) context.getData().get("transaction-id");
        transactionManagerImpl.commit(transactionId);
    }
}
