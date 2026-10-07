package com.example.genz.assistant;

import java.util.List;
import java.util.Locale;

/** Exact social messages bypass retrieval; messages containing a question still use knowledge. */
final class AssistantSmallTalk {
    private AssistantSmallTalk() {}
    static AssistantService.Reply reply(String question) {
        String text = question.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}]+", " ")
                .trim().replaceAll("\\s+", " ");
        String answer;
        if (text.matches("hi+|hey+|hello+|hello there|good morning|good afternoon|good evening")) {
            answer = "Hi! How can I help you today? You can ask about NextGoal or the available documents.";
        } else if (text.equals("សួស្តី") || text.equals("ជំរាបសួរ")) {
            answer = "សួស្តី! តើខ្ញុំអាចជួយអ្នកអ្វីបានខ្លះថ្ងៃនេះ?";
        } else {
            answer = switch (text) {
                case "thanks", "thank you", "thanks a lot" -> "You're welcome!";
                case "bye", "goodbye", "see you" -> "Take care! Come back whenever you need a hand.";
                case "how are you", "how are you doing" -> "I'm here and ready to help! What would you like to work on?";
                default -> null;
            };
        }
        return answer == null ? null : new AssistantService.Reply(answer, List.of());
    }
}
