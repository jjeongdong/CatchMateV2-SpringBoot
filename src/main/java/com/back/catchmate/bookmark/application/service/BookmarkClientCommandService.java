package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.bookmark.application.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.bookmark.application.port.in.BookmarkClientCommandUseCase;
import com.back.catchmate.bookmark.application.port.out.persistence.BookmarkRepository;
import com.back.catchmate.bookmark.domain.model.Bookmark;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class BookmarkClientCommandService implements BookmarkClientCommandUseCase {
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkReader bookmarkReader;

    @Override
    public BookmarkUpdateResponse updateBookmark(Long userId, Long boardId) {
        Optional<Bookmark> bookmarkOptional = bookmarkReader.findByUserIdAndBoardId(userId, boardId);

        if (bookmarkOptional.isPresent()) {
            bookmarkRepository.delete(bookmarkOptional.get());
            return BookmarkUpdateResponse.of(boardId, false);
        }

        bookmarkRepository.save(Bookmark.createBookmark(userId, boardId));
        return BookmarkUpdateResponse.of(boardId, true);
    }
}
