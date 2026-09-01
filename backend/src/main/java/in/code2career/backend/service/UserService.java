package in.code2career.backend.service;

import in.code2career.backend.dto.UserDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createUser(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword()); // Pore amra eta BCrypt diye hash korbo
        user.setCreatedAt(LocalDateTime.now());
        user.setXp(0);
        user.setLevel(1);

        return userRepository.save(user);
    }
}