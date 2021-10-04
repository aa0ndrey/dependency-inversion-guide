package aa0ndrey.dependency_inversion_guide.step_3.core.order;

import aa0ndrey.dependency_inversion_guide.step_3.core.product.Product;
import aa0ndrey.dependency_inversion_guide.step_3.core.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

public class CreateOrderEvents {
    @Data
    @AllArgsConstructor
    public static class Start {
        private CreateOrderRequest request;
    }

    @Data
    @AllArgsConstructor
    public static class End {
        private CreateOrderRequest request;
        private User user;
        private Product product;
        private Order order;
    }

}
