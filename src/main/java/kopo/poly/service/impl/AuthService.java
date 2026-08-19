package kopo.poly.service.impl;

import kopo.poly.dto.AuthenticatedUserDTO;
import kopo.poly.dto.CurrentUserDTO;
import kopo.poly.dto.EmailAuthResponseDTO;
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
import kopo.poly.dto.UserCreateDTO;
import kopo.poly.dto.VerifyCodeRequestDTO;
import kopo.poly.mapper.IUserMapper;
import kopo.poly.service.AuthServiceException;
import kopo.poly.service.IAuthService;
import kopo.poly.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles authentication and account-management business logic.
 */
@Service
public class AuthService implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    // New passwords use BCrypt; legacy SHA-256 hashes are upgraded after login.
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final IUserMapper userMapper;
    private final EmailAuthService emailAuthService;

    public AuthService(IUserMapper userMapper,
                       EmailAuthService emailAuthService) {
        this.userMapper = userMapper;
        this.emailAuthService = emailAuthService;
    }

    @Override
    @Transactional
    public AuthenticatedUserDTO login(LoginRequestDTO req) {
        // Normalize input before validating credentials.
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim();
        String password = req == null || req.getPassword() == null ? "" : req.getPassword();

        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
        if (password.isBlank()) {
            throw new AuthServiceException("AU-4002", "비밀번호를 입력해 주세요.");
        }

        Map<String, Object> user = userMapper.selectUserByEmail(email);
        if (user == null) {
            // Do not reveal whether an account exists.
            throw invalidCredential();
        }

        // Support aliases returned by different MyBatis mappings.
        String storedPassword = firstNonBlank(
                user.get("passwordHash"),
                user.get("password_hash"),
                user.get("PASSWORDHASH"),
                user.get("PASSWORD_HASH"),
                user.get("password"),
                user.get("PASSWORD")
        );
        // Missing stored password means the account cannot be authenticated.
        if (storedPassword.isBlank()) {
            log.warn("Login failed because password column is empty for email={}", email);
            throw invalidCredential();
        }

        if (!passwordMatches(password, storedPassword)) {
            throw invalidCredential();
        }
        // Upgrade a legacy hash after successful authentication.
        if (isLegacySha256(storedPassword)) {
            userMapper.updatePasswordByEmail(email, PASSWORD_ENCODER.encode(password));
        }

        return new AuthenticatedUserDTO(user.get("id"), user.get("name"), user.get("email"));
    }

    @Override
    public FindIdResponseDTO findId(FindIdRequestDTO req) {
        String name = req == null || req.getName() == null ? "" : req.getName().trim();
        String phoneNumber = req == null || req.getPhoneNumber() == null ? "" : req.getPhoneNumber().trim();

        if (name.isBlank()) {
            throw new AuthServiceException("AU-4003", "이름을 입력해 주세요.");
        }
        if (phoneNumber.isBlank()) {
            throw new AuthServiceException("AU-4005", "전화번호를 입력해 주세요.");
        }

        Map<String, Object> user = userMapper.selectUserByNameAndPhone(name, phoneNumber);
        if (user == null) {
            throw new AuthServiceException("AU-4041", "일치하는 계정을 찾을 수 없습니다.");
        }

        return new FindIdResponseDTO(user.get("email"), user.get("name"));
    }

    @Override
    public void sendPasswordResetCode(FindPasswordRequestDTO req) {
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim();
        String name = req == null || req.getName() == null ? "" : req.getName().trim();

        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
        if (name.isBlank()) {
            throw new AuthServiceException("AU-4003", "이름을 입력해 주세요.");
        }

        Map<String, Object> user = userMapper.selectUserByEmailAndName(email, name);
        if (user == null) {
            throw new AuthServiceException("AU-4042", "일치하는 계정을 찾을 수 없습니다.");
        }

        EmailAuthResponseDTO response;
        try {
            response = emailAuthService.sendAuthCode(email);
        } catch (IllegalArgumentException e) {
            throw new AuthServiceException("AU-4001", e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Password send-code mail error", e);
            throw new AuthServiceException("AU-5005", "인증번호 발송에 실패했습니다.");
        }
        if (!response.isSuccess()) {
            throw new AuthServiceException("AU-5005", response.getMessage());
        }
    }

    @Override
    public void verifyPasswordResetCode(VerifyCodeRequestDTO req) {
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        String code = req == null || req.getCode() == null ? "" : req.getCode();

        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
        if (code.isBlank()) {
            throw new AuthServiceException("AU-4004", "인증번호를 입력해 주세요.");
        }

        EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(email, code);
        if (!response.isSuccess()) {
            throw new AuthServiceException("AU-4013", response.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO req) {
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim();
        String password = req == null || req.getPassword() == null ? "" : req.getPassword();

        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
        if (password.length() < 6) {
            throw new AuthServiceException("AU-4002", "비밀번호는 최소 6자 이상이어야 합니다.");
        }
        if (!emailAuthService.isEmailVerified(email)) {
            throw new AuthServiceException("AU-4012", "이메일 인증을 먼저 완료해 주세요.");
        }

        Map<String, Object> user = userMapper.selectUserByEmail(email);
        if (user == null) {
            throw new AuthServiceException("AU-4042", "일치하는 계정을 찾을 수 없습니다.");
        }

        String oauthProvider = firstNonBlank(user.get("oauthProvider"), user.get("oauth_provider"), user.get("OAUTH_PROVIDER"));
        if (!oauthProvider.isBlank()) {
            throw new AuthServiceException("AU-4030", "소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다.");
        }

        int updated = userMapper.updatePasswordByEmail(email, PASSWORD_ENCODER.encode(password));
        if (updated < 1) {
            throw new AuthServiceException("AU-5007", "비밀번호 변경에 실패했습니다.");
        }
        // Consume verification so it cannot be reused.
        emailAuthService.expireEmailVerification(email);
    }

    @Override
    public EmailAvailabilityDTO checkEmail(SendCodeRequestDTO req) {
        String email = normalizeEmail(req);
        assertEmail(email);
        assertEmailNotRegistered(email);
        return new EmailAvailabilityDTO(true, email);
    }

    @Override
    public void sendSignupCode(SendCodeRequestDTO req) {
        String email = normalizeEmail(req);
        assertEmail(email);
        assertEmailNotRegistered(email);

        EmailAuthResponseDTO response;
        try {
            response = emailAuthService.sendAuthCode(email);
        } catch (IllegalArgumentException e) {
            throw new AuthServiceException("AU-4001", e.getMessage());
        } catch (IllegalStateException e) {
            throw new AuthServiceException("AU-5001", "메일 발송 설정을 확인해 주세요.");
        }
        if (!response.isSuccess()) {
            throw new AuthServiceException("AU-5001", response.getMessage());
        }
    }

    @Override
    public void verifySignupCode(VerifyCodeRequestDTO req) {
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        String code = req == null || req.getCode() == null ? "" : req.getCode();

        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
        if (code.isBlank()) {
            throw new AuthServiceException("AU-4004", "인증번호를 입력해 주세요.");
        }
        EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(email, code);
        if (!response.isSuccess()) {
            throw new AuthServiceException("AU-4011", response.getMessage());
        }
    }

    @Override
    @Transactional
    public void completeSignup(SignupCompleteRequestDTO req) {
        String email = req == null || req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
        if (req == null || req.getPassword() == null || req.getPassword().length() < 6) {
            throw new AuthServiceException("AU-4002", "비밀번호는 최소 6자 이상이어야 합니다.");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new AuthServiceException("AU-4003", "이름을 입력해 주세요.");
        }
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            throw new AuthServiceException("AU-4005", "전화번호를 입력해 주세요.");
        }
        if (req.getAddress() == null || req.getAddress().isBlank()) {
            throw new AuthServiceException("AU-4006", "주소를 입력해 주세요.");
        }
        if (!emailAuthService.isEmailVerified(email)) {
            throw new AuthServiceException("AU-4012", "이메일 인증을 먼저 완료해 주세요.");
        }

        assertEmailNotRegistered(email);

        String address = buildFullAddress(req.getAddress(), req.getDetailAddress(), req.getZonecode());
        UserCreateDTO userCreateDTO = new UserCreateDTO(
                req.getName(),
                email,
                PASSWORD_ENCODER.encode(req.getPassword()),
                req.getPhoneNumber(),
                address
        );
        userMapper.insertUser(userCreateDTO);
        emailAuthService.expireEmailVerification(email);

    }

    @Override
    public CurrentUserDTO getCurrentUser(Long userId) {
        assertLoggedIn(userId);

        Map<String, Object> user = userMapper.selectUserById(userId);
        if (user == null) {
            throw new AuthServiceException("AU-4040", "사용자를 찾을 수 없습니다.");
        }

        String oauthProvider = firstNonBlank(user.get("oauthProvider"), user.get("oauth_provider"), user.get("OAUTH_PROVIDER"));
        return new CurrentUserDTO(
                user.get("id"),
                user.get("name"),
                user.get("email"),
                firstNonBlank(user.get("phoneNumber"), user.get("phone_number"), user.get("PHONE_NUMBER")),
                firstNonBlank(user.get("address"), user.get("ADDRESS")),
                oauthProvider
        );
    }

    @Override
    public ProfileResponseDTO updateProfile(Long userId, ProfileUpdateRequestDTO req) {
        assertLoggedIn(userId);

        String name = req == null ? "" : firstNonBlank(req.getName());
        String phoneNumber = req == null ? "" : firstNonBlank(req.getPhoneNumber(), req.getPhone());
        String address = req == null ? "" : buildFullAddress(req.getAddress(), req.getDetailAddress(), req.getZonecode());

        if (name.isBlank()) {
            throw new AuthServiceException("AU-4003", "이름을 입력해 주세요.");
        }
        if (!phoneNumber.isBlank() && !phoneNumber.matches("^\\d{3}-\\d{4}-\\d{4}$")) {
            throw new AuthServiceException("AU-4005", "전화번호 형식이 올바르지 않습니다. 예: 010-1234-5678");
        }

        int updated = userMapper.updateProfileById(userId, name, phoneNumber, address);
        if (updated < 1) {
            throw new AuthServiceException("AU-4040", "사용자를 찾을 수 없습니다.");
        }

        return new ProfileResponseDTO(name, phoneNumber, address);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequestDTO req) {
        assertLoggedIn(userId);

        String currentPassword = req == null ? "" : firstNonBlank(req.getCurrentPassword());
        String newPassword = req == null ? "" : firstNonBlank(req.getNewPassword(), req.getPassword());
        if (currentPassword.isBlank()) {
            throw new AuthServiceException("AU-4002", "현재 비밀번호를 입력해 주세요.");
        }
        if (newPassword.length() < 6) {
            throw new AuthServiceException("AU-4002", "새 비밀번호는 최소 6자 이상이어야 합니다.");
        }

        Map<String, Object> user = userMapper.selectUserById(userId);
        if (user == null) {
            throw new AuthServiceException("AU-4040", "사용자를 찾을 수 없습니다.");
        }

        String oauthProvider = firstNonBlank(user.get("oauthProvider"), user.get("oauth_provider"), user.get("OAUTH_PROVIDER"));
        if (!oauthProvider.isBlank()) {
            throw new AuthServiceException("AU-4030", "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        String storedPassword = firstNonBlank(
                user.get("passwordHash"),
                user.get("password_hash"),
                user.get("PASSWORDHASH"),
                user.get("PASSWORD_HASH"),
                user.get("password"),
                user.get("PASSWORD")
        );
        if (storedPassword.isBlank() || !passwordMatches(currentPassword, storedPassword)) {
            throw new AuthServiceException("AU-4010", "현재 비밀번호가 일치하지 않습니다.");
        }

        int updated = userMapper.updatePasswordById(userId, PASSWORD_ENCODER.encode(newPassword));
        if (updated < 1) {
            throw new AuthServiceException("AU-5011", "비밀번호 변경에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        // Delete dependent records before removing the account.
        assertLoggedIn(userId);

        userMapper.deleteCommentLikesByUserId(userId);
        userMapper.deletePostLikesByUserId(userId);
        userMapper.deleteCommentsByUserId(userId);
        userMapper.deletePostLikesByPostOwnerId(userId);
        userMapper.deletePostsByUserId(userId);
        userMapper.deleteVerificationsByUserId(userId);
        userMapper.deleteVerificationRecordsByUserId(userId);

        int deleted = userMapper.deleteUserById(userId);
        if (deleted < 1) {
            throw new AuthServiceException("AU-4040", "사용자를 찾을 수 없습니다.");
        }
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }
        try {
            return isLegacySha256(storedPassword) && HashUtil.sha256(rawPassword).equalsIgnoreCase(storedPassword);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password.", e);
        }
    }

    private boolean isLegacySha256(String storedPassword) {
        return storedPassword != null && storedPassword.matches("(?i)^[0-9a-f]{64}$");
    }

    private void assertLoggedIn(Long userId) {
        if (userId == null) {
            throw new AuthServiceException("AU-4010", "로그인이 필요합니다.");
        }
    }

    private String normalizeEmail(SendCodeRequestDTO req) {
        return req == null || req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
    }

    private void assertEmail(String email) {
        if (email.isBlank()) {
            throw new AuthServiceException("AU-4001", "이메일을 입력해 주세요.");
        }
    }

    private void assertEmailNotRegistered(String email) {
        Integer cnt = userMapper.existsByEmail(email);
        if (cnt != null && cnt > 0) {
            throw new AuthServiceException("AU-4090", "이미 가입된 이메일입니다.");
        }
    }

    private AuthServiceException invalidCredential() {
        return new AuthServiceException("AU-4010", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    private String buildFullAddress(String address, String detailAddress, String zonecode) {
        String base = address == null ? "" : address.trim();
        String detail = detailAddress == null ? "" : detailAddress.trim();
        String zip = zonecode == null ? "" : zonecode.trim();

        StringBuilder fullAddress = new StringBuilder();
        if (!zip.isBlank()) {
            fullAddress.append("(").append(zip).append(") ");
        }
        fullAddress.append(base);
        if (!detail.isBlank()) {
            fullAddress.append(" ").append(detail);
        }
        return fullAddress.toString().trim();
    }

}
