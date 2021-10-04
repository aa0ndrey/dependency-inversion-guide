package aa0ndrey.dependency_inversion_guide.step_6.core.order;

public interface CreateOrderObserver {
    void onStart(CreateOrderContext context);

    void onEnd(CreateOrderContext context);
}
