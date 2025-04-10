package ec.gob.conagopare.sona.modules.forum;


import com.jayway.jsonpath.JsonPath;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
class ForumIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static boolean isSetUp = false;

    private static final String POSTER_USERNAME = "poster";
    private static final String POSTER_PASSWORD = "Qwerty15981598.";

    private static final String POSTER2_USERNAME = "poster2";
    private static final String POSTER2_PASSWORD = "Qwerty15981598.";

    private static final String LIKER_USERNAME = "liker";
    private static final String LIKER_PASSWORD = "Liker123.1598.";

    private static final String REPORTER_USERNAME = "reporter";
    private static final String REPORTER_PASSWORD = "Reporter123.1598.";

    private static final String COMMENTER_USERNAME = "commenter";
    private static final String COMMENTER_PASSWORD = "Commenter123.1598.";


    @BeforeEach
    void setUp() throws Exception {
        if (isSetUp) return;

        var singUpUser = new SingUpUser();
        singUpUser.setFirstName("Susana");
        singUpUser.setLastName("Basurto");
        singUpUser.setUsername(POSTER_USERNAME);
        singUpUser.setPassword(POSTER_PASSWORD);
        singUpUser.setEmail("susana.basruto@test.com");

        singUp(singUpUser, mockMvc);

        var singUpUser2 = new SingUpUser();
        singUpUser2.setFirstName("Susana");
        singUpUser2.setLastName("Basurto");
        singUpUser2.setUsername(POSTER2_USERNAME);
        singUpUser2.setPassword(POSTER2_PASSWORD);
        singUpUser2.setEmail("poster@test.com");

        singUp(singUpUser2, mockMvc);

        var singUpUser3 = new SingUpUser();
        singUpUser3.setFirstName("Peroncho");
        singUpUser3.setLastName("Perez");
        singUpUser3.setUsername(LIKER_USERNAME);
        singUpUser3.setPassword(LIKER_PASSWORD);
        singUpUser3.setEmail("perconcho1599@aa.com");

        singUp(singUpUser3, mockMvc);

        var singUpUser4 = new SingUpUser();
        singUpUser4.setFirstName("Reporter");
        singUpUser4.setLastName("Reporter");
        singUpUser4.setUsername(REPORTER_USERNAME);
        singUpUser4.setPassword(REPORTER_PASSWORD);
        singUpUser4.setEmail("reporter@aaa.com");

        singUp(singUpUser4, mockMvc);

        var singUpUser5 = new SingUpUser();
        singUpUser5.setFirstName("Commenter");
        singUpUser5.setLastName("Commenter");
        singUpUser5.setUsername(COMMENTER_USERNAME);
        singUpUser5.setPassword(COMMENTER_PASSWORD);
        singUpUser5.setEmail("comenter@aaa.com");

        singUp(singUpUser5, mockMvc);
        isSetUp = true;
    }

    @Test
    void createPostAndFindPost() throws Exception {

        var post = new PostDto();
        post.setAnonymous(false);
        post.setContent("Post to find");

        var accessToken = "Bearer " + obtainAccessToken(POSTER_USERNAME, POSTER_PASSWORD);

        var postId = expectedCreatePost(post, accessToken);

        var findPost = mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("Post: {}", findPost);
    }

    @Test
    void createPostAndDeletePost() throws Exception {

        var post = new PostDto();
        post.setAnonymous(false);
        post.setContent("Post to delete");

        var accessToken = "Bearer " + obtainAccessToken(POSTER_USERNAME, POSTER_PASSWORD);

        var postId = expectedCreatePost(post, accessToken);

        mockMvc.perform(delete("/forum/post/{postId}", postId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPostsAndSearchPosts() throws Exception {

        var post1 = new PostDto();
        post1.setAnonymous(false);
        post1.setContent("Post to search 1");

        var post2 = new PostDto();
        post2.setAnonymous(false);
        post2.setContent("Post to search 2");

        var post3 = new PostDto();
        post3.setAnonymous(false);
        post3.setContent("Post to search 3");

        var poster1accessToken = "Bearer " + obtainAccessToken(POSTER2_USERNAME, POSTER2_PASSWORD);
        var poster2accessToken = "Bearer " + obtainAccessToken(LIKER_USERNAME, LIKER_PASSWORD);

        expectedCreatePost(post1, poster1accessToken);
        expectedCreatePost(post2, poster1accessToken);
        expectedCreatePost(post2, poster2accessToken);

        mockMvc.perform(get("/forum/post")
                        .header("Authorization", poster1accessToken)
                        .param("search", "search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(3));

        mockMvc.perform(get("/forum/post")
                        .header("Authorization", poster1accessToken)
                        .param("search", "search 1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(1));

        var profileJson = mockMvc.perform(get("/user/profile")
                        .header("Authorization", poster1accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var profileId = JsonPath.read(profileJson, "$.id").toString();

        mockMvc.perform(get("/forum/post")
                        .header("Authorization", poster1accessToken)
                        .param("author", profileId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2));

    }

    @Test
    void likePostAndUnlikePost() throws Exception {

        var post = new PostDto();
        post.setAnonymous(false);
        post.setContent("Post to like and unlike");

        var posterAccessToken = "Bearer " + obtainAccessToken(POSTER_USERNAME, POSTER_PASSWORD);

        var postId = expectedCreatePost(post, posterAccessToken);


        var likerAccessToken = "Bearer " + obtainAccessToken(LIKER_USERNAME, LIKER_PASSWORD);

        expectedLikePost(postId, likerAccessToken);

        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedBy.size()").value(1));

        expectedUnLikePost(postId, likerAccessToken);

        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedBy.size()").value(0));

    }

    @Test
    void reportPost() throws Exception {

        var post = new PostDto();
        post.setAnonymous(false);
        post.setContent("Post to report");

        var posterAccessToken = "Bearer " + obtainAccessToken(POSTER_USERNAME, POSTER_PASSWORD);

        var postId = expectedCreatePost(post, posterAccessToken);

        var reporterAccessToken = "Bearer " + obtainAccessToken(REPORTER_USERNAME, REPORTER_PASSWORD);

        mockMvc.perform(post("/forum/post/{postId}/report", postId)
                        .header("Authorization", reporterAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedBy.size()").value(1));

    }

    @Test
    void interactWithComments() throws Exception {
        
        var post = new PostDto();
        post.setAnonymous(false);
        post.setContent("Post to comment");

        var comment1 = new NewComment();
        comment1.setAnonymous(false);
        comment1.setContent("Comment 1");

        var comment2 = new NewComment();
        comment2.setAnonymous(false);
        comment2.setContent("Comment 2");

        var posterAccessToken = "Bearer " + obtainAccessToken(POSTER_USERNAME, POSTER_PASSWORD);
        var commenterAccessToken = "Bearer " + obtainAccessToken(COMMENTER_USERNAME, COMMENTER_PASSWORD);

        // Se crea el post
        var postId = expectedCreatePost(post, posterAccessToken);

        // Se comentan los post por el usuario commenter
        expectedCommentPost(comment2, postId, commenterAccessToken);
        expectedCommentPost(comment1, postId, commenterAccessToken);

        // Se verifica que los comentarios se hayan creado
        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.size()").value(2));

        // Se verifica que los comentarios se hayan creado a traves del endpoint de comentarios
        var commentsJson = mockMvc.perform(get("/forum/post/{postId}/comments", postId)
                        .header("Authorization", commenterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2))
                .andExpect(jsonPath("$.content[0].content").value("Comment 2"))
                .andExpect(jsonPath("$.content[1].content").value("Comment 1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var comment1Id = JsonPath.read(commentsJson, "$.content[0].id");
        var comment2Id = JsonPath.read(commentsJson, "$.content[1].id");

        // Se elimina un comentario
        mockMvc.perform(delete("/forum/post/{postId}/comments/{commentId}", postId, comment1Id)
                        .header("Authorization", commenterAccessToken))
                .andExpect(status().isOk());

        // Se verifica que el comentario se haya eliminado
        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.size()").value(1));

        // Se likea el comentario restante
        mockMvc.perform(post("/forum/post/{postId}/comments/{commentId}/like", postId, comment2Id)
                        .header("Authorization", commenterAccessToken))
                .andExpect(status().isOk());

        // Se verifica que el comentario se halla likeado
        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].likedBy.size()").value(1));

        // Se unlikea el comentario restante
        mockMvc.perform(post("/forum/post/{postId}/comments/{commentId}/unlike", postId, comment2Id)
                        .header("Authorization", commenterAccessToken))
                .andExpect(status().isOk());

        // Se verifica que el comentario se halla unlikeado
        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].likedBy.size()").value(0));

        // Se reporta el comentario restante
        mockMvc.perform(post("/forum/post/{postId}/comments/{commentId}/report", postId, comment2Id)
                        .header("Authorization", commenterAccessToken))
                .andExpect(status().isOk());

        // Se verifica que el comentario se halla reportado
        mockMvc.perform(get("/forum/post/{postId}", postId)
                        .header("Authorization", posterAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].reportedBy.size()").value(1));

    }

    @Test
    void topPosts() throws Exception {

        var postThatHaveMoreLikes = new PostDto();
        postThatHaveMoreLikes.setAnonymous(true);
        postThatHaveMoreLikes.setContent("Post that have more likes");

        var postThatHaveMoreComments = new PostDto();
        postThatHaveMoreComments.setAnonymous(true);
        postThatHaveMoreComments.setContent("Post that have more comments");
        var comment = new NewComment();
        comment.setAnonymous(false);
        comment.setContent("Comment N");

        var postNoTop = new PostDto();
        postNoTop.setAnonymous(false);
        postNoTop.setContent("Post no top");

        var posterAccessToken = "Bearer " + obtainAccessToken(POSTER_USERNAME, POSTER_PASSWORD);
        var likerAccessToken = "Bearer " + obtainAccessToken(LIKER_USERNAME, LIKER_PASSWORD);
        var commenterAccessToken = "Bearer " + obtainAccessToken(COMMENTER_USERNAME, COMMENTER_PASSWORD);

        var postIdThatHaveMoreLikes = expectedCreatePost(postThatHaveMoreLikes, posterAccessToken);
        var postIdThatHaveMoreComments = expectedCreatePost(postThatHaveMoreComments, posterAccessToken);
        expectedCreatePost(postNoTop, posterAccessToken);

        expectedLikePost(postIdThatHaveMoreLikes, likerAccessToken);
        expectedLikePost(postIdThatHaveMoreLikes, commenterAccessToken);
        expectedLikePost(postIdThatHaveMoreLikes, posterAccessToken);

        expectedCommentPost(comment, postIdThatHaveMoreComments, commenterAccessToken);
        expectedCommentPost(comment, postIdThatHaveMoreComments, likerAccessToken);
        expectedCommentPost(comment, postIdThatHaveMoreComments, commenterAccessToken);
        expectedCommentPost(comment, postIdThatHaveMoreComments, likerAccessToken);

        mockMvc.perform(get("/forum/post/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostLikedPost.content").value("Post that have more likes"))
                .andExpect(jsonPath("$.mostLikedPost.likedBy.size()").value(3))
                .andExpect(jsonPath("$.mostCommentedPost.content").value("Post that have more comments"))
                .andExpect(jsonPath("$.mostCommentedPost.comments.size()").value(4));
    }

    private void expectedLikePost(String postId, String likerAccessToken) throws Exception {
        mockMvc.perform(post("/forum/post/{postId}/like", postId)
                        .header("Authorization", likerAccessToken))
                .andExpect(status().isOk());
    }

    private void expectedUnLikePost(String postId, String likerAccessToken) throws Exception {
        mockMvc.perform(post("/forum/post/{postId}/unlike", postId)
                        .header("Authorization", likerAccessToken))
                .andExpect(status().isOk());
    }

    private void expectedCommentPost(NewComment comment, String postId, String commenterAccessToken) throws Exception {
        var jsonRequestComment2 = objectMapper.writeValueAsString(comment);

        mockMvc.perform(post("/forum/post/{postId}/comments", postId)
                        .header("Authorization", commenterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequestComment2))
                .andExpect(status().isOk());
    }

    private String expectedCreatePost(PostDto post, String posterAccessToken) throws Exception {
        var jsonRequest = objectMapper.writeValueAsString(post);

        //EXTRACT ID TO JSON POST RESPONSE
        var contentResponse = mockMvc.perform(post("/forum/post")
                        .header("Authorization", posterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //EXTRACT ID TO JSON POST RESPONSE
        return JsonPath.read(contentResponse, "$.id");
    }


}