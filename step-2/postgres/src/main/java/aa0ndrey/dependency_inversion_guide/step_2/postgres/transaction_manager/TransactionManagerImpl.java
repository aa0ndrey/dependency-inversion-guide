package aa0ndrey.dependency_inversion_guide.step_2.postgres.transaction_manager;

import aa0ndrey.dependency_inversion_guide.step_2.core.transaction_manager.TransactionManager;

public class TransactionManagerImpl implements TransactionManager {
    public void begin() {
        //реализация начала транзакции
    }

    public void commit() {
        //реализация фиксации транзакции
    }
}
