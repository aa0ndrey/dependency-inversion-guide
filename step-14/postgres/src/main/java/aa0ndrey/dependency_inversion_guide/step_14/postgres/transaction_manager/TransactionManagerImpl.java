package aa0ndrey.dependency_inversion_guide.step_14.postgres.transaction_manager;

import aa0ndrey.dependency_inversion_guide.step_14.application.transaction_manager.TransactionManager;

public class TransactionManagerImpl implements TransactionManager {
    @Override
    public void begin() {
        //реализация начала транзакции
    }

    @Override
    public void commit() {
        //реализация фиксации транзакции
    }

    @Override
    public void rollback() {
        //реализация отката транзакции
    }

    @Override
    public boolean isActive() {
        //реализация, позволяющая определить, что есть активная транзакция
        throw new UnsupportedOperationException();
    }
}
