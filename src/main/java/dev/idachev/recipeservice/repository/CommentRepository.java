package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    Page<Comment> findByRecipeId(UUID recipeId, Pageable pageable);
    
    List<Comment> findByRecipeId(UUID recipeId);
    
    List<Comment> findByUserId(UUID userId);
    
    long countByRecipeId(UUID recipeId);
    
    void deleteByRecipeId(UUID recipeId);
} 