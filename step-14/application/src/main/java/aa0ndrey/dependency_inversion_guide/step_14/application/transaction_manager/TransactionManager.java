package aa0ndrey.dependency_inversion_guide.step_14.application.transaction_manager;

public interface TransactionManager {
    void begin();

    void commit();

    void rollback();

    boolean isActive();
}
