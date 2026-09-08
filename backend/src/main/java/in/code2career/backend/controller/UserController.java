package in.code2career.backend.controller;

import in.code2career.backend.dto.AuthResponseDto;
import in.code2career.backend.dto.LoginDto;
import in.code2career.backend.dto.UserDto;
import in.code2career.backend.dto.UserResponseDto;
import in.code2career.backend.mapper.UserMapper;
import in.code2career.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserMapper.mapToResponseDto(userService.createUser(userDto)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> loginUser(@Valid @RequestBody LoginDto loginDto) {
        String accessToken = userService.verifyLogin(loginDto);
        return ResponseEntity.ok(new AuthResponseDto(accessToken, "Bearer"));
    }
}
