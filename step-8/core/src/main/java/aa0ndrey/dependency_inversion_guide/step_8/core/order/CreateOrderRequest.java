package aa0ndrey.dependency_inversion_guide.step_8.core.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateOrderRequest {
    private UUID userId;
    private UUID productId;
}
