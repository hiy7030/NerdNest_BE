package com.server.domain.blog.controller;

import com.server.domain.blog.dto.BlogDto;
import com.server.domain.blog.dto.BlogResponseDto;
import com.server.domain.blog.entity.Blog;
import com.server.domain.blog.mapper.BlogMapper;
import com.server.domain.blog.service.BlogService;
import com.server.domain.category.entity.Category;
import com.server.domain.category.service.CategoryService;
import com.server.domain.comment.dto.CommentResponseDto;
import com.server.domain.comment.entity.Comment;
import com.server.domain.comment.service.CommentService;
import com.server.domain.member.entity.Member;
import com.server.domain.member.service.MemberService;
import com.server.response.ListResponseDto;
import com.server.response.MultiResponseDto;
import com.server.response.SingleResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;
    private final BlogMapper mapper;
    private final CategoryService categoryService;
    private final MemberService memberService;
    private final CommentService commentService;

    @PostMapping("/blogs")
    public ResponseEntity<HttpStatus> postBlog(@RequestBody @Valid BlogDto.Post blogPostDto,
                                               @AuthenticationPrincipal Member loginMember) {
        if (loginMember == null) {
            log.error("loginMember is null : 허용되지 않은 접근입니다.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        //TODO: Comment 추가예정
        Member foundMember = memberService.findMember(loginMember.getMemberId());
        Category category = categoryService.findSingleCategoryById(blogPostDto.getCategoryId());
        Blog blog = mapper.blogPostDtoToBlog(blogPostDto, category, foundMember);
        blogService.createBlog(blog);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/blogs/edit/{blog-id}")
    public ResponseEntity<HttpStatus> patchBlog(@RequestBody @Valid BlogDto.Patch blogPatchDto,
                                                @PathVariable("blog-id") @Positive long blogId,
                                                @AuthenticationPrincipal Member loginMember) {
        if (loginMember == null) {
            log.error("loginMember is null : 허용되지 않은 접근입니다.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        blogService.verifyOwner(blogId, loginMember);
        blogPatchDto.setBlogId(blogId);
        Category singleCategory = categoryService.findSingleCategoryByName(blogPatchDto.getCategoryName());
        Blog blog = mapper.blogPatchDtoToBlog(blogPatchDto, singleCategory);
        blogService.editBlog(blog);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/home/blogs")
    public ResponseEntity<ListResponseDto<?>> getBlogHomeData(@RequestParam(defaultValue = "newest", required = false) String tab,
                                                              @RequestParam(defaultValue = "1", required = false) int page,
                                                              @RequestParam(defaultValue = "12", required = false) int size) {
        //TODO : tab에 따른 likes 정렬기능 추가예정
        Page<Blog> blogsPageInfo = blogService.findAllBlog(switchTabToSort(tab), page, size);
        List<Blog> blogs = blogsPageInfo.getContent();
        List<BlogResponseDto.Home> blogResponseHomeDto = mapper.blogListToBlogResponseHomeDto(blogs);

        return new ResponseEntity<>(new ListResponseDto<>(blogResponseHomeDto), HttpStatus.OK);
    }

    private static String switchTabToSort(String tab) {
        String sort = "";
        switch (tab) {
            case "newest":
                sort = "blogId";
                break;
            case "likes":
                sort = "likes";
                break;
        }
        return sort;
    }

    @GetMapping("/home/blogs/mylikes")
    public ResponseEntity getBlogHomeDataByMyLikes(@RequestParam(defaultValue = "1", required = false) int page,
                                                   @RequestParam(defaultValue = "12", required = false) int size,
                                                   @AuthenticationPrincipal Member loginMember) {

        if (loginMember == null) {
            log.error("loginMember is null : 허용되지 않은 접근입니다.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Member member = memberService.findMember(loginMember.getMemberId());

        Page<Blog> blogPage = blogService.findBlogsByMemberWithLike(member, page, size);

        List<Blog> blogs = blogPage.getContent();
        List<BlogResponseDto.Home> blogResponseHomeDto = mapper.blogListToBlogResponseHomeDto(blogs);

        return new ResponseEntity<>(new MultiResponseDto.BlogList<>(blogResponseHomeDto, blogPage), HttpStatus.OK);
    }

    @GetMapping("/blogs/{blog-id}")
    public ResponseEntity<SingleResponseDto<?>> getBlogById(@PathVariable("blog-id") @Positive long blogId) {
        //TODO: Comment 추가 필요
        Blog blog = blogService.findBlogById(blogId);
        List<CommentResponseDto> layeredComments = commentService.getLayeredComments(blog);
        BlogResponseDto.WithComment blogResponseDtoWithComment = mapper.blogListToBlogResponseDtoWithComment(blog, layeredComments);

        return ResponseEntity.ok(new SingleResponseDto<>(blogResponseDtoWithComment));
    }

    @GetMapping("/blogs/category/{category-id}")
    public ResponseEntity<?> getBlogsByCategoryName(@PathVariable("category-id") long categoryId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "12") int size) {
        Page<Blog> blogsPageInfo = blogService.findBlogsByCategoryId(categoryId, page, size);
        List<Blog> blogs = blogsPageInfo.getContent();
        List<BlogResponseDto.WithCategory> blogResponseDtoWithCategory = mapper.blogListToBlogResponseDtoWithCategory(blogs);

        return new ResponseEntity<>(new MultiResponseDto.BlogList<>(blogResponseDtoWithCategory, blogsPageInfo), HttpStatus.OK);
    }

    // 개인 블로그 데이터 전체 게시글 조회
    @GetMapping("/blogs/all")
    public ResponseEntity<?> getBlogsByNickname(@RequestParam @NotBlank String nickname,
                                                @RequestParam(required = false, defaultValue = "1") int page,
                                                @RequestParam(required = false, defaultValue = "12") int size) {

        Page<Blog> blogsPageInfo = blogService.findBlogsByMemberNickname(nickname, page, size);
        List<Blog> blogs = blogsPageInfo.getContent();
        List<BlogResponseDto.WithCategory> blogResponseDtoWithCategory = mapper.blogListToBlogResponseDtoWithCategory(blogs);

        return new ResponseEntity<>(new MultiResponseDto.BlogList<>(blogResponseDtoWithCategory, blogsPageInfo), HttpStatus.OK);
    }

    @GetMapping("/blogs/edit/{blog-id}")
    public ResponseEntity<SingleResponseDto<?>> getBlog(@PathVariable("blog-id") @Positive long blogId) {
        Blog blog = blogService.findBlogById(blogId);
        BlogResponseDto blogResponseDto = mapper.blogToBlogResponseDto(blog);

        return ResponseEntity.ok(new SingleResponseDto<>(blogResponseDto));
    }

    @DeleteMapping("/blogs/{blog-id}")
    public ResponseEntity<HttpStatus> deleteBlog(@PathVariable("blog-id") @Positive long blogId,
                                                 @AuthenticationPrincipal Member loginMember) {
        if (loginMember == null) {
            log.error("loginMember is null : 허용되지 않은 접근입니다.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        blogService.verifyOwner(blogId, loginMember);
        blogService.deleteBlog(blogId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/search")
    public ResponseEntity searchBlog(@RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "12") int size) {
        Page<Blog> pageBlogs = blogService.searchBlog(keyword, page, size);
        List<Blog> blogs = pageBlogs.getContent();
        List<BlogResponseDto.Home> responses = mapper.blogListToBlogResponseHomeDto(blogs);

        return new ResponseEntity<>(new MultiResponseDto.BlogList<>(responses, pageBlogs), HttpStatus.OK);
    }
}
