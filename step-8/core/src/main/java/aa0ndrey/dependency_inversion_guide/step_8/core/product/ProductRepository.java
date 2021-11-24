package aa0ndrey.dependency_inversion_guide.step_8.core.product;

import java.util.UUID;

public interface ProductRepository {
    Product find(UUID id);
}
