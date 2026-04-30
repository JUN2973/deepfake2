package kopo.poly.controller;

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
    public String createPost() {
        return "create-post";
    }

    @GetMapping("/community/{id}")
    public String communityDetail(@PathVariable("id") String id) {
        return "post-detail";
    }
}