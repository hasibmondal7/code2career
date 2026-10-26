package in.code2career.backend.mapper;

import in.code2career.backend.dto.UserDto;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    @Test
    void normalizesEmailIndependentOfDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            UserDto dto = new UserDto();
            dto.setUsername("developer");
            dto.setEmail("TESTI@EXAMPLE.COM");

            assertEquals("testi@example.com", UserMapper.mapToEntity(dto, "encoded-password").getEmail());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
