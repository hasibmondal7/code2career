package in.code2career.backend.service;

import in.code2career.backend.dto.LoginDto;
import in.code2career.backend.dto.UserDto;
import in.code2career.backend.entity.User;

public interface UserService {

    User createUser(UserDto userDto);

    String verifyLogin(LoginDto loginDto);
}
