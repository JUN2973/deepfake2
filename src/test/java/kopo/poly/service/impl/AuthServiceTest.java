package kopo.poly.service.impl;

import kopo.poly.dto.SendCodeRequestDTO;
import kopo.poly.mapper.IUserMapper;
import kopo.poly.service.AuthServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void checkEmailRejectsAlreadyRegisteredEmail() {
        IUserMapper userMapper = mock(IUserMapper.class);
        when(userMapper.existsByEmail("taken@example.com")).thenReturn(1);

        AuthService authService = new AuthService(userMapper, null);
        SendCodeRequestDTO request = new SendCodeRequestDTO();
        request.setEmail("taken@example.com");

        assertThrows(AuthServiceException.class, () -> authService.checkEmail(request));
    }
}
