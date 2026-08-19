package kopo.poly.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OriginProtectionFilter extends OncePerRequestFilter {

    // application.properties의 app.security.allowed-origins에 등록된 외부 허용 출처 목록이다.
    private final Set<String> allowedOrigins;

    public OriginProtectionFilter(@Value("${app.security.allowed-origins:}") String allowedOrigins) {
        // 쉼표로 구분된 설정값을 공백 제거, 빈 값 제외, origin 정규화 순서로 처리한다.
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(OriginProtectionFilter::normalizeOrigin)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 조회성 요청은 서버 상태를 바꾸지 않으므로 Origin 검증 없이 그대로 통과시킨다.
        if (!isStateChanging(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 브라우저가 보낸 Origin을 우선 사용하고, 없으면 Referer에서 출처만 추출한다.
        String requestOrigin = firstText(request.getHeader("Origin"), originFromReferer(request.getHeader("Referer")));
        // Origin/Referer가 없거나 허용된 출처이면 요청을 계속 진행한다.
        if (!StringUtils.hasText(requestOrigin) || isAllowedOrigin(requestOrigin, request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 상태 변경 요청이 다른 출처에서 온 것으로 판단되면 CSRF 방어 목적으로 차단한다.
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "허용되지 않은 외부 출처 요청입니다.");
    }

    // 요청 출처가 현재 서비스 출처와 같거나, 설정으로 허용한 출처 목록에 포함되는지 확인한다.
    private boolean isAllowedOrigin(String origin, HttpServletRequest request) {
        String normalized = normalizeOrigin(origin);
        if (allowedOrigins.contains(normalized)) {
            return true;
        }
        return allowedOrigins.isEmpty() && isSameHostOrigin(normalized, request);
    }

    // 서버 상태를 바꿀 수 있는 HTTP 메서드만 Origin 검증 대상으로 삼는다.
    private static boolean isStateChanging(String method) {
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
    }

    // 브라우저가 보낸 Host 헤더와 요청 origin의 host만 비교한다. X-Forwarded-* 값은 CSRF 판단에 쓰지 않는다.
    private static boolean isSameHostOrigin(String normalizedOrigin, HttpServletRequest request) {
        String requestHost = normalizeHost(firstText(request.getHeader("Host"), request.getServerName()));
        String originHost = normalizeHost(hostFromOrigin(normalizedOrigin));
        return StringUtils.hasText(requestHost) && requestHost.equals(originHost);
    }

    private static String hostFromOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return "";
        }
        try {
            return URI.create(origin).getAuthority();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String normalizeHost(String host) {
        return StringUtils.hasText(host) ? host.trim().toLowerCase(Locale.ROOT) : "";
    }

    // Referer 전체 URL에서 scheme, host, port만 분리해 Origin 형식으로 변환한다.
    private static String originFromReferer(String referer) {
        if (!StringUtils.hasText(referer)) {
            return "";
        }
        try {
            URI uri = URI.create(referer);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
                return "";
            }
            int port = uri.getPort();
            return scheme + "://" + host + (port > -1 ? ":" + port : "");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    // origin 비교가 흔들리지 않도록 대소문자, 끝 슬래시, 기본 포트 표기를 정리한다.
    private static String normalizeOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return "";
        }
        try {
            URI uri = URI.create(origin.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
                return origin.trim().toLowerCase(Locale.ROOT).replaceAll("/+$", "");
            }
            int port = uri.getPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
            return scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT)
                    + (port > -1 && !defaultPort ? ":" + port : "");
        } catch (IllegalArgumentException e) {
            return origin.trim().toLowerCase(Locale.ROOT).replaceAll("/+$", "");
        }
    }

    // 여러 후보 문자열 중 실제 텍스트가 있는 첫 번째 값을 반환한다.
    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
