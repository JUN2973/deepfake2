package kopo.poly.service.impl;

import kopo.poly.dto.EmailAuthResponseDTO;
import kopo.poly.entity.EmailAuthEntity;
import kopo.poly.mapper.IEmailAuthMapper;
import kopo.poly.service.IMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailAuthServiceTest {

    private static final String EMAIL = "victim@example.com";
    private static final String CODE = "123456";

    private IEmailAuthMapper emailAuthMapper;
    private EmailAuthService emailAuthService;

    @BeforeEach
    void setUp() {
        emailAuthMapper = mock(IEmailAuthMapper.class);
        emailAuthService = new EmailAuthService(emailAuthMapper, mock(IMailService.class));

        EmailAuthEntity latest = new EmailAuthEntity();
        latest.setId(1L);
        latest.setEmail(EMAIL);
        latest.setAuthCode(CODE);
        latest.setVerified("N");
        latest.setExpiresAt(LocalDateTime.now().plusMinutes(5)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        when(emailAuthMapper.selectLatestByEmail(EMAIL)).thenReturn(latest);
    }

    @Test
    void verifySucceedsWithCorrectCodeBeforeLockout() {
        emailAuthService.verifyAuthCode(EMAIL, "000000");

        EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(EMAIL, CODE);

        assertTrue(response.isSuccess());
        verify(emailAuthMapper).markVerified(1L);
    }

    @Test
    void correctCodeIsRejectedAfterTooManyFailures() {
        for (int i = 0; i < 5; i++) {
            emailAuthService.verifyAuthCode(EMAIL, "000000");
        }

        EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(EMAIL, CODE);

        assertFalse(response.isSuccess());
        verify(emailAuthMapper, never()).markVerified(anyLong());
        verify(emailAuthMapper, atLeastOnce()).expireAllByEmail(EMAIL);
    }
}
