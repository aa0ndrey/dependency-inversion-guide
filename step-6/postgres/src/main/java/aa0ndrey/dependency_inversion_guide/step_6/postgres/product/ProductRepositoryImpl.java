package aa0ndrey.dependency_inversion_guide.step_6.postgres.product;

import aa0ndrey.dependency_inversion_guide.step_6.core.product.Product;

import java.util.UUID;

public class ProductRepositoryImpl {
    public Product find(UUID id) {
        //реализация select * from product where product.id = ?
        throw new UnsupportedOperationException();
    }
}
