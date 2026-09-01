package in.code2career.backend.service;

import in.code2career.backend.dto.LoginDto;
import in.code2career.backend.dto.UserDto;
import in.code2career.backend.entity.User;
import in.code2career.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setXp(0);
        user.setLevel(1);

        return userRepository.save(user);
    }

    public String verifyLogin(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail());

        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return "Login Successful";
        }
        return "Invalid Credentials";
    }
}