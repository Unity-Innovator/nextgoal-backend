package com.example.genz.reward.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.reward.dto.CoinRewardResponse;
import com.example.genz.reward.dto.RedeemRewardRequest;
import com.example.genz.reward.dto.RedemptionHistoryResponse;
import com.example.genz.reward.dto.RedemptionResponse;
import com.example.genz.reward.entity.CoinReward;
import com.example.genz.reward.entity.CoinRewardSize;
import com.example.genz.reward.entity.RewardRedemption;
import com.example.genz.reward.enums.RewardRedemptionStatus;
import com.example.genz.reward.mapper.CoinRewardMapper;
import com.example.genz.reward.repository.CoinRewardRepository;
import com.example.genz.reward.repository.CoinRewardSizeRepository;
import com.example.genz.reward.repository.RewardRedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoinRewardServiceImpl implements CoinRewardService {

    private final CoinRewardRepository coinRewardRepository;
    private final CoinRewardSizeRepository coinRewardSizeRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final UserRepository userRepository;
    private final CoinRewardMapper coinRewardMapper;

    @Value("${app.reward.delivery.min-days:3}")
    private int minDeliveryDays = 3;

    @Value("${app.reward.delivery.max-days:7}")
    private int maxDeliveryDays = 7;

    @Override
    @Transactional(readOnly = true)
    public List<CoinRewardResponse> getAllActiveRewards() {
        List<CoinReward> rewards = coinRewardRepository.findByIsActiveTrue();
        return coinRewardMapper.toCoinRewardResponseList(rewards);
    }

    @Override
    @Transactional(readOnly = true)
    public CoinRewardResponse getRewardById(UUID rewardId) {
        CoinReward reward = coinRewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNoteFoundException("Reward not found with ID: " + rewardId));
        return coinRewardMapper.toCoinRewardResponse(reward);
    }

    @Override
    @Transactional
    public RedemptionResponse redeemReward(RedeemRewardRequest request, String phoneNumber) {
        // 1. Authenticate & serialize user balance operations
        User user = userRepository.findByPhoneNumberForUpdate(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        // 2. Find and lock reward directly
        CoinReward reward = coinRewardRepository.findByIdForUpdate(request.getRewardId())
                .orElseThrow(() -> new ResourceNoteFoundException("Reward not found with ID: " + request.getRewardId()));

        if (!reward.isActive()) {
            throw new BadRequestException("Reward is not active for redemption");
        }

        // 3. Validate user coin balance before decrementing stock
        int cost = reward.getRedeemCoins();
        int currentCoins = user.getCoinBalance() != null ? user.getCoinBalance() : 0;
        if (currentCoins < cost) {
            throw new BadRequestException("Insufficient coins for redemption");
        }

        // 4. Determine if reward requires a size and validate/deduct stock
        List<CoinRewardSize> availableSizes = coinRewardSizeRepository.findByCoinRewardId(reward.getId());
        boolean hasSizes = !availableSizes.isEmpty();

        String selectedSizeName = null;
        CoinRewardSize selectedSizeEntity = null;

        if (hasSizes) {
            if (request.getSizeId() == null) {
                throw new BadRequestException("Size is required for this reward");
            }

            // Lock the selected size row to prevent overselling
            CoinRewardSize size = coinRewardSizeRepository.findByIdForUpdate(request.getSizeId())
                    .orElseThrow(() -> new ResourceNoteFoundException("Selected size not found"));

            if (!size.getCoinReward().getId().equals(reward.getId())) {
                throw new BadRequestException("Selected size does not belong to this reward");
            }

            if (size.getStock() <= 0) {
                throw new BadRequestException("Selected size is out of stock");
            }

            // Decrement stock
            size.setStock(size.getStock() - 1);
            coinRewardSizeRepository.saveAndFlush(size);

            selectedSizeName = size.getSize().name();
            selectedSizeEntity = size;
        } else {
            if (request.getSizeId() != null) {
                throw new BadRequestException("This reward does not have sizes");
            }

            if (reward.getStock() <= 0) {
                throw new BadRequestException("Reward is out of stock");
            }

            // Decrement stock
            reward.setStock(reward.getStock() - 1);
            coinRewardRepository.saveAndFlush(reward);
        }

        // 5. Deduct coins
        int remainingCoins = currentCoins - cost;
        user.setCoinBalance(remainingCoins);
        userRepository.saveAndFlush(user);

        // 6. Generate unique reference number & delivery window
        String referenceNumber = generateReferenceNumber();
        LocalDate now = LocalDate.now();
        LocalDate deliveryFrom = now.plusDays(minDeliveryDays);
        LocalDate deliveryTo = now.plusDays(maxDeliveryDays);

        // 7. Persist redemption snapshot
        RewardRedemption redemption = RewardRedemption.builder()
                .referenceNumber(referenceNumber)
                .user(user)
                .coinReward(reward)
                .coinRewardSize(selectedSizeEntity)
                .itemName(reward.getName())
                .selectedSize(selectedSizeName)
                .coinsUsed(cost)
                .phoneNumber(request.getPhoneNumber().trim())
                .deliveryAddress(request.getAddress().trim())
                .status(RewardRedemptionStatus.CONFIRMED)
                .estimatedDeliveryFrom(deliveryFrom)
                .estimatedDeliveryTo(deliveryTo)
                .build();

        RewardRedemption savedRedemption = rewardRedemptionRepository.save(redemption);
        log.info("Redemption successful: user={}, ref={}, item={}, coinsUsed={}, remainingCoins={}",
                user.getId(), referenceNumber, reward.getName(), cost, remainingCoins);

        return coinRewardMapper.toRedemptionResponse(savedRedemption, remainingCoins);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedemptionHistoryResponse> getRedemptionHistory(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        List<RewardRedemption> redemptions = rewardRedemptionRepository.findByUserOrderByCreatedAtDesc(user);
        return coinRewardMapper.toRedemptionHistoryResponseList(redemptions);
    }

    private String generateReferenceNumber() {
        int year = LocalDate.now().getYear();
        String candidate;
        int attempts = 0;
        do {
            int randomSeq = ThreadLocalRandom.current().nextInt(10000, 100000);
            candidate = String.format("REF-%d-TXN-%05d", year, randomSeq);
            attempts++;
            if (attempts > 50) {
                candidate = String.format("REF-%d-TXN-%05d-%d", year, randomSeq, System.currentTimeMillis() % 10000);
                break;
            }
        } while (rewardRedemptionRepository.existsByReferenceNumber(candidate));
        return candidate;
    }
}
