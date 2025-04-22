package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.RecipeVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RecipeVoteRepository extends JpaRepository<RecipeVote, UUID> {
    
    Optional<RecipeVote> findByUserIdAndRecipeId(UUID userId, UUID recipeId);
    
    List<RecipeVote> findByRecipeId(UUID recipeId);
    
    List<RecipeVote> findByUserId(UUID userId);
    
    List<RecipeVote> findByUserIdAndRecipeIdIn(UUID userId, Set<UUID> recipeIds);
    
    // TODO: Verify intended usage. If Recipe entity uses cascade delete for votes,
    // this method might be redundant or only needed for specific bulk operations.
    void deleteByRecipeId(UUID recipeId);
} 