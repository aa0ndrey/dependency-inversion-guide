package aa0ndrey.dependency_inversion_guide.step_5.core.order;

import aa0ndrey.dependency_inversion_guide.step_5.core.product.Product;
import aa0ndrey.dependency_inversion_guide.step_5.core.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class CreateOrderContext {
    private CreateOrderRequest request;
    private User user;
    private Product product;
    private Order createdOrder;
}
