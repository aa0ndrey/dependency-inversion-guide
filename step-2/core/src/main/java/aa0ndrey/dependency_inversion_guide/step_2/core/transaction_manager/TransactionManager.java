package aa0ndrey.dependency_inversion_guide.step_2.core.transaction_manager;

public interface TransactionManager {
    void begin();
    void commit();
}
