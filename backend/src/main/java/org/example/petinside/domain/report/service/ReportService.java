package org.example.petinside.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.report.dto.ReportCreateRequest;
import org.example.petinside.domain.report.entity.Report;
import org.example.petinside.domain.report.entity.ReportStatus;
import org.example.petinside.domain.report.entity.ReportTargetType;
import org.example.petinside.domain.report.repository.ReportRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // F-43: 게시글/댓글/대댓글 신고
    @Transactional
    public IdResponse createReport(Long userId, ReportCreateRequest request) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Long authorId = resolveAuthorId(request.targetType(), request.targetId());

        if (authorId.equals(userId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "본인이 작성한 글은 신고할 수 없습니다.");
        }

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                userId, request.targetType(), request.targetId(), ReportStatus.PENDING)) {
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "이미 신고한 내용입니다.");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(request.detail())
                .build();
        reportRepository.save(report);

        return IdResponse.from(report.getId());
    }

    private Long resolveAuthorId(ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .filter(p -> !p.isDeleted())
                    .orElseThrow(() -> new PostNotFoundException(targetId));
            return post.getAuthor().getId();
        }

        Comment comment = commentRepository.findById(targetId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(targetId));
        return comment.getUser().getId();
    }
}
