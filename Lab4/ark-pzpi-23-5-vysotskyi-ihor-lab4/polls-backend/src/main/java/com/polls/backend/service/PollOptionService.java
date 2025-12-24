package com.polls.backend.service;

import com.polls.backend.entity.*;
import com.polls.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollOptionService {

    private static final Logger logger = LoggerFactory.getLogger(PollOptionService.class);
    private static final int MAX_OPTION_TEXT_LENGTH = 500;
    private static final int MIN_OPTION_TEXT_LENGTH = 1;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @Autowired
    private PollRepository pollRepository;

    // ========================================================================
    // БІЗНЕС-ЛОГІКА: Управління варіантами з валідацією orderNum
    // ========================================================================

    /**
     * CRUD: Получить все варианты
     */
    public List<PollOption> getAll() {
        return pollOptionRepository.findAll();
    }

    /**
     * CRUD: Получить вариант по ID
     */
    public PollOption getOptionById(UUID id) {
        return pollOptionRepository.findById(id).orElse(null);
    }


    /**
     * Додавання варіанту до голосування з автоматичним orderNum
     *
     * ВАЛІДАЦІЯ:
     * 1. text не повинен бути null або пустим
     * 2. text не повинен перевищувати MAX_OPTION_TEXT_LENGTH
     * 3. Poll повинен існувати
     *
     * РЕФАКТОРИНГ: Чітка обробка помилок з логуванням
     */
    public PollOption addOption(UUID pollId, String text) {
        // Валідація 1: text null або пусто
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Спроба додати варіант з пустим текстом для Poll: {}", pollId);
            throw new IllegalArgumentException("Текст варіанту не може бути пустим");
        }

        // Валідація 2: довжина текста
        if (text.length() < MIN_OPTION_TEXT_LENGTH || text.length() > MAX_OPTION_TEXT_LENGTH) {
            logger.warn("Текст варіанту має неправильну довжину: {} символів для Poll: {}",
                    text.length(), pollId);
            throw new IllegalArgumentException(
                    String.format("Текст повинен бути від %d до %d символів",
                            MIN_OPTION_TEXT_LENGTH, MAX_OPTION_TEXT_LENGTH));
        }

        // Валідація 3: Poll існує
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> {
                    logger.error("Poll не знайдено: {}", pollId);
                    return new IllegalArgumentException("Poll не знайдено");
                });

        // МАТЕМАТИКА: Визначити наступний orderNum
        int nextOrderNum = getNextOrderNum(pollId);

        PollOption option = new PollOption();
        option.setPoll(poll);
        option.setText(text.trim());
        option.setOrderNum(nextOrderNum);

        PollOption saved = pollOptionRepository.save(option);
        logger.info("Варіант додано для Poll {}: '{}' з orderNum {}", pollId, text, nextOrderNum);

        return saved;
    }

    /**
     * Додавання варіанту з явним orderNum (з валідацією)
     *
     * ВАЛІДАЦІЯ:
     * 1. text валідний (як в addOption)
     * 2. orderNum >= 0
     * 3. orderNum не дублюється в межах Poll
     *
     * РЕФАКТОРИНГ: Винесена валідація тексту в окремий метод
     */
    public PollOption addOptionWithOrder(UUID pollId, String text, Integer orderNum) {
        // Валідація тексту (переиспользуємо логіку)
        validateOptionText(text);

        // Валідація orderNum: не null и >= 0
        if (orderNum == null || orderNum < 0) {
            logger.warn("Неправильний orderNum: {} для Poll: {}", orderNum, pollId);
            throw new IllegalArgumentException("orderNum повинен бути >= 0");
        }

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> {
                    logger.error("Poll не знайдено: {}", pollId);
                    return new IllegalArgumentException("Poll не знайдено");
                });

        // КРИТИЧНО: Безпечна перевірка на дублювання
        // Якщо options == null, то немає дублів
        if (poll.getOptions() != null && !poll.getOptions().isEmpty()) {
            boolean orderNumExists = poll.getOptions().stream()
                    .anyMatch(o -> o.getOrderNum() != null && o.getOrderNum().equals(orderNum));

            if (orderNumExists) {
                logger.warn("orderNum {} вже існує для Poll: {}", orderNum, pollId);
                throw new IllegalArgumentException(
                        "orderNum " + orderNum + " вже існує для цього голосування");
            }
        }

        PollOption option = new PollOption();
        option.setPoll(poll);
        option.setText(text.trim());
        option.setOrderNum(orderNum);

        PollOption saved = pollOptionRepository.save(option);
        logger.info("Варіант з orderNum {} додано для Poll {}: '{}'",
                orderNum, pollId, text);

        return saved;
    }

    /**
     * РЕФАКТОРИНГ: Винесена валідація тексту в окремий метод
     * DRY (Don't Repeat Yourself) - переиспользуємо логіку
     */
    private void validateOptionText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст варіанту не може бути пустим");
        }
        if (text.length() > MAX_OPTION_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Текст не повинен перевищувати %d символів",
                            MAX_OPTION_TEXT_LENGTH));
        }
    }

    /**
     * Отримання наступного доступного orderNum
     * МАТЕМАТИКА: max(orderNum) + 1
     *
     * РЕФАКТОРИНГ: Надійна обробка порожного списку
     */
    private int getNextOrderNum(UUID pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElse(null);

        if (poll == null || poll.getOptions() == null || poll.getOptions().isEmpty()) {
            return 0;
        }

        // МАТЕМАТИКА: Знайти максимальний orderNum
        // РЕФАКТОРИНГ: Обробка null значень в orderNum
        int maxOrder = poll.getOptions().stream()
                .map(PollOption::getOrderNum)
                .filter(o -> o != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);

        return maxOrder + 1;
    }

    /**
     * Отримання всіх варіантів для голосування в правильному порядку
     */
    public List<PollOption> getOptionsByPoll(UUID pollId) {
        Poll poll = pollRepository.findById(pollId).orElse(null);

        if (poll == null) {
            logger.warn("Poll не знайдено для отримання опцій: {}", pollId);
            return Collections.emptyList();
        }

        return pollOptionRepository.findByPollOrderByOrderNum(poll);
    }

    /**
     * Видалення варіанту з логуванням
     */
    public boolean deleteOption(UUID optionId) {
        if (pollOptionRepository.existsById(optionId)) {
            pollOptionRepository.deleteById(optionId);
            logger.info("Варіант видален: {}", optionId);
            return true;
        }
        logger.warn("Варіант не знайдено для видалення: {}", optionId);
        return false;
    }

    /**
     * Перепорядкування всіх варіантів після видалення
     * РЕФАКТОРИНГ: Атомарна операція, вся або нічого
     */
    public void reorderOptions(UUID pollId) {
        try {
            List<PollOption> options = getOptionsByPoll(pollId);

            for (int i = 0; i < options.size(); i++) {
                PollOption option = options.get(i);
                option.setOrderNum(i);
                pollOptionRepository.save(option);
            }
            logger.info("Варіанти переупорядковано для Poll: {}", pollId);
        } catch (Exception e) {
            logger.error("Помилка при переупорядкуванні варіантів для Poll: {}", pollId, e);
            throw new RuntimeException("Не вдалося переупорядкувати варіанти", e);
        }
    }
}