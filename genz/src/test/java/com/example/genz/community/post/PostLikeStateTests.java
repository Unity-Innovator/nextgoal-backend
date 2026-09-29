package com.example.genz.community.post;

import com.example.genz.auth.entity.User;
import com.example.genz.community.chat.service.SupabaseStorageService;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.mapper.PostMapper;
import com.example.genz.community.post.repository.PostLikeRepository;
import com.example.genz.settings.repository.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostLikeStateTests {
    @Test
    void readResponseReflectsViewerLikeRatherThanTotalCount() {
        var likes = mock(PostLikeRepository.class);
        var mapper = new PostMapper(mock(UserSettingsRepository.class),
                mock(SupabaseStorageService.class), likes);
        var owner = new User();
        owner.setId(UUID.randomUUID());
        var post = new Post();
        post.setId(UUID.randomUUID());
        post.setAuthor(owner);
        post.setLikeCount(1);
        when(likes.existsByPost_IdAndUser_Id(post.getId(), owner.getId())).thenReturn(true, false);

        var liked = mapper.toResponse(post, owner.getId());
        assertTrue(liked.likedByCurrentUser());
        assertTrue(liked.ownedByCurrentUser());
        assertEquals(1, liked.likeCount());

        var otherViewer = UUID.randomUUID();
        var other = mapper.toResponse(post, otherViewer);
        assertFalse(other.likedByCurrentUser());
        assertEquals(1, other.likeCount());
        verify(likes).existsByPost_IdAndUser_Id(post.getId(), otherViewer);

        assertFalse(mapper.toResponse(post, owner.getId()).likedByCurrentUser());
    }
}
