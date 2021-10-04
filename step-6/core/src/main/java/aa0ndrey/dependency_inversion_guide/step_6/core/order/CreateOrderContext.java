package aa0ndrey.dependency_inversion_guide.step_6.core.order;

import aa0ndrey.dependency_inversion_guide.step_6.core.product.Product;
import aa0ndrey.dependency_inversion_guide.step_6.core.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOrderContext {
    private CreateOrderRequest request;
    private User user;
    private Product product;
    private Order createdOrder;
}
