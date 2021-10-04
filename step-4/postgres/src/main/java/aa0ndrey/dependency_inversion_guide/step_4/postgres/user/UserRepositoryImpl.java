package aa0ndrey.dependency_inversion_guide.step_4.postgres.user;

import aa0ndrey.dependency_inversion_guide.step_4.core.user.User;
import aa0ndrey.dependency_inversion_guide.step_4.core.user.UserRepository;

import java.util.UUID;

public class UserRepositoryImpl implements UserRepository {
    @Override
    public User find(UUID id) {
        //реализация select * from user where user.id = ?
        throw new UnsupportedOperationException();
    }
}
