package kopo.poly.controller.api;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.EmailAuthResponseDTO;
import kopo.poly.dto.FindIdRequestDTO;
import kopo.poly.dto.FindPasswordRequestDTO;
import kopo.poly.dto.LoginRequestDTO;
import kopo.poly.dto.ResetPasswordRequestDTO;
import kopo.poly.dto.SendCodeRequestDTO;
import kopo.poly.dto.SignupCompleteRequestDTO;
import kopo.poly.dto.SignupRequestDTO;
import kopo.poly.dto.VerifyCodeRequestDTO;
import kopo.poly.mapper.IUserMapper;
import kopo.poly.service.IEmailVerificationService;
import kopo.poly.service.impl.EmailAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 회원가입, 로그인, 아이디/비밀번호 찾기 API를 제공하는 인증 컨트롤러다.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    // 인증 API 전체에서 같은 로거를 사용한다.
    // DB 오류나 예외는 서버 로그에 남기고, 화면에는 ApiResponse 형태의 결과만 돌려준다.
    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    // app_user 테이블을 조회/저장/수정/삭제하는 MyBatis 매퍼다.
    private final IUserMapper userMapper;

    // 회원가입 완료 안내 메일처럼 인증번호가 아닌 알림 메일을 보내는 서비스다.
    private final IEmailVerificationService emailVerificationService;

    // 회원가입/비밀번호 재설정에서 인증번호 발송과 검증을 담당하는 서비스다.
    private final EmailAuthService emailAuthService;

    public AuthApiController(IUserMapper userMapper,
                             IEmailVerificationService emailVerificationService,
                             EmailAuthService emailAuthService) {
        this.userMapper = userMapper;
        this.emailVerificationService = emailVerificationService;
        this.emailAuthService = emailAuthService;
    }

    @PostMapping("/signup")
    public ApiResponse<Object> signup(@RequestBody SignupRequestDTO req) {
        // 예전 단일 회원가입 API를 막아 둔 엔드포인트다.
        // 현재 회원가입은 중복확인 -> 인증번호 발송 -> 인증번호 확인 -> 최종 가입 순서로만 진행된다.
        return ApiResponse.fail("AU-4000",
                "직접 회원가입 API는 사용하지 않습니다. send-code, verify-code, complete 순서로 진행해 주세요.");
    }

    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody LoginRequestDTO req, HttpSession session) {
        try {
            // 화면에서 넘어온 이메일은 앞뒤 공백을 제거한다.
            // 비밀번호는 입력값 그대로 해시해야 하므로 trim 하지 않는다.
            String email = req.getEmail() == null ? "" : req.getEmail().trim();
            String password = req.getPassword() == null ? "" : req.getPassword();

            // 필수값 검증: 빈 이메일/비밀번호는 DB 조회 전에 바로 실패 처리한다.
            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }
            if (password.isBlank()) {
                return ApiResponse.fail("AU-4002", "비밀번호를 입력해 주세요.");
            }

            // 이메일로 사용자 한 명을 조회한다.
            // 실제 SQL은 UserMapper.xml의 selectUserByEmail이 실행한다.
            Map<String, Object> user = userMapper.selectUserByEmail(email);
            if (user == null) {
                return ApiResponse.fail("AU-4010", "이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // MyBatis가 Map으로 돌려주는 컬럼명이 환경마다 달라질 수 있어 여러 후보 키를 순서대로 확인한다.
            String storedPassword = firstNonBlank(
                    user.get("passwordHash"),
                    user.get("password_hash"),
                    user.get("PASSWORDHASH"),
                    user.get("PASSWORD_HASH"),
                    user.get("password"),
                    user.get("PASSWORD")
            );
            // 회원가입 때 저장한 방식과 맞추기 위해 입력 비밀번호를 SHA-256으로 해시한다.
            String inputHash = sha256(password);

            if (storedPassword.isBlank()) {
                log.warn("Login failed because password column is empty for email={}", email);
                return ApiResponse.fail("AU-4010", "이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // 신규 가입자는 해시값으로 비교하고, 기존 개발 중 평문으로 저장된 계정은 호환 비교한다.
            boolean matchesHashed = inputHash.equalsIgnoreCase(storedPassword);
            boolean matchesPlain = password.equals(storedPassword);

            // 기존 평문 비밀번호 행과의 호환 처리다. 신규/재설정 비밀번호는 SHA-256으로 저장된다.
            if (!matchesHashed && !matchesPlain) {
                return ApiResponse.fail("AU-4010", "이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // 컨트롤러와 JSP는 이 세션 값을 로그인 상태로 사용한다.
            // 로그인 성공 시 세션에 사용자 정보를 저장한다.
            // 이후 JSP와 API는 USER_ID가 있으면 로그인 상태로 판단한다.
            session.setAttribute("USER_ID", user.get("id"));
            session.setAttribute("USER_NAME", user.get("name"));
            session.setAttribute("USER_EMAIL", user.get("email"));

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Login DB error", e);
            return ApiResponse.fail("AU-5000",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5000", "서버 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/find-id")
    public ApiResponse<Object> findId(@RequestBody FindIdRequestDTO req) {
        try {
            String name = req.getName() == null ? "" : req.getName().trim();
            String phoneNumber = req.getPhoneNumber() == null ? "" : req.getPhoneNumber().trim();

            if (name.isBlank()) {
                return ApiResponse.fail("AU-4003", "이름을 입력해 주세요.");
            }
            if (phoneNumber.isBlank()) {
                return ApiResponse.fail("AU-4005", "전화번호를 입력해 주세요.");
            }

            Map<String, Object> user = userMapper.selectUserByNameAndPhone(name, phoneNumber);
            if (user == null) {
                return ApiResponse.fail("AU-4041", "입력한 정보와 일치하는 아이디를 찾을 수 없습니다.");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("email", user.get("email"));
            result.put("name", user.get("name"));

            return ApiResponse.ok(result);
        } catch (DataAccessException e) {
            log.error("Find-id DB error", e);
            return ApiResponse.fail("AU-5004",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5004", "아이디 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/password/send-code")
    public ApiResponse<Object> sendPasswordResetCode(@RequestBody FindPasswordRequestDTO req) {
        try {
            String email = req.getEmail() == null ? "" : req.getEmail().trim();
            String name = req.getName() == null ? "" : req.getName().trim();

            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }
            if (name.isBlank()) {
                return ApiResponse.fail("AU-4003", "이름을 입력해 주세요.");
            }

            Map<String, Object> user = userMapper.selectUserByEmailAndName(email, name);
            if (user == null) {
                return ApiResponse.fail("AU-4042", "입력한 정보와 일치하는 회원을 찾을 수 없습니다.");
            }

            EmailAuthResponseDTO response = emailAuthService.sendAuthCode(email);
            if (!response.isSuccess()) {
                return ApiResponse.fail("AU-5005", response.getMessage());
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Password send-code DB error", e);
            return ApiResponse.fail("AU-5005",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("AU-4001", e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.fail("AU-5005", e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("AU-5005", "인증번호 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/password/verify-code")
    public ApiResponse<Object> verifyPasswordResetCode(@RequestBody VerifyCodeRequestDTO req) {
        try {
            String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }
            if (req.getCode() == null || req.getCode().isBlank()) {
                return ApiResponse.fail("AU-4004", "인증 코드를 입력해 주세요.");
            }

            EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(email, req.getCode());
            if (!response.isSuccess()) {
                return ApiResponse.fail("AU-4013", response.getMessage());
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Password verify-code DB error", e);
            return ApiResponse.fail("AU-5006",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5006", "인증 코드 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/password/reset")
    public ApiResponse<Object> resetPassword(@RequestBody ResetPasswordRequestDTO req) {
        try {
            String email = req.getEmail() == null ? "" : req.getEmail().trim();
            String password = req.getPassword() == null ? "" : req.getPassword();

            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }
            if (password.length() < 6) {
                return ApiResponse.fail("AU-4002", "비밀번호는 최소 6자 이상이어야 합니다.");
            }
            if (!emailAuthService.isEmailVerified(email)) {
                return ApiResponse.fail("AU-4012", "이메일 인증을 먼저 완료해 주세요.");
            }

            Map<String, Object> user = userMapper.selectUserByEmail(email);
            if (user == null) {
                return ApiResponse.fail("AU-4042", "입력한 정보와 일치하는 회원을 찾을 수 없습니다.");
            }

            String hash = sha256(password);
            int updated = userMapper.updatePasswordByEmail(email, hash);
            if (updated < 1) {
                return ApiResponse.fail("AU-5007", "비밀번호 변경에 실패했습니다.");
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Password reset DB error", e);
            return ApiResponse.fail("AU-5007",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5007", "비밀번호 재설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/signup/check-email")
    public ApiResponse<Object> checkEmail(@RequestBody SendCodeRequestDTO req) {
        try {
            // 중복확인은 대소문자 차이로 다른 이메일처럼 처리되지 않도록 소문자로 통일한다.
            String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }

            // app_user 테이블에 같은 이메일이 있는지 COUNT로 확인한다.
            Integer cnt = userMapper.existsByEmail(email);
            if (cnt != null && cnt > 0) {
                return ApiResponse.fail("AU-4090", "이미 가입된 이메일입니다.");
            }

            // 프론트에서는 available=true를 보고 인증번호 받기 버튼을 활성화한다.
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("available", true);
            result.put("email", email);
            return ApiResponse.ok(result);
        } catch (DataAccessException e) {
            log.error("Signup check-email DB error", e);
            return ApiResponse.fail("AU-5008",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5008", "이메일 중복 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/signup/send-code")
    public ApiResponse<Object> sendCode(@RequestBody SendCodeRequestDTO req) {
        try {
            // 인증번호를 보낼 때도 이메일을 다시 정규화한다.
            // 화면에서 중복확인을 했더라도 서버가 한 번 더 검사해야 안전하다.
            String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }

            // 중복확인 후 다른 요청이 먼저 가입하는 상황을 막기 위한 2차 중복 검사다.
            Integer cnt = userMapper.existsByEmail(email);
            if (cnt != null && cnt > 0) {
                return ApiResponse.fail("AU-4090", "이미 가입된 이메일입니다.");
            }

            // EmailAuthService가 인증번호를 생성하고 메일을 발송한다.
            // 발송 성공 후 서비스 내부 저장소에 이메일별 인증 상태가 보관된다.
            EmailAuthResponseDTO response = emailAuthService.sendAuthCode(email);
            if (!response.isSuccess()) {
                return ApiResponse.fail("AU-5001", response.getMessage());
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Signup send-code DB error", e);
            return ApiResponse.fail("AU-5001",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("AU-4001", e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.fail("AU-5001", "메일 발송 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5001", "인증 메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/signup/verify-code")
    public ApiResponse<Object> verifyCode(@RequestBody VerifyCodeRequestDTO req) {
        try {
            // 인증번호 검증도 같은 이메일 문자열을 사용해야 하므로 소문자로 통일한다.
            String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }
            if (req.getCode() == null || req.getCode().isBlank()) {
                return ApiResponse.fail("AU-4004", "인증 코드를 입력해 주세요.");
            }

            // 인증번호가 맞으면 EmailAuthService 내부에서 해당 이메일을 "인증 완료" 상태로 바꾼다.
            EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(email, req.getCode());
            if (!response.isSuccess()) {
                return ApiResponse.fail("AU-4011", response.getMessage());
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Signup verify-code DB error", e);
            return ApiResponse.fail("AU-5002",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5002", "인증 코드 확인에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/signup/complete")
    public ApiResponse<Object> complete(@RequestBody SignupCompleteRequestDTO req) {
        try {
            // 최종 가입 단계에서도 이메일을 소문자로 통일해서 DB 저장 기준을 맞춘다.
            String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
            // 서버 필수값 검증이다. 화면 검증을 우회해도 서버에서 한 번 더 막아야 데이터가 깨지지 않는다.
            if (email.isBlank()) {
                return ApiResponse.fail("AU-4001", "이메일을 입력해 주세요.");
            }
            if (req.getPassword() == null || req.getPassword().length() < 6) {
                return ApiResponse.fail("AU-4002", "비밀번호는 최소 6자 이상이어야 합니다.");
            }
            if (req.getName() == null || req.getName().isBlank()) {
                return ApiResponse.fail("AU-4003", "이름을 입력해 주세요.");
            }
            if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
                return ApiResponse.fail("AU-4005", "전화번호를 입력해 주세요.");
            }
            if (req.getAddress() == null || req.getAddress().isBlank()) {
                return ApiResponse.fail("AU-4006", "주소를 입력해 주세요.");
            }

            // 인증번호 확인을 완료한 이메일만 가입을 허용한다.
            if (!emailAuthService.isEmailVerified(email)) {
                return ApiResponse.fail("AU-4012", "이메일 인증을 먼저 완료해 주세요.");
            }

            // 최종 저장 직전에도 중복을 한 번 더 확인한다.
            // 중복확인 버튼은 UX용이고, 실제 안전장치는 서버의 최종 중복 검사다.
            Integer cnt = userMapper.existsByEmail(email);
            if (cnt != null && cnt > 0) {
                return ApiResponse.fail("AU-4090", "이미 가입된 이메일입니다.");
            }

            // 주소검색 API가 준 기본주소, 상세주소, 우편번호를 DB에 저장할 한 줄 주소로 합친다.
            String address = buildFullAddress(req.getAddress(), req.getDetailAddress(), req.getZonecode());
            // 비밀번호는 평문 저장하지 않고 SHA-256 해시로 변환해서 저장한다.
            String hash = sha256(req.getPassword());
            // app_user 테이블에 회원 정보를 INSERT한다.
            userMapper.insertUser(req.getName(), email, hash, req.getPhoneNumber(), address);

            try {
                // 가입 성공 안내 메일은 부가 기능이라 실패해도 회원가입 자체는 성공으로 둔다.
                emailVerificationService.sendSignupCompletedMail(email, req.getName());
            } catch (Exception ignored) {
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Signup complete DB error", e);
            return ApiResponse.fail("AU-5003",
                    "DB 연결에 실패했습니다. MariaDB 서버와 datasource 설정을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5003", "회원가입에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(HttpSession session) {
        // 세션을 무효화하면 서버가 기억하던 로그인 상태가 삭제된다.
        session.invalidate();
        return ApiResponse.ok(true);
    }

    @GetMapping("/me")
    public ApiResponse<Object> me(HttpSession session) {
        Long userId = getSessionUserId(session);
        if (userId == null) {
            return ApiResponse.fail("AU-4010", "로그인이 필요합니다.");
        }

        Map<String, Object> user = userMapper.selectUserById(userId);
        if (user == null) {
            return ApiResponse.fail("AU-4040", "회원 정보를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.get("id"));
        result.put("name", user.get("name"));
        result.put("email", user.get("email"));
        result.put("phone", firstNonBlank(user.get("phoneNumber"), user.get("phone_number"), user.get("PHONE_NUMBER")));
        result.put("address", firstNonBlank(user.get("address"), user.get("ADDRESS")));
        return ApiResponse.ok(result);
    }

    @PutMapping("/profile")
    public ApiResponse<Object> updateProfile(@RequestBody Map<String, Object> req, HttpSession session) {
        Long userId = getSessionUserId(session);
        if (userId == null) {
            return ApiResponse.fail("AU-4010", "로그인이 필요합니다.");
        }

        String name = firstNonBlank(req.get("name"));
        String phoneNumber = firstNonBlank(req.get("phoneNumber"), req.get("phone"));
        String address = buildFullAddress(
                firstNonBlank(req.get("address")),
                firstNonBlank(req.get("detailAddress")),
                firstNonBlank(req.get("zonecode"))
        );

        if (name.isBlank()) {
            return ApiResponse.fail("AU-4003", "이름을 입력해 주세요.");
        }
        if (!phoneNumber.isBlank() && !phoneNumber.matches("^\\d{3}-\\d{4}-\\d{4}$")) {
            return ApiResponse.fail("AU-4005", "전화번호 형식을 확인해 주세요. 예: 010-1234-5678");
        }

        try {
            int updated = userMapper.updateProfileById(userId, name, phoneNumber, address);
            if (updated < 1) {
                return ApiResponse.fail("AU-4040", "회원 정보를 찾을 수 없습니다.");
            }

            session.setAttribute("USER_NAME", name);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", name);
            result.put("phone", phoneNumber);
            result.put("address", address);
            return ApiResponse.ok(result);
        } catch (DataAccessException e) {
            log.error("Profile update DB error", e);
            return ApiResponse.fail("AU-5010", "프로필 수정 중 DB 오류가 발생했습니다.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5010", "프로필 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    public ApiResponse<Object> changePassword(@RequestBody Map<String, Object> req, HttpSession session) {
        Long userId = getSessionUserId(session);
        if (userId == null) {
            return ApiResponse.fail("AU-4010", "로그인이 필요합니다.");
        }

        String currentPassword = firstNonBlank(req.get("currentPassword"));
        String newPassword = firstNonBlank(req.get("newPassword"), req.get("password"));
        if (currentPassword.isBlank()) {
            return ApiResponse.fail("AU-4002", "현재 비밀번호를 입력해 주세요.");
        }
        if (newPassword.length() < 6) {
            return ApiResponse.fail("AU-4002", "새 비밀번호는 최소 6자 이상이어야 합니다.");
        }

        try {
            Map<String, Object> user = userMapper.selectUserById(userId);
            if (user == null) {
                return ApiResponse.fail("AU-4040", "회원 정보를 찾을 수 없습니다.");
            }

            String storedPassword = firstNonBlank(
                    user.get("passwordHash"),
                    user.get("password_hash"),
                    user.get("PASSWORDHASH"),
                    user.get("PASSWORD_HASH"),
                    user.get("password"),
                    user.get("PASSWORD")
            );
            String currentHash = sha256(currentPassword);
            if (!currentHash.equalsIgnoreCase(storedPassword) && !currentPassword.equals(storedPassword)) {
                return ApiResponse.fail("AU-4010", "현재 비밀번호가 일치하지 않습니다.");
            }

            int updated = userMapper.updatePasswordById(userId, sha256(newPassword));
            if (updated < 1) {
                return ApiResponse.fail("AU-5011", "비밀번호 변경에 실패했습니다.");
            }

            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Password change DB error", e);
            return ApiResponse.fail("AU-5011", "비밀번호 변경 중 DB 오류가 발생했습니다.");
        } catch (Exception e) {
            return ApiResponse.fail("AU-5011", "비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/account")
    @Transactional
    public ApiResponse<Object> deleteAccount(HttpSession session) {
        Long userId = getSessionUserId(session);
        if (userId == null) {
            return ApiResponse.fail("AU-4010", "로그인이 필요합니다.");
        }

        try {
            userMapper.deleteCommentLikesByUserId(userId);
            userMapper.deletePostLikesByUserId(userId);
            userMapper.deleteCommentsByUserId(userId);
            userMapper.deletePostLikesByPostOwnerId(userId);
            userMapper.deletePostsByUserId(userId);
            userMapper.deleteVerificationsByUserId(userId);
            userMapper.deleteVerificationRecordsByUserId(userId);

            int deleted = userMapper.deleteUserById(userId);
            if (deleted < 1) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ApiResponse.fail("AU-4040", "삭제할 회원 정보를 찾을 수 없습니다.");
            }

            session.invalidate();
            return ApiResponse.ok(true);
        } catch (DataAccessException e) {
            log.error("Delete account DB error", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.fail("AU-5009", "회원탈퇴 처리 중 DB 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Delete account error", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.fail("AU-5009", "회원탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String sha256(String s) throws Exception {
        // 회원가입/로그인/비밀번호 변경에서 같은 SHA-256 해시 규칙을 사용한다.
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String firstNonBlank(Object... values) {
        // Map 결과에서 여러 후보 키 중 실제 값이 있는 첫 번째 문자열을 찾는 공통 유틸이다.
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

    private Long getSessionUserId(HttpSession session) {
        // 세션에 저장된 USER_ID를 Long 타입으로 변환한다.
        // 로그인하지 않았거나 값이 이상하면 null을 반환해 인증 실패로 처리한다.
        Object userIdObj = session.getAttribute("USER_ID");
        if (userIdObj == null) {
            return null;
        }

        if (userIdObj instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(userIdObj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildFullAddress(String address, String detailAddress, String zonecode) {
        // 주소검색 결과의 우편번호, 기본주소, 상세주소를 DB 저장용 문자열로 합친다.
        // 예: (12345) 서울시 ... 101동 101호
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
