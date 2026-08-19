package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(커뮤니티): 게시글 목록, 검색/정렬, 작성, 상세 화면 이동을 담당한다.
 */
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 커뮤니티 목록과 글쓰기 화면을 반환하는 페이지 컨트롤러다.
 */
@Controller
public class CommunityController {

    @GetMapping("/community")
    public String community() {
        return "community";
    }

    @GetMapping("/community/create")
    public String createPost(HttpSession session) {
        if (session.getAttribute("USER_ID") == null) {
            return "redirect:/login";
        }
        return "create-post";
    }

    @GetMapping("/community/{id}")
    public String communityDetail(@PathVariable("id") String id) {
        return "post-detail";
    }
}
