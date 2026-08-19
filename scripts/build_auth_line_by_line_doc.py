from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


OUT = "docs/회원가입_로그인_코드_한줄설명_보강본.docx"


def set_run_font(run, name="맑은 고딕", size=None, bold=None, color=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if color is not None:
        run.font.color.rgb = RGBColor.from_string(color)


def set_style_font(style, name="맑은 고딕", size=None, color=None, bold=None):
    style.font.name = name
    style._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if size is not None:
        style.font.size = Pt(size)
    if color is not None:
        style.font.color.rgb = RGBColor.from_string(color)
    if bold is not None:
        style.font.bold = bold


def shade_cell(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_width(cell, width):
    cell.width = width
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.find(qn("w:tcW"))
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:w"), str(int(width.inches * 1440)))
    tc_w.set(qn("w:type"), "dxa")


def set_cell_text(cell, text, bold=False, fill=None, code=False):
    cell.text = ""
    if fill:
        shade_cell(cell, fill)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.05
    run = p.add_run(text)
    set_run_font(run, name="Consolas" if code else "맑은 고딕", size=8.2 if code else 8.8, bold=bold)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def add_table(doc, headers, rows, widths):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    for i, header in enumerate(headers):
        set_cell_text(table.rows[0].cells[i], header, bold=True, fill="E8EEF5")
        set_cell_width(table.rows[0].cells[i], widths[i])
    for row in rows:
        cells = table.add_row().cells
        for i, value in enumerate(row):
            set_cell_text(cells[i], value, code=(i == 1 and headers[i] == "코드"))
            set_cell_width(cells[i], widths[i])
    doc.add_paragraph()
    return table


def add_line_table(doc, rows):
    add_table(
        doc,
        ["라인", "코드", "한 줄 설명"],
        rows,
        [Inches(0.65), Inches(3.35), Inches(5.5)],
    )


def add_bullet(doc, text):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run(text)
    set_run_font(run, size=9.5)


def add_note(doc, title, body):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    shade_cell(cell, "F4F6F9")
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run(title)
    set_run_font(run, size=9.5, bold=True, color="1F3A5F")
    p = cell.add_paragraph()
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run(body)
    set_run_font(run, size=9)
    doc.add_paragraph()


def add_flow_table(doc):
    add_table(
        doc,
        ["흐름", "이동 위치", "무슨 역할인지"],
        [
            ["회원가입 화면 진입", "/signup -> AuthPageController.signup() -> signup.jsp", "브라우저가 /signup으로 들어오면 회원가입 JSP 화면을 보여줍니다."],
            ["이메일 중복확인", "signup.jsp -> POST /api/v1/auth/signup/check-email -> AuthApiController.checkEmail() -> AuthService.checkEmail()", "DB의 app_user 테이블에 같은 이메일이 있는지 확인합니다."],
            ["인증번호 발송", "signup.jsp -> POST /api/v1/auth/signup/send-code -> AuthApiController.sendCode() -> AuthService.sendSignupCode() -> EmailAuthService.sendAuthCode()", "6자리 인증번호를 만들고 email_auth 테이블에 저장한 뒤 메일을 보냅니다."],
            ["인증번호 확인", "signup.jsp -> POST /api/v1/auth/signup/verify-code -> AuthApiController.verifyCode() -> AuthService.verifySignupCode() -> EmailAuthService.verifyAuthCode()", "입력한 인증번호와 DB의 최신 인증번호를 비교하고 맞으면 verified를 Y로 바꿉니다."],
            ["최종 회원가입", "signup.jsp submit -> POST /api/v1/auth/signup/complete -> AuthApiController.complete() -> AuthService.completeSignup() -> UserMapper.insertUser()", "필수값, 인증 여부, 중복 여부를 확인한 뒤 app_user 테이블에 회원을 저장합니다."],
            ["로그인", "login.jsp -> POST /api/v1/auth/login -> AuthApiController.login() -> AuthService.login() -> UserMapper.selectUserByEmail()", "이메일로 회원을 조회하고 BCrypt로 비밀번호를 비교한 뒤 세션에 사용자 정보를 저장합니다."],
            ["로그아웃", "화면 -> POST /api/v1/auth/logout -> AuthApiController.logout()", "session.invalidate()로 서버에 저장된 로그인 상태를 삭제합니다."],
        ],
        [Inches(1.5), Inches(4.0), Inches(4.0)],
    )


def add_path(doc, path):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run("코드 경로: ")
    set_run_font(run, size=9, bold=True, color="1F4D78")
    run = p.add_run(path)
    set_run_font(run, name="Consolas", size=8.8)


def build():
    doc = Document()
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width = Inches(11)
    section.page_height = Inches(8.5)
    section.top_margin = Inches(0.55)
    section.bottom_margin = Inches(0.55)
    section.left_margin = Inches(0.55)
    section.right_margin = Inches(0.55)

    styles = doc.styles
    set_style_font(styles["Normal"], size=9.5)
    styles["Normal"].paragraph_format.line_spacing = 1.12
    styles["Normal"].paragraph_format.space_after = Pt(4)
    set_style_font(styles["Heading 1"], size=14.5, color="2E74B5", bold=True)
    styles["Heading 1"].paragraph_format.space_before = Pt(12)
    styles["Heading 1"].paragraph_format.space_after = Pt(6)
    set_style_font(styles["Heading 2"], size=12.5, color="1F4D78", bold=True)
    styles["Heading 2"].paragraph_format.space_before = Pt(8)
    styles["Heading 2"].paragraph_format.space_after = Pt(4)

    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = title.add_run("회원가입 / 로그인 코드 한 줄 설명서")
    set_run_font(run, size=18, bold=True, color="0B2545")
    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = subtitle.add_run("DeepScan 프로젝트 | 발표 질문 대비용 상세 프린트")
    set_run_font(run, size=9.5, color="555555")

    add_note(
        doc,
        "읽는 순서",
        "화면 JSP가 fetch로 API를 호출하고, AuthApiController가 요청을 받아 AuthService로 넘기며, AuthService가 Mapper를 통해 DB를 조회/저장합니다. 로그인 성공 상태는 JWT가 아니라 HttpSession에 저장합니다.",
    )

    doc.add_heading("0. 전체 이동 흐름", level=1)
    add_flow_table(doc)
    add_note(
        doc,
        "발표 때 첫 문장",
        "회원가입과 로그인은 화면 JSP에서 바로 DB로 가는 구조가 아닙니다. JSP가 fetch로 API를 호출하고, Controller가 요청을 받은 뒤 Service에서 검증과 비즈니스 로직을 처리하고, Mapper XML의 SQL을 통해 MariaDB에 접근합니다.",
    )

    doc.add_heading("1. 화면 이동 컨트롤러", level=1)
    add_path(doc, "src/main/java/kopo/poly/controller/AuthPageController.java")
    add_line_table(doc, [
        ("1", "package kopo.poly.controller;", "이 클래스가 컨트롤러 패키지에 속한다는 선언입니다."),
        ("3", "import org.springframework.stereotype.Controller;", "@Controller 어노테이션을 쓰기 위해 가져옵니다."),
        ("4", "import org.springframework.web.bind.annotation.GetMapping;", "GET 방식 URL 매핑을 쓰기 위해 가져옵니다."),
        ("7", "@Controller", "이 클래스가 화면을 반환하는 Spring MVC 컨트롤러임을 Spring에게 알려줍니다."),
        ("8", "public class AuthPageController {", "회원가입, 로그인, 계정 찾기, 마이페이지 화면 이동을 담당하는 클래스입니다."),
        ("10", "@GetMapping(\"/signup\")", "브라우저가 /signup으로 GET 요청하면 아래 메서드가 실행됩니다."),
        ("11-13", "public String signup() { return \"signup\"; }", "문자열 signup을 반환합니다. 설정상 /WEB-INF/views/signup.jsp 화면을 보여줍니다."),
        ("15", "@GetMapping(\"/login\")", "/login 요청을 로그인 화면으로 연결합니다."),
        ("16-18", "public String login() { return \"login\"; }", "login.jsp를 렌더링하도록 화면 이름 login을 반환합니다."),
        ("20-23", "findAccount() -> \"find-account\"", "아이디/비밀번호 찾기 화면 find-account.jsp로 이동합니다."),
        ("25-28", "myPage() -> \"mypage\"", "마이페이지 화면 mypage.jsp로 이동합니다."),
    ])

    doc.add_heading("2. 로그인 화면 fetch 코드", level=1)
    add_path(doc, "src/main/webapp/WEB-INF/views/login.jsp")
    add_line_table(doc, [
        ("103", "href=\"<%= contextPath %>/oauth2/authorization/google\"", "구글 로그인 버튼입니다. 클릭하면 Google OAuth 시작 URL로 이동합니다."),
        ("169", "const response = await fetch(contextPath + \"/api/v1/auth/login\", {", "일반 로그인 API를 비동기로 호출합니다. contextPath는 프로젝트 기본 경로입니다."),
        ("170", "method: \"POST\",", "로그인 정보는 서버에 저장/검증해야 하므로 POST 방식으로 보냅니다."),
        ("171-173", "headers: { \"Content-Type\": \"application/json\" },", "요청 본문이 JSON 형식임을 서버에 알려줍니다."),
        ("174-177", "body: JSON.stringify({ email: email, password: password })", "화면에서 입력한 이메일과 비밀번호를 JSON 문자열로 변환해 보냅니다."),
        ("응답", "const json = await response.json()", "서버가 반환한 ApiResponse를 JSON으로 읽습니다."),
        ("성공", "if (json && json.success) ...", "success가 true면 로그인 성공으로 보고 홈 화면 등으로 이동합니다."),
        ("실패", "json.error.message", "로그인 실패 시 서버가 내려준 에러 메시지를 화면에 보여줍니다."),
    ])

    doc.add_heading("3. 회원가입 화면 fetch 코드", level=1)
    add_path(doc, "src/main/webapp/WEB-INF/views/signup.jsp")
    add_line_table(doc, [
        ("55", "<form id=\"signupForm\" ...>", "최종 회원가입 submit 이벤트를 연결하기 위한 폼입니다."),
        ("448", "fetch(\".../api/v1/auth/signup/check-email\", {", "이메일 중복 확인 API를 호출합니다."),
        ("449", "method: \"POST\",", "중복 확인할 이메일을 서버로 보내기 위해 POST를 사용합니다."),
        ("450", "headers: { \"Content-Type\": \"application/json\" },", "요청 데이터가 JSON임을 표시합니다."),
        ("451", "body: JSON.stringify({ email: email })", "사용자가 입력한 이메일만 JSON으로 보냅니다."),
        ("494", "fetch(\".../api/v1/auth/signup/send-code\", {", "인증번호 발송 API를 호출합니다."),
        ("497", "body: JSON.stringify({ email: email })", "인증번호를 받을 이메일을 서버에 전달합니다."),
        ("529", "fetch(\".../api/v1/auth/signup/verify-code\", {", "사용자가 입력한 인증번호 확인 API를 호출합니다."),
        ("532", "body: JSON.stringify({ email: email, code: code })", "이메일과 인증번호를 함께 서버로 보냅니다."),
        ("571", "signupForm.addEventListener(\"submit\", async function (event) {", "회원가입 버튼을 누르면 브라우저 기본 제출을 막고 JS로 API를 호출합니다."),
        ("572", "event.preventDefault();", "페이지 새로고침 없이 AJAX 방식으로 회원가입을 처리하기 위해 기본 제출을 막습니다."),
        ("575", "if (!state.verified) ...", "이메일 인증이 끝나지 않았으면 최종 가입을 막습니다."),
        ("597", "fetch(\".../api/v1/auth/signup/complete\", {", "최종 회원가입 API를 호출합니다."),
        ("600", "body: JSON.stringify(payload)", "이름, 이메일, 비밀번호, 전화번호, 주소 정보를 JSON으로 보냅니다."),
    ])

    doc.add_heading("4. 로그인 요청 DTO", level=1)
    add_path(doc, "src/main/java/kopo/poly/dto/LoginRequestDTO.java")
    add_line_table(doc, [
        ("1", "package kopo.poly.dto;", "DTO 클래스가 dto 패키지에 있음을 선언합니다."),
        ("8", "public class LoginRequestDTO {", "로그인 요청 데이터를 담는 클래스입니다."),
        ("9", "private String email;", "사용자가 입력한 이메일 값을 저장하는 필드입니다."),
        ("10", "private String password;", "사용자가 입력한 비밀번호 값을 저장하는 필드입니다."),
        ("12", "public String getEmail() { return email; }", "컨트롤러/서비스가 이메일 값을 읽을 때 사용합니다."),
        ("13", "public void setEmail(String email) { this.email = email; }", "JSON 요청의 email 값이 이 필드에 들어오도록 합니다."),
        ("15", "public String getPassword() { return password; }", "서비스가 비밀번호 값을 읽을 때 사용합니다."),
        ("16", "public void setPassword(String password) { this.password = password; }", "JSON 요청의 password 값이 이 필드에 들어오도록 합니다."),
    ])

    doc.add_heading("5. 인증 API 컨트롤러 - 로그인", level=1)
    add_path(doc, "src/main/java/kopo/poly/controller/api/AuthApiController.java")
    add_line_table(doc, [
        ("32", "private final IAuthService authService;", "컨트롤러가 실제 인증 로직을 직접 처리하지 않고 서비스에 맡기기 위한 필드입니다."),
        ("34-36", "public AuthApiController(IAuthService authService) { ... }", "생성자 주입으로 AuthService 구현체를 받아옵니다."),
        ("52", "@PostMapping(\"/login\")", "/api/v1/auth/login POST 요청을 이 메서드와 연결합니다."),
        ("53", "public ApiResponse<Object> login(...)", "로그인 요청을 받고 공통 응답 형식 ApiResponse로 결과를 반환합니다."),
        ("55", "AuthenticatedUserDTO user = authService.login(req);", "이메일/비밀번호 검증은 AuthService.login()에 위임합니다."),
        ("58", "request.changeSessionId();", "로그인 성공 후 세션 ID를 새로 바꿔 세션 고정 공격 위험을 줄입니다."),
        ("59", "session.setAttribute(\"USER_ID\", user.getId());", "로그인한 사용자의 DB id를 세션에 저장합니다."),
        ("60", "session.setAttribute(\"USER_NAME\", user.getName());", "화면에서 사용할 사용자 이름을 세션에 저장합니다."),
        ("61", "session.setAttribute(\"USER_EMAIL\", user.getEmail());", "사용자 이메일을 세션에 저장합니다."),
        ("63", "return ApiResponse.ok(true);", "로그인 성공 결과를 JSON으로 반환합니다."),
        ("64-66", "catch (AuthServiceException e)", "입력값 오류, 비밀번호 불일치 같은 인증 실패를 잡아 에러 코드와 메시지로 반환합니다."),
        ("67-70", "catch (DataAccessException e)", "DB 연결/조회 오류가 나면 로그를 남기고 DB 오류 메시지를 반환합니다."),
        ("71-74", "catch (Exception e)", "예상하지 못한 서버 오류를 잡아 서버 에러 응답을 반환합니다."),
    ])

    doc.add_heading("6. 인증 API 컨트롤러 - 회원가입", level=1)
    add_path(doc, "src/main/java/kopo/poly/controller/api/AuthApiController.java")
    add_line_table(doc, [
        ("45-49", "@PostMapping(\"/signup\")", "예전 단일 회원가입 API입니다. 현재는 직접 가입을 막고 단계별 가입만 허용합니다."),
        ("143", "@PostMapping(\"/signup/check-email\")", "이메일 중복 확인 API URL입니다."),
        ("145", "return ApiResponse.ok(authService.checkEmail(req));", "이메일 사용 가능 여부 확인을 서비스에 맡기고 성공 응답을 반환합니다."),
        ("159", "@PostMapping(\"/signup/send-code\")", "회원가입용 이메일 인증번호 발송 API URL입니다."),
        ("162", "authService.sendSignupCode(req);", "중복 확인과 메일 발송 요청을 서비스에 맡깁니다."),
        ("163", "return ApiResponse.ok(true);", "인증번호 발송이 성공하면 true를 반환합니다."),
        ("176", "@PostMapping(\"/signup/verify-code\")", "사용자가 입력한 인증번호를 검증하는 API URL입니다."),
        ("179", "authService.verifySignupCode(req);", "이메일과 인증번호 일치 여부 검증을 서비스에 맡깁니다."),
        ("180", "return ApiResponse.ok(true);", "검증 성공 시 true를 반환합니다."),
        ("193", "@PostMapping(\"/signup/complete\")", "최종 회원가입 저장 API URL입니다."),
        ("196", "authService.completeSignup(req);", "입력값 최종 검증, 비밀번호 해시, DB 저장을 서비스에 맡깁니다."),
        ("197", "return ApiResponse.ok(true);", "회원 저장이 성공하면 프론트에 성공 응답을 보냅니다."),
        ("210-214", "@PostMapping(\"/logout\")", "로그아웃 요청 시 session.invalidate()로 서버 세션을 삭제합니다."),
    ])

    doc.add_heading("7. AuthService - 로그인 로직", level=1)
    add_path(doc, "src/main/java/kopo/poly/service/impl/AuthService.java")
    add_line_table(doc, [
        ("39", "private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();", "비밀번호를 BCrypt 방식으로 암호화/검증하기 위한 객체입니다."),
        ("55", "public AuthenticatedUserDTO login(LoginRequestDTO req) {", "로그인 검증의 핵심 메서드입니다."),
        ("57", "String email = req == null ... ? \"\" : req.getEmail().trim();", "요청 객체나 이메일이 null이면 빈 문자열로 처리하고, 있으면 앞뒤 공백을 제거합니다."),
        ("58", "String password = req == null ... ? \"\" : req.getPassword();", "비밀번호도 null 방어 처리를 합니다. 비밀번호는 공백도 의미가 있을 수 있어 trim하지 않습니다."),
        ("60-62", "if (email.isBlank()) throw ...", "이메일이 비어 있으면 DB 조회 전에 사용자 입력 오류를 발생시킵니다."),
        ("63-65", "if (password.isBlank()) throw ...", "비밀번호가 비어 있으면 로그인 검증을 진행하지 않습니다."),
        ("67", "Map<String, Object> user = userMapper.selectUserByEmail(email);", "app_user 테이블에서 이메일로 회원을 조회합니다."),
        ("68-70", "if (user == null) throw invalidCredential();", "해당 이메일의 회원이 없으면 계정 정보 오류로 처리합니다."),
        ("73-80", "String storedPassword = firstNonBlank(...)", "DB 컬럼명이 환경별로 다르게 매핑될 수 있어 여러 후보 중 비어 있지 않은 비밀번호 해시를 찾습니다."),
        ("81-84", "if (storedPassword.isBlank()) ...", "DB에 비밀번호 해시가 없으면 정상 로그인할 수 없으므로 실패 처리합니다."),
        ("86-88", "if (!passwordMatches(password, storedPassword)) ...", "사용자가 입력한 비밀번호와 DB 해시가 맞지 않으면 실패 처리합니다."),
        ("90-92", "if (isLegacySha256(storedPassword)) updatePasswordByEmail(...)", "예전 SHA-256 해시로 로그인에 성공한 경우 BCrypt 해시로 자동 교체합니다."),
        ("94", "return new AuthenticatedUserDTO(user.get(\"id\"), ...);", "컨트롤러가 세션에 넣을 최소 사용자 정보 id/name/email을 반환합니다."),
    ])

    doc.add_heading("8. AuthService - 회원가입 로직", level=1)
    add_path(doc, "src/main/java/kopo/poly/service/impl/AuthService.java")
    add_line_table(doc, [
        ("203", "public EmailAvailabilityDTO checkEmail(SendCodeRequestDTO req) {", "이메일 중복 확인 서비스 메서드입니다."),
        ("204", "String email = normalizeEmail(req);", "이메일을 trim하고 소문자로 통일합니다."),
        ("205", "assertEmail(email);", "이메일이 비어 있는지 검사합니다."),
        ("206", "assertEmailNotRegistered(email);", "이미 가입된 이메일인지 DB에서 확인합니다."),
        ("207", "return new EmailAvailabilityDTO(true, email);", "사용 가능한 이메일이면 true와 정리된 이메일을 반환합니다."),
        ("211", "public void sendSignupCode(SendCodeRequestDTO req) {", "회원가입 인증번호 발송 서비스 메서드입니다."),
        ("212-214", "normalizeEmail / assertEmail / assertEmailNotRegistered", "발송 전에도 이메일 형식과 중복 여부를 서버에서 다시 검사합니다."),
        ("218", "response = emailAuthService.sendAuthCode(email);", "실제 인증번호 생성, DB 저장, 메일 발송은 EmailAuthService에 맡깁니다."),
        ("224-226", "if (!response.isSuccess()) throw ...", "메일 발송/저장 결과가 실패면 회원가입 흐름을 중단합니다."),
        ("231", "public void verifySignupCode(VerifyCodeRequestDTO req) {", "회원가입 인증번호 확인 서비스 메서드입니다."),
        ("232", "String email = ...trim().toLowerCase();", "검증할 이메일을 소문자로 통일합니다."),
        ("233", "String code = ...", "사용자가 입력한 인증번호를 꺼냅니다."),
        ("235-240", "email/code blank check", "이메일이나 인증번호가 비어 있으면 검증하지 않고 실패시킵니다."),
        ("242", "EmailAuthResponseDTO response = emailAuthService.verifyAuthCode(email, code);", "인증번호 실제 비교를 EmailAuthService에 맡깁니다."),
        ("243-245", "if (!response.isSuccess()) throw ...", "인증 실패 메시지를 AuthServiceException으로 변환해 컨트롤러가 응답하게 합니다."),
        ("249", "String email = ...trim().toLowerCase();", "최종 가입할 이메일을 정규화합니다."),
        ("251-265", "필수값 검증", "이메일, 비밀번호 길이, 이름, 전화번호, 주소를 서버에서 다시 검증합니다."),
        ("266-268", "if (!emailAuthService.isEmailVerified(email)) ...", "이메일 인증이 완료되지 않았으면 DB 저장을 막습니다."),
        ("270", "assertEmailNotRegistered(email);", "가입 직전에도 이메일 중복 여부를 다시 검사합니다."),
        ("271", "String address = buildFullAddress(...);", "우편번호, 기본주소, 상세주소를 하나의 주소 문자열로 합칩니다."),
        ("272", "userMapper.insertUser(... PASSWORD_ENCODER.encode(req.getPassword()) ...);", "비밀번호를 BCrypt 해시로 바꾼 뒤 app_user 테이블에 저장합니다."),
        ("273", "emailAuthService.expireEmailVerification(email);", "회원가입 완료 후 인증번호를 재사용하지 못하게 만료 처리합니다."),
    ])

    doc.add_heading("8-1. completeSignup() 상세 한 줄 설명", level=1)
    add_path(doc, "src/main/java/kopo/poly/service/impl/AuthService.java")
    add_line_table(doc, [
        ("254", "@Override", "IAuthService 인터페이스에 선언된 completeSignup()을 실제로 구현한다는 뜻입니다."),
        ("255", "@Transactional", "회원 저장과 인증 만료 처리를 하나의 트랜잭션으로 묶습니다. 중간 오류가 나면 DB 작업을 되돌릴 수 있습니다."),
        ("256", "public void completeSignup(SignupCompleteRequestDTO req) {", "최종 회원가입 처리 메서드입니다. signup.jsp가 보낸 최종 가입 데이터가 req에 담겨 들어옵니다."),
        ("257", "// 이메일 인증이 완료된 사용자만 최종 회원 정보를 저장한다.", "이 메서드의 목적을 설명하는 주석입니다. 인증을 완료한 사용자만 DB에 저장한다는 의미입니다."),
        ("258", "String email = req == null || req.getEmail() == null ? \"\" : req.getEmail().trim().toLowerCase();", "요청 또는 이메일이 null이면 빈 값으로 처리하고, 정상 이메일이면 공백 제거 후 소문자로 통일합니다."),
        ("260-262", "if (email.isBlank()) throw ... AU-4001", "이메일이 비어 있으면 회원가입을 중단하고 컨트롤러로 에러를 올립니다."),
        ("263-265", "if (req == null || req.getPassword() == null || req.getPassword().length() < 6) throw ...", "요청 자체가 없거나 비밀번호가 없거나 6자 미만이면 회원가입을 막습니다."),
        ("266-268", "if (req.getName() == null || req.getName().isBlank()) throw ...", "이름이 null이거나 공백이면 이름 입력 오류를 발생시킵니다."),
        ("269-271", "if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) throw ...", "전화번호가 비어 있으면 회원가입을 막습니다."),
        ("272-274", "if (req.getAddress() == null || req.getAddress().isBlank()) throw ...", "주소가 비어 있으면 회원가입을 막습니다."),
        ("275-277", "if (!emailAuthService.isEmailVerified(email)) throw ...", "email_auth 테이블 기준으로 인증 완료 상태인지 확인합니다. 인증 전이면 최종 저장을 막습니다."),
        ("279", "assertEmailNotRegistered(email);", "최종 저장 직전에 app_user 테이블에 같은 이메일이 있는지 다시 확인합니다."),
        ("281", "String address = buildFullAddress(req.getAddress(), req.getDetailAddress(), req.getZonecode());", "기본주소, 상세주소, 우편번호를 하나의 주소 문자열로 합칩니다."),
        ("282", "// 비밀번호는 평문으로 저장하지 않고 BCrypt 해시로 변환해 저장한다.", "DB에 원문 비밀번호를 저장하지 않는다는 보안 목적의 주석입니다."),
        ("283", "UserCreateDTO userCreateDTO = new UserCreateDTO(", "DB 저장에 필요한 회원 정보를 담을 DTO 객체 생성을 시작합니다."),
        ("284", "req.getName(),", "회원 이름을 DTO에 넣습니다."),
        ("285", "email,", "정리된 이메일을 DTO에 넣습니다."),
        ("286", "PASSWORD_ENCODER.encode(req.getPassword()),", "사용자가 입력한 비밀번호를 BCrypt 해시로 변환해서 DTO에 넣습니다."),
        ("287", "req.getPhoneNumber(),", "전화번호를 DTO에 넣습니다."),
        ("288", "address", "합쳐진 최종 주소 문자열을 DTO에 넣습니다."),
        ("289", ");", "UserCreateDTO 객체 생성이 끝납니다."),
        ("290", "userMapper.insertUser(userCreateDTO);", "IUserMapper를 통해 UserMapper.xml의 INSERT SQL을 실행하고 app_user 테이블에 회원을 저장합니다."),
        ("291", "emailAuthService.expireEmailVerification(email);", "회원가입이 끝났으므로 같은 인증번호를 재사용하지 못하게 email_auth 인증 상태를 만료 처리합니다."),
        ("292", "}", "completeSignup() 메서드가 끝납니다."),
    ])

    doc.add_heading("9. AuthService - 비밀번호 비교 보조 메서드", level=1)
    add_path(doc, "src/main/java/kopo/poly/service/impl/AuthService.java")
    add_line_table(doc, [
        ("384", "private boolean passwordMatches(String rawPassword, String storedPassword) {", "사용자 입력 비밀번호와 DB 저장 해시를 비교하는 메서드입니다."),
        ("385", "if (storedPassword.startsWith(\"$2a$\") ...)", "BCrypt 해시는 보통 $2a$, $2b$, $2y$로 시작하므로 BCrypt 여부를 판단합니다."),
        ("386", "return PASSWORD_ENCODER.matches(rawPassword, storedPassword);", "BCrypt 방식이면 원문과 해시를 BCrypt 검증 함수로 비교합니다."),
        ("388-390", "return isLegacySha256(...) && HashUtil.sha256(...)", "예전 SHA-256 해시라면 입력 비밀번호를 SHA-256으로 바꿔 DB 값과 비교합니다."),
        ("392", "throw new IllegalStateException(...)", "해시 처리 중 예외가 나면 서버 내부 오류로 올립니다."),
        ("395", "return storedPassword != null && storedPassword.matches(\"(?i)^[0-9a-f]{64}$\");", "64자리 16진수 문자열이면 예전 SHA-256 해시로 판단합니다."),
        ("398-402", "assertLoggedIn(Long userId)", "세션에 USER_ID가 없으면 로그인 필요 오류를 발생시킵니다."),
        ("404-406", "normalizeEmail(SendCodeRequestDTO req)", "이메일을 null-safe하게 꺼내고 소문자로 통일합니다."),
        ("408-412", "assertEmail(String email)", "이메일이 빈 값이면 공통 에러 코드 AU-4001을 발생시킵니다."),
        ("414-419", "assertEmailNotRegistered(String email)", "userMapper.existsByEmail() 결과가 1 이상이면 이미 가입된 이메일로 처리합니다."),
        ("421-423", "invalidCredential()", "아이디 없음과 비밀번호 틀림을 같은 메시지로 처리해 계정 존재 여부 노출을 줄입니다."),
    ])

    doc.add_heading("10. EmailAuthService - 인증번호 발송", level=1)
    add_path(doc, "src/main/java/kopo/poly/service/impl/EmailAuthService.java")
    add_line_table(doc, [
        ("36", "private static final SecureRandom SECURE_RANDOM = new SecureRandom();", "예측하기 어려운 인증번호 생성을 위해 SecureRandom을 사용합니다."),
        ("39-40", "DateTimeFormatter.ofPattern(\"yyyy-MM-dd HH:mm:ss\")", "DB에 저장할 만료 시간을 문자열 형식으로 맞춥니다."),
        ("41-43", "MAX_SENDS / MAX_VERIFY_FAILURES / RATE_LIMIT_WINDOW", "10분 동안 발송/실패 횟수를 제한하기 위한 상수입니다."),
        ("52", "public EmailAuthResponseDTO sendAuthCode(String email) {", "인증번호 발송 전체 흐름을 처리하는 메서드입니다."),
        ("53", "String normalizedEmail = normalizeEmail(email);", "이메일을 trim하고 소문자로 바꿉니다."),
        ("54", "validateEmail(normalizedEmail);", "이메일이 비었거나 형식이 잘못됐는지 검사합니다."),
        ("55-56", "enforceRateLimit(...)", "짧은 시간에 너무 많이 인증번호를 요청하지 못하게 막습니다."),
        ("58", "String authCode = generateAuthCode();", "6자리 인증번호를 생성합니다."),
        ("59", "String expiresAt = LocalDateTime.now().plusMinutes(5)...", "인증번호 만료 시간을 현재 시각 기준 5분 뒤로 설정합니다."),
        ("62", "emailAuthRepository.expireAllByEmail(normalizedEmail);", "같은 이메일의 이전 인증번호를 무효화해 최신 코드만 유효하게 합니다."),
        ("64-68", "EmailAuthEntity entity = new EmailAuthEntity(); ...", "DB에 저장할 이메일, 인증번호, 검증상태 N, 만료시간을 엔티티에 담습니다."),
        ("69", "emailAuthRepository.insertEmailAuth(entity);", "email_auth 테이블에 인증번호 정보를 INSERT합니다."),
        ("71", "sendAuthCodeMail(normalizedEmail, authCode);", "실제 메일 발송 서비스를 호출합니다."),
        ("73-76", "EmailAuthResponseDTO response = ...", "프론트에 보낼 성공 여부, 이메일, verified=N 상태를 응답 객체에 담습니다."),
    ])

    doc.add_heading("11. EmailAuthService - 인증번호 검증", level=1)
    add_path(doc, "src/main/java/kopo/poly/service/impl/EmailAuthService.java")
    add_line_table(doc, [
        ("79", "public EmailAuthResponseDTO verifyAuthCode(String email, String authCode) {", "사용자가 입력한 인증번호가 맞는지 확인하는 메서드입니다."),
        ("80", "String normalizedEmail = normalizeEmail(email);", "이메일을 DB 저장 기준과 맞게 정리합니다."),
        ("82-87", "email/authCode blank check", "이메일 또는 인증번호가 비어 있으면 실패 응답을 반환합니다."),
        ("89", "EmailAuthEntity latest = emailAuthRepository.selectLatestByEmail(normalizedEmail);", "해당 이메일의 가장 최근 인증번호 기록을 DB에서 가져옵니다."),
        ("90-92", "if (latest == null) ...", "인증번호 발급 기록이 없으면 검증할 수 없으므로 실패합니다."),
        ("95", "LocalDateTime expiresAt = parseDateTime(latest.getExpiresAt());", "DB에서 가져온 만료시간 문자열을 LocalDateTime으로 변환합니다."),
        ("96-98", "if (LocalDateTime.now().isAfter(expiresAt)) ...", "현재 시간이 만료시간 이후면 인증 실패입니다."),
        ("100", "if (!authCode.equals(latest.getAuthCode())) {", "입력 코드와 DB의 최신 코드가 같은지 비교합니다."),
        ("101-103", "verify failure rate limit", "인증번호 틀린 횟수가 너무 많으면 잠시 차단합니다."),
        ("104", "return new EmailAuthResponseDTO(false, ...)", "코드가 다르면 실패 메시지를 반환합니다."),
        ("107", "emailAuthRepository.markVerified(latest.getId());", "코드가 맞으면 해당 인증 기록의 verified 값을 Y로 변경합니다."),
        ("108", "verifyFailureWindows.remove(normalizedEmail);", "성공했으므로 실패 횟수 기록을 지웁니다."),
        ("110-113", "response.setVerified(\"Y\")", "프론트에 인증 완료 상태 Y를 반환합니다."),
        ("116", "public boolean isEmailVerified(String email) {", "최종 회원가입 전에 이메일 인증이 아직 유효한지 확인합니다."),
        ("128-132", "verified and expiresAt check", "verified가 Y이고 만료되지 않았을 때만 true를 반환합니다."),
        ("136-141", "expireEmailVerification(String email)", "회원가입 완료 후 인증번호를 다시 못 쓰게 verified=N으로 만료 처리합니다."),
    ])

    doc.add_heading("12. 회원 DB Mapper", level=1)
    add_path(doc, "src/main/resources/mapper/UserMapper.xml")
    add_line_table(doc, [
        ("13-15", "<select id=\"existsByEmail\" resultType=\"int\">", "이메일 중복 확인용 SQL입니다. app_user에서 같은 이메일 개수를 셉니다."),
        ("18-21", "<insert id=\"insertUser\">", "일반 회원가입 최종 저장 SQL입니다."),
        ("19", "INSERT INTO app_user(name, email, password_hash, phone_number, address)", "회원 이름, 이메일, 비밀번호 해시, 전화번호, 주소를 저장할 컬럼입니다."),
        ("20", "VALUES (#{name}, #{email}, #{passwordHash}, #{phoneNumber}, #{address})", "Mapper 메서드 파라미터 값들이 SQL에 바인딩됩니다."),
        ("30-37", "<select id=\"selectUserByEmail\" resultType=\"map\">", "로그인할 때 이메일로 회원 정보를 조회합니다."),
        ("31-33", "SELECT id, name, email, password_hash AS passwordHash, ...", "서비스에서 쓰기 쉽게 DB 컬럼명을 Java 쪽 이름으로 alias 처리합니다."),
        ("35", "WHERE email = #{email}", "입력 이메일과 같은 회원만 조회합니다."),
        ("86-89", "<update id=\"updatePasswordByEmail\">", "비밀번호 재설정 또는 SHA-256 -> BCrypt 자동 전환 때 password_hash를 갱신합니다."),
        ("153-156", "<delete id=\"deleteUserById\">", "회원탈퇴 마지막 단계에서 app_user 본문을 삭제합니다."),
    ])

    doc.add_heading("13. 이메일 인증 DB Mapper", level=1)
    add_path(doc, "src/main/resources/mapper/EmailAuthMapper.xml")
    add_line_table(doc, [
        ("12-30", "<insert id=\"insertEmailAuth\">", "새 인증번호를 email_auth 테이블에 저장합니다."),
        ("17-23", "email, auth_code, verified, expires_at, updated_at", "저장할 값은 이메일, 인증번호, 검증 여부, 만료 시간, 갱신 시간입니다."),
        ("32-46", "<select id=\"selectLatestByEmail\">", "인증번호 검증 시 해당 이메일의 가장 최근 기록을 가져옵니다."),
        ("43", "ORDER BY id DESC", "가장 나중에 발급된 인증번호가 먼저 오도록 정렬합니다."),
        ("44", "LIMIT 1", "최신 인증번호 한 건만 사용합니다."),
        ("48-53", "<update id=\"expireAllByEmail\">", "같은 이메일의 인증 상태를 N으로 바꿔 기존 코드를 무효화합니다."),
        ("55-60", "<update id=\"markVerified\">", "인증번호가 맞으면 verified를 Y로 바꿉니다."),
    ])

    doc.add_heading("14. 세션 유틸", level=1)
    add_path(doc, "src/main/java/kopo/poly/util/SessionUtil.java")
    add_line_table(doc, [
        ("20", "public static Long getUserId(HttpSession session) {", "세션에서 현재 로그인한 사용자 ID를 꺼내는 공통 메서드입니다."),
        ("21", "Object userId = session.getAttribute(\"USER_ID\");", "AuthApiController.login()에서 저장한 USER_ID 값을 읽습니다."),
        ("22-24", "if (userId == null) return null;", "로그인하지 않은 사용자는 USER_ID가 없으므로 null을 반환합니다."),
        ("26-28", "if (userId instanceof Number number) ...", "세션 값이 숫자 타입이면 Long으로 변환합니다."),
        ("31", "return Long.parseLong(String.valueOf(userId));", "문자열로 저장된 경우에도 Long으로 변환합니다."),
        ("32-34", "catch (NumberFormatException e) return null;", "숫자로 바꿀 수 없는 값이면 로그인 사용자로 인정하지 않습니다."),
    ])

    doc.add_heading("15. 발표 때 그대로 말할 핵심 문장", level=1)
    add_bullet(doc, "회원가입은 check-email, send-code, verify-code, complete 4단계로 나눴습니다.")
    add_bullet(doc, "프론트 JSP는 fetch로 JSON 요청을 보내고, 서버는 AuthApiController에서 요청을 받습니다.")
    add_bullet(doc, "AuthService는 입력값 검증, 중복 확인, 비밀번호 BCrypt 해시, 회원 저장을 담당합니다.")
    add_bullet(doc, "EmailAuthService는 인증번호 생성, email_auth 저장, 메일 발송, 만료 확인, verified=Y 처리를 담당합니다.")
    add_bullet(doc, "로그인은 이메일로 app_user를 조회한 뒤 BCryptPasswordEncoder.matches()로 비밀번호를 비교합니다.")
    add_bullet(doc, "로그인 성공 시 JWT를 만들지 않고 HttpSession에 USER_ID, USER_NAME, USER_EMAIL을 저장합니다.")
    add_bullet(doc, "request.changeSessionId()를 호출해 로그인 성공 시 세션 ID를 교체하므로 세션 고정 공격을 줄입니다.")

    doc.save(OUT)


if __name__ == "__main__":
    build()
