package aa0ndrey.dependency_inversion_guide.step_7.postgres.transaction_manager;

public class TransactionManagerImpl {
    public void begin() {
        //реализация начала транзакции
    }

    public void commit() {
        //реализация фиксации транзакции
    }

    public void rollback() {
        //реализация отката транзакции
    }

    public boolean isActive() {
        //реализация, позволяющая определить, что есть активная транзакция
        throw new UnsupportedOperationException();
    }
}
