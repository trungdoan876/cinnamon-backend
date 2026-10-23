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

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final LessonRepository lessonRepository;

    public List<VocabularyResponse> getVocabulariesByLessonId(Long lessonId) {
        return vocabularyRepository.findByLessonId(lessonId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

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

    @Transactional
    public void deleteVocabulary(Long id) {
        Vocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Từ vựng không tồn tại"));
        vocabularyRepository.delete(vocab);
    }

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
