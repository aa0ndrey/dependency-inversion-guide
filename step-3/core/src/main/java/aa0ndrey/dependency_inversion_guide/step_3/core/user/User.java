package aa0ndrey.dependency_inversion_guide.step_3.core.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class User {
    private UUID id;
    private String name;
    private int balance;
}
