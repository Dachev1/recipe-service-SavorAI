package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.RecipeVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeVoteRepository extends JpaRepository<RecipeVote, UUID> {
    
    Optional<RecipeVote> findByUserIdAndRecipeId(UUID userId, UUID recipeId);
    
    List<RecipeVote> findByRecipeId(UUID recipeId);
    
    List<RecipeVote> findByUserId(UUID userId);
    
    long countByRecipeIdAndVoteType(UUID recipeId, RecipeVote.VoteType voteType);
    
    void deleteByRecipeId(UUID recipeId);
} 