package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.repository.RecipeVoteRepository;
import dev.idachev.recipeservice.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class VoteService {

    private final RecipeVoteRepository voteRepository;
    private final RecipeRepository recipeRepository;
    private final UserService userService;

    @Autowired
    public VoteService(RecipeVoteRepository voteRepository,
                       RecipeRepository recipeRepository,
                       UserService userService) {
        this.voteRepository = voteRepository;
        this.recipeRepository = recipeRepository;
        this.userService = userService;
    }

    /**
     * Cast a vote (upvote or downvote) for a recipe.
     *
     * @param recipeId The ID of the recipe to vote on
     * @param voteType The type of vote (UPVOTE or DOWNVOTE)
     * @param userId The ID of the user casting the vote
     * @return The updated Recipe
     */
    @Transactional
    public Recipe vote(UUID recipeId, RecipeVote.VoteType voteType, UUID userId) {
        // Get the recipe
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        // Check if user is trying to vote on their own recipe
        if (recipe.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You cannot vote on your own recipe");
        }

        // Check if user has already voted
        Optional<RecipeVote> existingVote = voteRepository.findByUserIdAndRecipeId(userId, recipeId);

        if (existingVote.isPresent()) {
            RecipeVote vote = existingVote.get();
            RecipeVote.VoteType oldVoteType = vote.getVoteType();

            // If the user is changing their vote
            if (oldVoteType != voteType) {
                // Update vote counts
                if (oldVoteType == RecipeVote.VoteType.UPVOTE) {
                    recipe.setUpvotes(recipe.getUpvotes() - 1);
                } else {
                    recipe.setDownvotes(recipe.getDownvotes() - 1);
                }

                if (voteType == RecipeVote.VoteType.UPVOTE) {
                    recipe.setUpvotes(recipe.getUpvotes() + 1);
                } else {
                    recipe.setDownvotes(recipe.getDownvotes() + 1);
                }

                // Update vote
                vote.setVoteType(voteType);
                voteRepository.save(vote);
            } else {
                // User is unvoting
                if (voteType == RecipeVote.VoteType.UPVOTE) {
                    recipe.setUpvotes(recipe.getUpvotes() - 1);
                } else {
                    recipe.setDownvotes(recipe.getDownvotes() - 1);
                }
                
                // Remove vote
                voteRepository.delete(vote);
            }
        } else {
            // New vote
            RecipeVote vote = RecipeVote.builder()
                    .userId(userId)
                    .recipeId(recipeId)
                    .voteType(voteType)
                    .build();
            
            // Update vote counts
            if (voteType == RecipeVote.VoteType.UPVOTE) {
                recipe.setUpvotes(recipe.getUpvotes() + 1);
            } else {
                recipe.setDownvotes(recipe.getDownvotes() + 1);
            }
            
            voteRepository.save(vote);
        }

        // Save and return updated recipe
        return recipeRepository.save(recipe);
    }

    /**
     * Get the vote type for a user on a recipe.
     *
     * @param recipeId The ID of the recipe
     * @param userId The ID of the user
     * @return The vote type or null if no vote exists
     */
    @Transactional(readOnly = true)
    public RecipeVote.VoteType getUserVote(UUID recipeId, UUID userId) {
        return voteRepository.findByUserIdAndRecipeId(userId, recipeId)
                .map(RecipeVote::getVoteType)
                .orElse(null);
    }

    /**
     * Get the count of upvotes for a recipe.
     *
     * @param recipeId The ID of the recipe
     * @return The count of upvotes
     */
    @Transactional(readOnly = true)
    public long getUpvoteCount(UUID recipeId) {
        return voteRepository.countByRecipeIdAndVoteType(recipeId, RecipeVote.VoteType.UPVOTE);
    }

    /**
     * Get the count of downvotes for a recipe.
     *
     * @param recipeId The ID of the recipe
     * @return The count of downvotes
     */
    @Transactional(readOnly = true)
    public long getDownvoteCount(UUID recipeId) {
        return voteRepository.countByRecipeIdAndVoteType(recipeId, RecipeVote.VoteType.DOWNVOTE);
    }
} 