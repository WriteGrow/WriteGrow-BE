package com.example.writegrow.domain.writing.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 글의 수정 이력 한 건. 보호자 리포트의 "수정 전/후 비교"가 이 기록을 근거로 한다.
 */
@Getter
@Entity
@Table(name = "writing_revision")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WritingRevision extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writing_id", nullable = false)
    private Writing writing;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private RevisionSource source;

    WritingRevision(Writing writing, int revisionNo, String content, RevisionSource source) {
        this.writing = writing;
        this.revisionNo = revisionNo;
        this.content = content;
        this.source = source;
    }
}
