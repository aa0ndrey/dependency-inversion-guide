package aa0ndrey.dependency_inversion_guide.step_3.core.user;

import java.util.UUID;

public interface UserRepository {
    User find(UUID id);
}
