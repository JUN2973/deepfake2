package kopo.poly.service;

import kopo.poly.dto.AuthenticatedUserDTO;
import kopo.poly.dto.CurrentUserDTO;
import kopo.poly.dto.EmailAvailabilityDTO;
import kopo.poly.dto.FindIdRequestDTO;
import kopo.poly.dto.FindIdResponseDTO;
import kopo.poly.dto.FindPasswordRequestDTO;
import kopo.poly.dto.LoginRequestDTO;
import kopo.poly.dto.PasswordChangeRequestDTO;
import kopo.poly.dto.ProfileResponseDTO;
import kopo.poly.dto.ProfileUpdateRequestDTO;
import kopo.poly.dto.ResetPasswordRequestDTO;
import kopo.poly.dto.SendCodeRequestDTO;
import kopo.poly.dto.SignupCompleteRequestDTO;
import kopo.poly.dto.VerifyCodeRequestDTO;

/**
 * 로그인처럼 인증 도메인에서 처리해야 하는 비즈니스 로직의 서비스 계약이다.
 */
public interface IAuthService {
    AuthenticatedUserDTO login(LoginRequestDTO req);

    FindIdResponseDTO findId(FindIdRequestDTO req);

    void sendPasswordResetCode(FindPasswordRequestDTO req);

    void verifyPasswordResetCode(VerifyCodeRequestDTO req);

    void resetPassword(ResetPasswordRequestDTO req);

    EmailAvailabilityDTO checkEmail(SendCodeRequestDTO req);

    void sendSignupCode(SendCodeRequestDTO req);

    void verifySignupCode(VerifyCodeRequestDTO req);

    void completeSignup(SignupCompleteRequestDTO req);

    CurrentUserDTO getCurrentUser(Long userId);

    ProfileResponseDTO updateProfile(Long userId, ProfileUpdateRequestDTO req);

    void changePassword(Long userId, PasswordChangeRequestDTO req);

    void deleteAccount(Long userId);
}
