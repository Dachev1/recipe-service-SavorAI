package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.BadRequestException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.repository.RecipeVoteRepository;
import dev.idachev.recipeservice.web.dto.VoteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VoteService {

    private final RecipeVoteRepository voteRepository;
    private final RecipeRepository recipeRepository;

    @Autowired
    public VoteService(RecipeVoteRepository voteRepository,
                       RecipeRepository recipeRepository) {
        this.voteRepository = voteRepository;
        this.recipeRepository = recipeRepository;
    }

    /**
     * Cast a vote (upvote or downvote) for a recipe based on VoteRequest.
     * Handles mapping from request string to internal enum.
     * Updates vote counts directly on the Recipe entity.
     *
     * @param recipeId The ID of the recipe to vote on
     * @param request The vote request DTO containing vote type string
     * @param userId The ID of the user casting the vote
     * @return The updated Recipe entity with refreshed vote counts.
     */
    @Transactional
    public Recipe vote(UUID recipeId, VoteRequest request, UUID userId) {
        log.debug("Vote request: recipeId={}, voteTypeString={}, userId={}", recipeId, request.voteType(), userId);

        // 1. Map vote type string from request to enum
        final RecipeVote.VoteType voteType;
        try {
            voteType = RecipeVote.VoteType.valueOf(request.voteType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid vote type string received: {}", request.voteType());
            throw new BadRequestException("Invalid vote type: '" + request.voteType() + "'. Must be 'UPVOTE' or 'DOWNVOTE'.");
        }
        log.debug("Mapped vote type: {}", voteType);

        // 2. Get the recipe
        Recipe existingRecipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        
        // 3. Check if user is trying to vote on their own recipe
        if (existingRecipe.getUserId().equals(userId)) {
            log.warn("User {} attempted to vote on their own recipe {}", userId, recipeId);
            throw new UnauthorizedAccessException("You cannot vote on your own recipe");
        }
        
        // 4. Determine vote action and persist RecipeVote changes
        // Use AtomicReference to track the state change (NEW, CHANGED, REMOVED, NO_CHANGE)
        // and the old vote type if applicable.
        AtomicReference<VoteActionState> actionState = new AtomicReference<>(VoteActionState.NO_CHANGE);
        AtomicReference<RecipeVote.VoteType> oldVoteTypeRef = new AtomicReference<>(null);

        voteRepository.findByUserIdAndRecipeId(userId, recipeId)
            .ifPresentOrElse(existingVote -> { // Vote exists: Update or Delete
                RecipeVote.VoteType oldVoteType = existingVote.getVoteType();
                oldVoteTypeRef.set(oldVoteType);
                log.debug("Existing vote found: type={}, newVoteType={}", oldVoteType, voteType);
                
                if (oldVoteType != voteType) { // Changing vote
                    log.debug("Replacing vote from {} to {}", oldVoteType, voteType);
                    // Delete the old vote record
                    voteRepository.delete(existingVote);
                    // Create the new vote record
                    RecipeVote newVote = RecipeVote.builder()
                        .userId(userId)
                        .recipeId(recipeId)
                        .voteType(voteType)
                        // createdAt handled by @PrePersist
                        // updatedAt will be set on creation by @PrePersist
                        .build();
                    voteRepository.save(newVote);
                    actionState.set(VoteActionState.CHANGED);
                    log.debug("Vote replaced: oldId={}, newId={}, type={}", existingVote.getId(), newVote.getId(), newVote.getVoteType());
                } else { // Unvoting (toggling off)
                    voteRepository.delete(existingVote);
                    actionState.set(VoteActionState.REMOVED);
                    log.debug("Vote deleted");
                }
            }, 
            () -> { // Vote does not exist: Create new
                log.debug("No existing vote found. Creating new {} vote", voteType);
                RecipeVote newVote = RecipeVote.builder()
                        .userId(userId)
                        .recipeId(recipeId)
                        .voteType(voteType)
                        .build();
                voteRepository.save(newVote);
                actionState.set(VoteActionState.NEW);
                log.debug("New vote saved: id={}", newVote.getId());
            });

        // 5. Calculate new vote counts based on action state
        int currentUpvotes = existingRecipe.getUpvotes() != null ? existingRecipe.getUpvotes() : 0;
        int currentDownvotes = existingRecipe.getDownvotes() != null ? existingRecipe.getDownvotes() : 0;
        int newUpvotes = currentUpvotes;
        int newDownvotes = currentDownvotes;
        RecipeVote.VoteType oldVoteType = oldVoteTypeRef.get();

        switch (actionState.get()) {
            case NEW:
                if (voteType == RecipeVote.VoteType.UPVOTE) newUpvotes++;
                else newDownvotes++;
                break;
            case REMOVED:
                if (voteType == RecipeVote.VoteType.UPVOTE) newUpvotes = Math.max(0, newUpvotes - 1);
                else newDownvotes = Math.max(0, newDownvotes - 1);
                break;
            case CHANGED:
                if (oldVoteType == RecipeVote.VoteType.UPVOTE) newUpvotes = Math.max(0, newUpvotes - 1);
                else newDownvotes = Math.max(0, newDownvotes - 1);
                // Add new vote
                if (voteType == RecipeVote.VoteType.UPVOTE) newUpvotes++;
                else newDownvotes++;
                break;
            case NO_CHANGE: // Should not happen with current logic, but handle defensively
            default:
                log.warn("Vote action resulted in NO_CHANGE state for recipe {}, user {}", recipeId, userId);
                break;
        }
        
        // 6. Create updated Recipe entity if counts changed
        Recipe recipeToSave = existingRecipe;
        if (newUpvotes != currentUpvotes || newDownvotes != currentDownvotes) {
            log.debug("Vote counts changing: Up {} -> {}, Down {} -> {}", currentUpvotes, newUpvotes, currentDownvotes, newDownvotes);
            recipeToSave = existingRecipe.toBuilder()
                            .upvotes(newUpvotes)
                            .downvotes(newDownvotes)
                            .updatedAt(LocalDateTime.now()) // Also update timestamp if votes change
                            .build();
            recipeToSave = recipeRepository.save(recipeToSave);
            log.debug("Recipe vote counts updated: id={}, newUpvotes={}, newDownvotes={}", 
                      recipeToSave.getId(), recipeToSave.getUpvotes(), recipeToSave.getDownvotes());
        } else {
             log.debug("Vote counts did not change for recipe {}", recipeId);
        }

        // 7. Return the potentially updated recipe entity
        return recipeToSave;
    }

    // Helper enum for state tracking
    private enum VoteActionState {
        NEW, REMOVED, CHANGED, NO_CHANGE
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
        // This method remains useful for RecipeService.enhanceWithUserInteractions
        return voteRepository.findByUserIdAndRecipeId(userId, recipeId)
                .map(RecipeVote::getVoteType)
                .orElse(null);
    }

    /**
     * Fetches user votes for multiple recipes efficiently.
     * @param userId The user ID.
     * @param recipeIds Set of recipe IDs.
     * @return Map of Recipe ID to VoteType (or null if no vote).
     */
    @Transactional(readOnly = true)
    public Map<UUID, RecipeVote.VoteType> getUserVotesForRecipes(UUID userId, Set<UUID> recipeIds) {
        if (userId == null || recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("Fetching votes for user {} on {} recipe IDs", userId, recipeIds.size());
        List<RecipeVote> votes = voteRepository.findByUserIdAndRecipeIdIn(userId, recipeIds);
        return votes.stream()
                .collect(Collectors.toMap(RecipeVote::getRecipeId, RecipeVote::getVoteType));
    }

    // Removed getUpvoteCount and getDownvoteCount as counts are maintained on Recipe entity
    // Ensure RecipeService.enhanceWithUserInteractions uses counts from the RecipeResponse/Recipe entity

} 