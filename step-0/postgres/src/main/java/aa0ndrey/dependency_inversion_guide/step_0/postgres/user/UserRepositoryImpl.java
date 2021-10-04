package aa0ndrey.dependency_inversion_guide.step_0.postgres.user;

import aa0ndrey.dependency_inversion_guide.step_0.core.user.User;

import java.util.UUID;

public class UserRepositoryImpl {
    public User find(UUID id) {
        //реализация select * from user where user.id = ?
        throw new UnsupportedOperationException();
    }
}
