package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    Page<Comment> findByRecipeId(UUID recipeId, Pageable pageable);
    
    List<Comment> findByRecipeId(UUID recipeId);
    
    Page<Comment> findByUserId(UUID userId, Pageable pageable);
    
    long countByRecipeId(UUID recipeId);
    
    // TODO: Verify intended usage. If Recipe entity uses cascade delete for comments,
    // this method might be redundant or only needed for specific bulk operations.
    void deleteByRecipeId(UUID recipeId);
    
    List<RecipeCommentCount> countByRecipeIdIn(Set<UUID> recipeIds);
    
    // Helper projection interface for the count query
    interface RecipeCommentCount {
        UUID getRecipeId();
        long getCommentCount();
    }
} 