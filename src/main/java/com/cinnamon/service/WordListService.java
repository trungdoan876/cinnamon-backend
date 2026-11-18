package com.cinnamon.service;

import com.cinnamon.dto.request.WordListRequest;
import com.cinnamon.dto.response.WordListResponse;
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
 * Quản lý danh sách từ (bộ flashcard) của người dùng: tạo / đổi tên / xóa / xem tiến độ.
 * Xóa danh sách sẽ xóa luôn các từ bên trong.
 */
@Service
@RequiredArgsConstructor
public class WordListService {

    private final WordListRepository listRepository;
    private final PersonalWordRepository wordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WordListResponse> list(String email) {
        return listRepository.findByUserIdOrderByIdDesc(findUser(email).getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WordListResponse create(String email, WordListRequest req) {
        WordList list = WordList.builder()
                .user(findUser(email))
                .name(req.getName().trim())
                .description(blankToNull(req.getDescription()))
                .build();
        return toResponse(listRepository.save(list));
    }

    @Transactional
    public WordListResponse update(String email, Long id, WordListRequest req) {
        WordList list = findOwned(email, id);
        list.setName(req.getName().trim());
        list.setDescription(blankToNull(req.getDescription()));
        return toResponse(list);
    }

    @Transactional
    public void delete(String email, Long id) {
        WordList list = findOwned(email, id);
        wordRepository.deleteByListId(list.getId());
        listRepository.delete(list);
    }

    private WordList findOwned(String email, Long id) {
        return listRepository.findByIdAndUserId(id, findUser(email).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh sách từ"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private WordListResponse toResponse(WordList l) {
        return WordListResponse.builder()
                .id(l.getId())
                .name(l.getName())
                .description(l.getDescription())
                .wordCount(wordRepository.countByListId(l.getId()))
                .masteredCount(wordRepository.countByListIdAndMastered(l.getId(), true))
                .build();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

