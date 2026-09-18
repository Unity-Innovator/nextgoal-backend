package com.example.genz.reward;

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
import com.example.genz.reward.enums.RewardSize;
import com.example.genz.reward.mapper.CoinRewardMapper;
import com.example.genz.reward.repository.CoinRewardRepository;
import com.example.genz.reward.repository.CoinRewardSizeRepository;
import com.example.genz.reward.repository.RewardRedemptionRepository;
import com.example.genz.reward.service.CoinRewardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoinRewardServiceTest {

    @Mock
    private CoinRewardRepository coinRewardRepository;

    @Mock
    private CoinRewardSizeRepository coinRewardSizeRepository;

    @Mock
    private RewardRedemptionRepository rewardRedemptionRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private CoinRewardMapper coinRewardMapper = new CoinRewardMapper();

    @InjectMocks
    private CoinRewardServiceImpl coinRewardService;

    private User testUser;
    private CoinReward shirtReward;
    private CoinReward umbrellaReward;
    private CoinRewardSize sizeM;
    private CoinRewardSize sizeL;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFullName("John Doe");
        testUser.setPhoneNumber("012345678");
        testUser.setCoinBalance(2450);

        shirtReward = CoinReward.builder()
                .id(UUID.randomUUID())
                .name("NextGoal T-Shirt")
                .description("NextGoal branded T-Shirt")
                .imageUrl("https://example.com/shirt.png")
                .redeemCoins(500)
                .stock(0)
                .isActive(true)
                .build();

        sizeM = CoinRewardSize.builder()
                .id(UUID.randomUUID())
                .coinReward(shirtReward)
                .size(RewardSize.M)
                .stock(15)
                .build();

        sizeL = CoinRewardSize.builder()
                .id(UUID.randomUUID())
                .coinReward(shirtReward)
                .size(RewardSize.L)
                .stock(20)
                .build();

        shirtReward.setCoinRewardSizes(List.of(sizeM, sizeL));

        umbrellaReward = CoinReward.builder()
                .id(UUID.randomUUID())
                .name("Umbrella")
                .description("NextGoal branded umbrella")
                .imageUrl("https://example.com/umbrella.png")
                .redeemCoins(300)
                .stock(10)
                .isActive(true)
                .coinRewardSizes(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("1. Retrieve active rewards in catalog")
    void testGetAllActiveRewards() {
        when(coinRewardRepository.findByIsActiveTrue()).thenReturn(List.of(shirtReward, umbrellaReward));

        List<CoinRewardResponse> catalog = coinRewardService.getAllActiveRewards();

        assertEquals(2, catalog.size());
        assertEquals("NextGoal T-Shirt", catalog.get(0).getName());
        assertEquals("Umbrella", catalog.get(1).getName());
    }

    @Test
    @DisplayName("2. Retrieve reward with sizes includes available size options")
    void testGetRewardWithSizes() {
        when(coinRewardRepository.findById(shirtReward.getId())).thenReturn(Optional.of(shirtReward));

        CoinRewardResponse response = coinRewardService.getRewardById(shirtReward.getId());

        assertNotNull(response);
        assertEquals(2, response.getSizes().size());
        assertEquals(RewardSize.M, response.getSizes().get(0).getSize());
        assertEquals(15, response.getSizes().get(0).getStock());
    }

    @Test
    @DisplayName("3. Retrieve reward without sizes has empty sizes list")
    void testGetRewardWithoutSizes() {
        when(coinRewardRepository.findById(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));

        CoinRewardResponse response = coinRewardService.getRewardById(umbrellaReward.getId());

        assertNotNull(response);
        assertNotNull(response.getSizes());
        assertTrue(response.getSizes().isEmpty(), "Sizes should be empty list, not null");
        assertEquals(10, response.getStock());
    }

    @Test
    @DisplayName("4. Inactive reward is not redeemable")
    void testInactiveRewardCannotBeRedeemed() {
        umbrellaReward.setActive(false);
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findByIdForUpdate(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(umbrellaReward.getId())
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                coinRewardService.redeemReward(request, testUser.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    @DisplayName("5. Successfully redeem reward without size")
    void testRedeemRewardWithoutSizeSuccess() {
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findById(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));
        when(coinRewardRepository.findByIdForUpdate(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));
        when(coinRewardSizeRepository.findByCoinRewardId(umbrellaReward.getId())).thenReturn(Collections.emptyList());

        when(rewardRedemptionRepository.save(any(RewardRedemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(umbrellaReward.getId())
                .phoneNumber("098765432")
                .address("Siem Reap, Cambodia")
                .build();

        RedemptionResponse response = coinRewardService.redeemReward(request, testUser.getPhoneNumber());

        assertNotNull(response);
        assertTrue(response.getReferenceNumber().startsWith("REF-2026-TXN-"));
        assertEquals("Umbrella", response.getItem());
        assertNull(response.getSize());
        assertEquals(300, response.getCoinsUsed());
        assertEquals(2150, response.getRemainingCoins());
        assertEquals(RewardRedemptionStatus.CONFIRMED, response.getStatus());
        assertNotNull(response.getEstimatedDeliveryFrom());
        assertNotNull(response.getEstimatedDeliveryTo());

        // Verify stock deducted
        assertEquals(9, umbrellaReward.getStock());
        // Verify user coins deducted
        assertEquals(2150, testUser.getCoinBalance());
    }

    @Test
    @DisplayName("6. Successfully redeem reward with size")
    void testRedeemRewardWithSizeSuccess() {
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findByIdForUpdate(shirtReward.getId())).thenReturn(Optional.of(shirtReward));
        when(coinRewardSizeRepository.findByCoinRewardId(shirtReward.getId())).thenReturn(List.of(sizeM, sizeL));
        when(coinRewardSizeRepository.findByIdForUpdate(sizeM.getId())).thenReturn(Optional.of(sizeM));

        when(rewardRedemptionRepository.save(any(RewardRedemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(shirtReward.getId())
                .sizeId(sizeM.getId())
                .phoneNumber("012345678")
                .address("Phnom Penh, Cambodia")
                .build();

        RedemptionResponse response = coinRewardService.redeemReward(request, testUser.getPhoneNumber());

        assertNotNull(response);
        assertTrue(response.getReferenceNumber().startsWith("REF-2026-TXN-"));
        assertEquals("NextGoal T-Shirt", response.getItem());
        assertEquals("M", response.getSize());
        assertEquals(500, response.getCoinsUsed());
        assertEquals(1950, response.getRemainingCoins());
        assertEquals(RewardRedemptionStatus.CONFIRMED, response.getStatus());

        // Verify size M stock was decremented from 15 to 14
        assertEquals(14, sizeM.getStock());
        // Verify size L stock was untouched
        assertEquals(20, sizeL.getStock());
        // Verify user coins deducted
        assertEquals(1950, testUser.getCoinBalance());
    }

    @Test
    @DisplayName("7. Size is required when reward has sizes")
    void testSizeRequiredWhenRewardHasSizes() {
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findByIdForUpdate(shirtReward.getId())).thenReturn(Optional.of(shirtReward));
        when(coinRewardSizeRepository.findByCoinRewardId(shirtReward.getId())).thenReturn(List.of(sizeM, sizeL));

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(shirtReward.getId())
                .sizeId(null) // omitted!
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                coinRewardService.redeemReward(request, testUser.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("Size is required"));
    }

    @Test
    @DisplayName("8. Size does not belong to reward")
    void testSizeDoesNotBelongToReward() {
        CoinReward anotherReward = CoinReward.builder()
                .id(UUID.randomUUID())
                .name("Another Product")
                .build();

        CoinRewardSize foreignSize = CoinRewardSize.builder()
                .id(UUID.randomUUID())
                .coinReward(anotherReward)
                .size(RewardSize.S)
                .stock(5)
                .build();

        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findByIdForUpdate(shirtReward.getId())).thenReturn(Optional.of(shirtReward));
        when(coinRewardSizeRepository.findByCoinRewardId(shirtReward.getId())).thenReturn(List.of(sizeM));
        when(coinRewardSizeRepository.findByIdForUpdate(foreignSize.getId())).thenReturn(Optional.of(foreignSize));

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(shirtReward.getId())
                .sizeId(foreignSize.getId())
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                coinRewardService.redeemReward(request, testUser.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("does not belong"));
    }

    @Test
    @DisplayName("9. Size is out of stock")
    void testSizeOutOfStock() {
        sizeM.setStock(0);
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findByIdForUpdate(shirtReward.getId())).thenReturn(Optional.of(shirtReward));
        when(coinRewardSizeRepository.findByCoinRewardId(shirtReward.getId())).thenReturn(List.of(sizeM));
        when(coinRewardSizeRepository.findByIdForUpdate(sizeM.getId())).thenReturn(Optional.of(sizeM));

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(shirtReward.getId())
                .sizeId(sizeM.getId())
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                coinRewardService.redeemReward(request, testUser.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("out of stock"));
    }

    @Test
    @DisplayName("10. Reward is out of stock (unsized)")
    void testUnsizedRewardOutOfStock() {
        umbrellaReward.setStock(0);
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findById(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));
        when(coinRewardRepository.findByIdForUpdate(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));
        when(coinRewardSizeRepository.findByCoinRewardId(umbrellaReward.getId())).thenReturn(Collections.emptyList());

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(umbrellaReward.getId())
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                coinRewardService.redeemReward(request, testUser.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("out of stock"));
    }

    @Test
    @DisplayName("11. User has insufficient coins")
    void testInsufficientCoins() {
        testUser.setCoinBalance(100); // needs 300
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findById(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));
        when(coinRewardRepository.findByIdForUpdate(umbrellaReward.getId())).thenReturn(Optional.of(umbrellaReward));
        when(coinRewardSizeRepository.findByCoinRewardId(umbrellaReward.getId())).thenReturn(Collections.emptyList());

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(umbrellaReward.getId())
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                coinRewardService.redeemReward(request, testUser.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("Insufficient coins"));
        // User coin balance remains 100
        assertEquals(100, testUser.getCoinBalance());
        // Umbrella stock remains 10
        assertEquals(10, umbrellaReward.getStock());
    }

    @Test
    @DisplayName("12. Historical snapshot preservation: changes to reward entity do not alter redemption snapshot")
    void testHistoricalSnapshotPreservation() {
        when(userRepository.findByPhoneNumberForUpdate(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));
        when(coinRewardRepository.findByIdForUpdate(shirtReward.getId())).thenReturn(Optional.of(shirtReward));
        when(coinRewardSizeRepository.findByCoinRewardId(shirtReward.getId())).thenReturn(List.of(sizeM));
        when(coinRewardSizeRepository.findByIdForUpdate(sizeM.getId())).thenReturn(Optional.of(sizeM));

        ArgumentCaptor<RewardRedemption> captor = ArgumentCaptor.forClass(RewardRedemption.class);

        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(shirtReward.getId())
                .sizeId(sizeM.getId())
                .phoneNumber("0888999000")
                .address("123 Street, Battambang")
                .build();

        coinRewardService.redeemReward(request, testUser.getPhoneNumber());

        verify(rewardRedemptionRepository).save(captor.capture());
        RewardRedemption saved = captor.getValue();

        assertEquals("NextGoal T-Shirt", saved.getItemName());
        assertEquals("M", saved.getSelectedSize());
        assertEquals(500, saved.getCoinsUsed());
        assertEquals("0888999000", saved.getPhoneNumber());
        assertEquals("123 Street, Battambang", saved.getDeliveryAddress());
    }

    @Test
    @DisplayName("13. User can only view their own redemption history")
    void testUserCanOnlyViewTheirOwnRedemptionHistory() {
        when(userRepository.findByPhoneNumber(testUser.getPhoneNumber())).thenReturn(Optional.of(testUser));

        RewardRedemption r1 = RewardRedemption.builder()
                .id(UUID.randomUUID())
                .referenceNumber("REF-2026-TXN-11111")
                .user(testUser)
                .itemName("Umbrella")
                .coinsUsed(300)
                .status(RewardRedemptionStatus.CONFIRMED)
                .estimatedDeliveryFrom(LocalDate.now().plusDays(3))
                .estimatedDeliveryTo(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(rewardRedemptionRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(List.of(r1));

        List<RedemptionHistoryResponse> history = coinRewardService.getRedemptionHistory(testUser.getPhoneNumber());

        assertEquals(1, history.size());
        assertEquals("REF-2026-TXN-11111", history.get(0).getReferenceNumber());
        assertEquals("Umbrella", history.get(0).getItem());
        assertEquals(300, history.get(0).getCoinsUsed());
    }
}
