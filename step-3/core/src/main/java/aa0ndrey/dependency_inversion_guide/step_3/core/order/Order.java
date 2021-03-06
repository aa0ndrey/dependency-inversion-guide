package aa0ndrey.dependency_inversion_guide.step_3.core.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Order {
    private UUID id;
    private UUID userId;
    private UUID productId;
}
