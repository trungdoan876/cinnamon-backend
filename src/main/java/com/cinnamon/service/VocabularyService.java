package com.cinnamon.service;

import com.cinnamon.dto.request.VocabularyRequest;
import com.cinnamon.dto.response.VocabularyResponse;
import com.cinnamon.entity.Lesson;
import com.cinnamon.entity.Vocabulary;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.LessonRepository;
import com.cinnamon.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VocabularyService - Dịch vụ quản lý các thẻ từ vựng (Flashcard) theo từng bài học.
 * Cung cấp:
 * - Lấy danh sách từ vựng kèm phiên âm IPA, định nghĩa, câu ví dụ, hình ảnh và audio phát âm.
 * - Thêm mới, cập nhật và xóa thẻ từ vựng trong bài học.
 */
@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final LessonRepository lessonRepository;

    /**
     * Lấy danh sách toàn bộ từ vựng thuộc về một bài học cụ thể.
     *
     * @param lessonId ID bài học
     * @return Danh sách VocabularyResponse
     */
    public List<VocabularyResponse> getVocabulariesByLessonId(Long lessonId) {
        return vocabularyRepository.findByLessonId(lessonId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Thêm mới một thẻ từ vựng vào bài học.
     *
     * @param request chứa lessonId, word, pronunciation, definition, exampleSentence, imageUrl, audioUrl
     * @return VocabularyResponse của từ vựng vừa tạo
     */
    @Transactional
    public VocabularyResponse addVocabulary(VocabularyRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài học không tồn tại"));

        Vocabulary vocab = Vocabulary.builder()
                .lesson(lesson)
                .word(request.getWord())
                .pronunciation(request.getPronunciation())
                .definition(request.getDefinition())
                .exampleSentence(request.getExampleSentence())
                .imageUrl(request.getImageUrl())
                .audioUrl(request.getAudioUrl())
                .build();

        return toResponse(vocabularyRepository.save(vocab));
    }

    /**
     * Chỉnh sửa thông tin của một thẻ từ vựng theo ID.
     *
     * @param id      ID thẻ từ vựng cần sửa
     * @param request dữ liệu cập nhật mới
     * @return VocabularyResponse sau khi cập nhật
     */
    @Transactional
    public VocabularyResponse updateVocabulary(Long id, VocabularyRequest request) {
        Vocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Từ vựng không tồn tại"));

        vocab.setWord(request.getWord());
        vocab.setPronunciation(request.getPronunciation());
        vocab.setDefinition(request.getDefinition());
        vocab.setExampleSentence(request.getExampleSentence());
        vocab.setImageUrl(request.getImageUrl());
        vocab.setAudioUrl(request.getAudioUrl());

        return toResponse(vocabularyRepository.save(vocab));
    }

    /**
     * Xóa thẻ từ vựng khỏi bài học.
     *
     * @param id ID thẻ từ vựng cần xóa
     */
    @Transactional
    public void deleteVocabulary(Long id) {
        Vocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Từ vựng không tồn tại"));
        vocabularyRepository.delete(vocab);
    }

    /**
     * Chuyển đổi Entity Vocabulary sang DTO VocabularyResponse để trả về client.
     */
    private VocabularyResponse toResponse(Vocabulary vocab) {
        return VocabularyResponse.builder()
                .id(vocab.getId())
                .lessonId(vocab.getLesson().getId())
                .word(vocab.getWord())
                .pronunciation(vocab.getPronunciation())
                .definition(vocab.getDefinition())
                .exampleSentence(vocab.getExampleSentence())
                .imageUrl(vocab.getImageUrl())
                .audioUrl(vocab.getAudioUrl())
                .build();
    }
}
