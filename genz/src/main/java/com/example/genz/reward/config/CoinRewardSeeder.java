package com.example.genz.reward.config;

import com.example.genz.reward.entity.CoinReward;
import com.example.genz.reward.entity.CoinRewardSize;
import com.example.genz.reward.enums.RewardSize;
import com.example.genz.reward.repository.CoinRewardRepository;
import com.example.genz.reward.repository.CoinRewardSizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CoinRewardSeeder {

    private final CoinRewardRepository coinRewardRepository;
    private final CoinRewardSizeRepository coinRewardSizeRepository;

    @Bean
    CommandLineRunner seedCoinRewards() {
        return args -> {
            if (coinRewardRepository.count() == 0) {
                log.info("Seeding 10 default coin rewards into database...");

                // 1. NextGoal Signature Oversized T-Shirt (Sized)
                CoinReward tShirt = CoinReward.builder()
                        .name("NextGoal Signature Oversized T-Shirt")
                        .imageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=800&q=80")
                        .description("Premium heavyweight organic cotton oversized tee featuring the minimalist NextGoal emblem. Ultra-soft, breathable, and everyday-ready.")
                        .redeemCoins(500)
                        .stock(0)
                        .isActive(true)
                        .build();
                tShirt = coinRewardRepository.save(tShirt);

                coinRewardSizeRepository.saveAll(List.of(
                        createSize(tShirt, RewardSize.S, 25),
                        createSize(tShirt, RewardSize.M, 50),
                        createSize(tShirt, RewardSize.L, 50),
                        createSize(tShirt, RewardSize.XL, 30),
                        createSize(tShirt, RewardSize.XXL, 15)
                ));

                // 2. NextGoal Essential Streetwear Hoodie (Sized)
                CoinReward hoodie = CoinReward.builder()
                        .name("NextGoal Essential Streetwear Hoodie")
                        .imageUrl("https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=800&q=80")
                        .description("Cozy brushed fleece hoodie with embroidered savings streak badge on sleeve and subtle holographic chest logo.")
                        .redeemCoins(1200)
                        .stock(0)
                        .isActive(true)
                        .build();
                hoodie = coinRewardRepository.save(hoodie);

                coinRewardSizeRepository.saveAll(List.of(
                        createSize(hoodie, RewardSize.S, 15),
                        createSize(hoodie, RewardSize.M, 30),
                        createSize(hoodie, RewardSize.L, 35),
                        createSize(hoodie, RewardSize.XL, 20),
                        createSize(hoodie, RewardSize.XXL, 10)
                ));

                // 3. NextGoal Sport Windbreaker Jacket (Sized)
                CoinReward jacket = CoinReward.builder()
                        .name("NextGoal Sport Windbreaker Jacket")
                        .imageUrl("https://images.unsplash.com/photo-1548883354-7622d03aca27?auto=format&fit=crop&w=800&q=80")
                        .description("Weather-resistant lightweight zip jacket with reflective accents and secure zipper pockets. Perfect for active lifestyles.")
                        .redeemCoins(1800)
                        .stock(0)
                        .isActive(true)
                        .build();
                jacket = coinRewardRepository.save(jacket);

                coinRewardSizeRepository.saveAll(List.of(
                        createSize(jacket, RewardSize.S, 10),
                        createSize(jacket, RewardSize.M, 20),
                        createSize(jacket, RewardSize.L, 25),
                        createSize(jacket, RewardSize.XL, 15),
                        createSize(jacket, RewardSize.XXL, 8)
                ));

                // 4. NextGoal Stainless Steel Thermal Tumbler 750ml (Unsized)
                CoinReward tumbler = CoinReward.builder()
                        .name("NextGoal Stainless Steel Thermal Tumbler (750ml)")
                        .imageUrl("https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=800&q=80")
                        .description("Double-wall vacuum insulated stainless steel water bottle. Keeps drinks cold for 24 hours or hot for 12 hours with leak-proof lid.")
                        .redeemCoins(400)
                        .stock(80)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(tumbler);

                // 5. NextGoal Minimalist Canvas Tote Bag (Unsized)
                CoinReward toteBag = CoinReward.builder()
                        .name("NextGoal Minimalist Canvas Tote Bag")
                        .imageUrl("https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80")
                        .description("Eco-friendly heavy-duty 12oz natural cotton canvas tote with interior pocket for essentials and reinforced shoulder straps.")
                        .redeemCoins(250)
                        .stock(100)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(toteBag);

                // 6. NextGoal Premium Embroidered Cap (Unsized)
                CoinReward cap = CoinReward.builder()
                        .name("NextGoal Premium Embroidered Cap")
                        .imageUrl("https://images.unsplash.com/photo-1588850561407-ed78c282e89b?auto=format&fit=crop&w=800&q=80")
                        .description("Classic 6-panel structured curved brim baseball cap with adjustable brass buckle strap and 3D NextGoal typography.")
                        .redeemCoins(350)
                        .stock(60)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(cap);

                // 7. NextGoal Compact UV Protection Travel Umbrella (Unsized)
                CoinReward umbrella = CoinReward.builder()
                        .name("NextGoal Compact UV Protection Travel Umbrella")
                        .imageUrl("https://images.unsplash.com/photo-1556909212-d5b604d0c90d?auto=format&fit=crop&w=800&q=80")
                        .description("Wind-defying 8-rib reinforced automatic open/close folding umbrella with UPF 50+ sun protection coating and waterproof sleeve.")
                        .redeemCoins(300)
                        .stock(45)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(umbrella);

                // 8. NextGoal Wireless MagSafe Power Bank 10,000mAh (Unsized)
                CoinReward powerBank = CoinReward.builder()
                        .name("NextGoal Wireless MagSafe Power Bank (10,000mAh)")
                        .imageUrl("https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?auto=format&fit=crop&w=800&q=80")
                        .description("Sleek magnetic fast-charging wireless power bank with digital battery percentage display and USB-C Power Delivery.")
                        .redeemCoins(1500)
                        .stock(40)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(powerBank);

                // 9. NextGoal Ceramic Artisan Coffee Mug 350ml (Unsized)
                CoinReward mug = CoinReward.builder()
                        .name("NextGoal Ceramic Artisan Coffee Mug (350ml)")
                        .imageUrl("https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80")
                        .description("Handcrafted matte black ceramic mug with glossy interior glaze and gold foil NextGoal savings motto. Microwave and dishwasher safe.")
                        .redeemCoins(200)
                        .stock(70)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(mug);

                // 10. NextGoal Ergonomic Desk Mat & Mouse Pad (Unsized)
                CoinReward deskMat = CoinReward.builder()
                        .name("NextGoal Ergonomic Desk Mat & Mouse Pad")
                        .imageUrl("https://images.unsplash.com/photo-1616440347437-b1c73416efc2?auto=format&fit=crop&w=800&q=80")
                        .description("Extended 900x400mm waterproof vegan leather desk mat with anti-slip suede base and stitched anti-fray edges.")
                        .redeemCoins(450)
                        .stock(50)
                        .isActive(true)
                        .build();
                coinRewardRepository.save(deskMat);

                log.info("Successfully seeded 10 default coin rewards and sizes.");
            }
        };
    }

    private CoinRewardSize createSize(CoinReward reward, RewardSize size, int stock) {
        return CoinRewardSize.builder()
                .coinReward(reward)
                .size(size)
                .stock(stock)
                .build();
    }
}
