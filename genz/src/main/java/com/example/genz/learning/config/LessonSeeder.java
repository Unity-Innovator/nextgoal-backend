package com.example.genz.learning.config;

import com.example.genz.learning.entity.Lesson;
import com.example.genz.learning.entity.LessonQuestion;
import com.example.genz.learning.enums.LessonCategory;
import com.example.genz.learning.enums.LessonDifficulty;
import com.example.genz.learning.repository.LessonQuestionRepository;
import com.example.genz.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class LessonSeeder {

    private final LessonRepository lessonRepository;
    private final LessonQuestionRepository lessonQuestionRepository;

    @Bean
    CommandLineRunner seedLessons() {
        return args -> {
            if (lessonRepository.count() == 0) {
                // Lesson 1: Smart Saving & Emergency Funds
                Lesson lesson1 = createLesson("Smart Saving & Emergency Funds", LessonCategory.SAVING, LessonDifficulty.EASY, 50, "Learn how to build an emergency fund, save consistently, and leverage high-yield savings.");
                lesson1 = lessonRepository.save(lesson1);

                List<LessonQuestion> questions1 = List.of(
                        createQuestion(lesson1, "What is the recommended size of an emergency fund?", "1 week of expenses", "3 to 6 months of essential living expenses", "5 years of salary", "$50", "B", 1),
                        createQuestion(lesson1, "What is the main benefit of high-yield savings accounts?", "Higher interest rate compared to traditional accounts", "Free debit card rewards", "Unlimited cash withdrawals without limits", "No identity verification needed", "A", 2),
                        createQuestion(lesson1, "What is the 50/30/20 budgeting rule for savings?", "50% wants, 30% needs, 20% savings", "50% needs, 30% wants, 20% savings & debt repayment", "50% savings, 30% taxes, 20% food", "50% debt, 30% savings, 20% entertainment", "B", 3),
                        createQuestion(lesson1, "Which of the following is considered an unexpected emergency expense?", "Monthly Netflix subscription", "Sudden medical bill or car repair", "Annual vacation flight tickets", "Buying a new video game", "B", 4),
                        createQuestion(lesson1, "Why is starting to save early beneficial?", "Compound interest accelerates long-term growth", "Banks give free money to younger savers", "Inflation makes early money worth less", "It guarantees zero investment risk", "A", 5)
                );
                lessonQuestionRepository.saveAll(questions1);

                // Lesson 2: Budgeting Basics for Gen Z
                Lesson lesson2 = createLesson("Budgeting Basics for Gen Z", LessonCategory.BUDGETING, LessonDifficulty.EASY, 50, "Master fixed vs variable expenses, zero-based budgeting, and expense tracking.");
                lesson2 = lessonRepository.save(lesson2);

                List<LessonQuestion> questions2 = List.of(
                        createQuestion(lesson2, "What is a fixed expense in a personal budget?", "An expense that stays the same every month like rent", "Dining out at restaurants", "Weekend shopping trips", "Unexpected car maintenance", "A", 1),
                        createQuestion(lesson2, "What is zero-based budgeting?", "Having zero dollars in your bank account", "Allocating every dollar of income to a specific category until $0 remains unassigned", "Never spending money on entertainment", "Borrowing money without paying interest", "B", 2),
                        createQuestion(lesson2, "Which of the following is a variable expense?", "Monthly Apartment Rent", "Fixed Internet subscription bill", "Weekly grocery and dining expenses", "Car insurance fixed monthly premium", "C", 3),
                        createQuestion(lesson2, "What is the primary purpose of tracking daily expenses?", "To get taxed more accurately by the government", "To identify spending habits and cut unnecessary impulse buys", "To increase credit limit automatically", "To earn bank points on every transaction", "B", 4),
                        createQuestion(lesson2, "What is net income?", "Total earnings before taxes and deductions", "Take-home pay remaining after taxes and mandatory deductions", "Total money spent in a month", "Total credit limit available on cards", "B", 5)
                );
                lessonQuestionRepository.saveAll(questions2);

                // Lesson 3: Introduction to Investing & Stocks
                Lesson lesson3 = createLesson("Introduction to Investing & Stocks", LessonCategory.INVESTING, LessonDifficulty.MEDIUM, 100, "Understand stocks, index funds, diversification, and wealth accumulation.");
                lesson3 = lessonRepository.save(lesson3);

                List<LessonQuestion> questions3 = List.of(
                        createQuestion(lesson3, "What is a stock share?", "A loan made to the government", "Partial ownership in a publicly traded company", "A fixed-rate bank deposit certificate", "A cryptocurrency token", "B", 1),
                        createQuestion(lesson3, "What does diversification in investing mean?", "Putting all money into a single high-tech stock", "Spreading investments across various asset classes to reduce risk", "Trading stocks multiple times a day", "Keeping all money in physical cash", "B", 2),
                        createQuestion(lesson3, "What is an Index Fund?", "A fund tracking a specific market index like the S&P 500", "A high-risk lottery program", "A bank savings account with zero returns", "A government tax document", "A", 3),
                        createQuestion(lesson3, "What is inflation's impact on uninvested cash savings?", "Increases purchasing power over time", "Erodes purchasing power over time as prices rise", "Has zero effect on cash purchasing power", "Automatically doubles cash value every decade", "B", 4),
                        createQuestion(lesson3, "What is a dividend?", "A penalty charged by brokers", "A portion of company earnings distributed to shareholders", "The fee paid when buying mutual funds", "An interest payment on credit cards", "B", 5)
                );
                lessonQuestionRepository.saveAll(questions3);

                // Lesson 4: Understanding Debt & Credit Scores
                Lesson lesson4 = createLesson("Understanding Debt & Credit Scores", LessonCategory.DEBT_MANAGEMENT, LessonDifficulty.MEDIUM, 100, "Learn how credit scores work, debt payoff strategies, and credit card usage.");
                lesson4 = lessonRepository.save(lesson4);

                List<LessonQuestion> questions4 = List.of(
                        createQuestion(lesson4, "What factor has the largest impact on your credit score?", "Payment history (paying bills on time)", "The color of your debit card", "The total number of bank accounts open", "Your annual salary size", "A", 1),
                        createQuestion(lesson4, "What is credit utilization ratio?", "Percentage of available credit limit currently being used", "The fee charged for opening a credit card", "The interest rate charged on unpaid credit card balance", "The total cash withdrawn from an ATM", "A", 2),
                        createQuestion(lesson4, "Which debt payoff strategy targets the debt with highest interest rate first?", "Debt Avalanche method", "Debt Snowball method", "Random selection method", "Debt consolidation fee strategy", "A", 3),
                        createQuestion(lesson4, "What happens if you only pay the minimum payment on a credit card balance?", "The debt is fully cleared in 3 months", "You pay significant interest and take much longer to pay off the debt", "Your credit score automatically reaches 850", "The bank waives all interest charges", "B", 4),
                        createQuestion(lesson4, "What is compound interest when applied to credit card debt?", "Interest charged only on the original purchase amount", "Interest charged on both the principal and previously accumulated interest", "A bonus cash reward from the credit card company", "A discount on monthly bill payments", "B", 5)
                );
                lessonQuestionRepository.saveAll(questions4);

                // Lesson 5: Financial Literacy Essentials
                Lesson lesson5 = createLesson("Financial Literacy Essentials", LessonCategory.FINANCIAL_LITERACY, LessonDifficulty.HARD, 150, "Master essential economic principles, liquidity, real vs nominal return, and assets.");
                lesson5 = lessonRepository.save(lesson5);

                List<LessonQuestion> questions5 = List.of(
                        createQuestion(lesson5, "What is the Rule of 72 used for in finance?", "Estimating the years required to double an investment at a given annual return", "Calculating monthly mortgage payments", "Determining retirement age automatically", "Calculating income tax brackets", "A", 1),
                        createQuestion(lesson5, "What is liquidity in financial terms?", "How easily an asset can be converted into cash without losing value", "The total amount of water utility bills", "The debt ratio of a corporate business", "The total interest earned on bonds", "A", 2),
                        createQuestion(lesson5, "What is the difference between real vs nominal interest rate?", "Real interest rate accounts for inflation; nominal rate does not", "Nominal rate includes bank bonuses; real rate does not", "Real interest rate is fixed; nominal interest rate fluctuates daily", "There is no difference between real and nominal rates", "A", 3),
                        createQuestion(lesson5, "What is an asset?", "Something that puts money in your pocket or holds economic value", "A monthly bill or debt obligation", "An annual subscription expense", "A parking ticket fine", "A", 4),
                        createQuestion(lesson5, "What is risk tolerance in personal investing?", "An investor's willingness and ability to endure market value fluctuations", "The maximum amount a bank allows you to deposit", "The legal age requirement to open an investment account", "The penalty fee for early withdrawal from checking accounts", "A", 5)
                );
                lessonQuestionRepository.saveAll(questions5);

                System.out.println("Seeded 5 financial lessons and 25 quiz questions into the database.");
            }
        };
    }

    private Lesson createLesson(String title, LessonCategory category, LessonDifficulty difficulty, Integer pointsReward, String content) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setCategory(category);
        lesson.setDifficulty(difficulty);
        lesson.setPointsReward(pointsReward);
        lesson.setContent(content);
        lesson.setIsActive(true);
        return lesson;
    }

    private LessonQuestion createQuestion(Lesson lesson, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer, Integer orderIndex) {
        LessonQuestion question = new LessonQuestion();
        question.setLesson(lesson);
        question.setQuestion(questionText);
        question.setOptionA(optionA);
        question.setOptionB(optionB);
        question.setOptionC(optionC);
        question.setOptionD(optionD);
        question.setCorrectAnswer(correctAnswer);
        question.setOrderIndex(orderIndex);
        return question;
    }
}
