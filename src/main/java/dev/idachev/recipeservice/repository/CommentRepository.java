package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.Comment;
import dev.idachev.recipeservice.repository.dto.RecipeCommentCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByRecipeId(UUID recipeId, Pageable pageable);

    // Add method to find comments by user ID with pagination
    Page<Comment> findByUserId(UUID userId, Pageable pageable);

    long countByRecipeId(UUID recipeId);

    // Use explicit @Query with the new DTO
    @Query("SELECT NEW dev.idachev.recipeservice.repository.dto.RecipeCommentCountDto(c.recipeId, COUNT(c)) " +
           "FROM Comment c WHERE c.recipeId IN :recipeIds GROUP BY c.recipeId")
    List<RecipeCommentCountDto> countByRecipeIdIn(@Param("recipeIds") Set<UUID> recipeIds);

} 