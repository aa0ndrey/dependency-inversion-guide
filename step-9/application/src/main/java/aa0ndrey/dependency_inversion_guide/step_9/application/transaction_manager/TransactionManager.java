package aa0ndrey.dependency_inversion_guide.step_9.application.transaction_manager;

public interface TransactionManager {
    void begin();

    void commit();

    void rollback();

    boolean isActive();
}
