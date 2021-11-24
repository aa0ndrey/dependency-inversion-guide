package aa0ndrey.dependency_inversion_guide.step_8.core.order;

public interface CreateOrderObserver {
    default void onStart() {}

    default void onEnd() {}

    default void onFinally() {}
}