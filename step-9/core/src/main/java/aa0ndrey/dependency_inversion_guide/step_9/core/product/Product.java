package aa0ndrey.dependency_inversion_guide.step_9.core.product;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Product {
    private UUID id;
    private String name;
    private int price;
}
