package aa0ndrey.dependency_inversion_guide.step_3.core.order;

public interface CreateOrderObserver {
    void onStart(CreateOrderEvents.Start event);

    void onEnd(CreateOrderEvents.End event);
}
