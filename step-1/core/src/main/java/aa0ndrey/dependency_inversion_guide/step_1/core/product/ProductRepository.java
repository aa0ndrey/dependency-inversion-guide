package aa0ndrey.dependency_inversion_guide.step_1.core.product;

import java.util.UUID;

public interface ProductRepository {
    Product find(UUID id);
}
