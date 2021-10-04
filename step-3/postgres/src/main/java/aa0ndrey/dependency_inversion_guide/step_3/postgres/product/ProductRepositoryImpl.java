package aa0ndrey.dependency_inversion_guide.step_3.postgres.product;

import aa0ndrey.dependency_inversion_guide.step_3.core.product.Product;
import aa0ndrey.dependency_inversion_guide.step_3.core.product.ProductRepository;

import java.util.UUID;

public class ProductRepositoryImpl implements ProductRepository {
    @Override
    public Product find(UUID id) {
        //реализация select * from product where product.id = ?
        throw new UnsupportedOperationException();
    }
}
