package aa0ndrey.dependency_inversion_guide.step_5.postgres.transaction_manager;

public class TransactionManagerImpl {
    public long begin() {
        //реализация начала транзакции
        throw new UnsupportedOperationException();
    }

    public void commit(long transactionId) {
        //реализация фиксации транзакции
    }
}
