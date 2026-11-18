package com.cinnamon.service;

import com.cinnamon.dto.request.PersonalWordRequest;
import com.cinnamon.dto.response.PersonalWordResponse;
import com.cinnamon.entity.PersonalWord;
import com.cinnamon.entity.User;
import com.cinnamon.entity.WordList;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.PersonalWordRepository;
import com.cinnamon.repository.UserRepository;
import com.cinnamon.repository.WordListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quản lý flashcard từ vựng cá nhân (thêm / sửa / xóa / ôn tập), mỗi từ thuộc một danh sách {@link WordList}.
 * Mọi thao tác ràng buộc theo chủ sở hữu: người dùng chỉ thấy và sửa từ của chính mình.
 * Tham số "email" là principal của Spring Security (JWT subject).
 */
@Service
@RequiredArgsConstructor
public class PersonalWordService {

    private final PersonalWordRepository wordRepository;
    private final WordListRepository listRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách từ.
     *
     * @param listId null = tất cả danh sách
     * @param status "learning" (đang học), "mastered" (đã thuộc), còn lại: tất cả
     */
    @Transactional(readOnly = true)
    public List<PersonalWordResponse> list(String email, Long listId, String status) {
        Long userId = findUser(email).getId();
        List<PersonalWord> words = listId == null
                ? wordRepository.findByUserIdOrderByIdDesc(userId)
                : wordRepository.findByUserIdAndListIdOrderByIdDesc(userId, listId);

        return words.stream()
                .filter(w -> {
                    if ("learning".equalsIgnoreCase(status)) return !Boolean.TRUE.equals(w.getMastered());
                    if ("mastered".equalsIgnoreCase(status)) return Boolean.TRUE.equals(w.getMastered());
                    return true;
                })
                .map(PersonalWordResponse::from)
                .toList();
    }

    @Transactional
    public PersonalWordResponse create(String email, PersonalWordRequest req) {
        User user = findUser(email);
        PersonalWord word = PersonalWord.builder()
                .user(user)
                .list(findOwnedList(user.getId(), req.getListId()))
                .word(req.getWord().trim())
                .pronunciation(trimOrNull(req.getPronunciation()))
                .definition(req.getDefinition().trim())
                .exampleSentence(trimOrNull(req.getExampleSentence()))
                .build();
        return PersonalWordResponse.from(wordRepository.save(word));
    }

    @Transactional
    public PersonalWordResponse update(String email, Long id, PersonalWordRequest req) {
        User user = findUser(email);
        PersonalWord word = findOwned(user.getId(), id);
        word.setList(findOwnedList(user.getId(), req.getListId()));
        word.setWord(req.getWord().trim());
        word.setPronunciation(trimOrNull(req.getPronunciation()));
        word.setDefinition(req.getDefinition().trim());
        word.setExampleSentence(trimOrNull(req.getExampleSentence()));
        return PersonalWordResponse.from(word);
    }

    @Transactional
    public void delete(String email, Long id) {
        wordRepository.delete(findOwned(findUser(email).getId(), id));
    }

    /** Ghi nhận kết quả ôn tập: known = true → đã thuộc, false → tiếp tục học. */
    @Transactional
    public PersonalWordResponse review(String email, Long id, boolean known) {
        PersonalWord word = findOwned(findUser(email).getId(), id);
        word.setReviewCount((word.getReviewCount() == null ? 0 : word.getReviewCount()) + 1);
        word.setMastered(known);
        return PersonalWordResponse.from(word);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private PersonalWord findOwned(Long userId, Long id) {
        return wordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy từ vựng"));
    }

    private WordList findOwnedList(Long userId, Long listId) {
        return listRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách từ"));
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
