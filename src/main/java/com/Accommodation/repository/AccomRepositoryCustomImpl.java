package com.Accommodation.repository;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.dto.QMainAccomDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.QAccom;
import com.Accommodation.entity.QAccomImg;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class AccomRepositoryCustomImpl implements AccomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public AccomRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    private BooleanExpression accomNameLike(String searchQuery) {
        return (searchQuery == null || searchQuery.trim().isEmpty())
                ? null
                : QAccom.accom.accomName.containsIgnoreCase(searchQuery);
    }

    private BooleanExpression accomTypeEq(AccomType accomType) {
        return accomType == null ? null : QAccom.accom.accomType.eq(accomType);
    }

    private BooleanExpression gradeEq(AccomGrade grade) {
        return grade == null ? null : QAccom.accom.grade.eq(grade);
    }

    private BooleanExpression statusEq(AccomStatus status) {
        return status == null ? null : QAccom.accom.status.eq(status);
    }

    private BooleanExpression notDeleted() {
        return QAccom.accom.deleted.isFalse();
    }

    private com.querydsl.jpa.JPQLQuery<Long> repImageIdSubQuery(QAccom accom) {
        QAccomImg accomImgSub = new QAccomImg("accomImgSub");

        return JPAExpressions
                .select(accomImgSub.id.min())
                .from(accomImgSub)
                .where(
                        accomImgSub.accom.eq(accom),
                        accomImgSub.repImgYn.eq("Y")
                );
    }

    @Override
    public Page<Accom> getAdminAccomPage(AccomSearchDto accomSearchDto, Pageable pageable) {

        QAccom accom = QAccom.accom;

        List<Accom> content = queryFactory
                .selectFrom(accom)
                .where(
                        notDeleted(),
                        accomNameLike(accomSearchDto.getSearchQuery()),
                        accomTypeEq(accomSearchDto.getAccomType()),
                        gradeEq(accomSearchDto.getGrade()),
                        statusEq(accomSearchDto.getAccomStatus())
                )
                .orderBy(accom.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(accom.count())
                .from(accom)
                .where(
                        notDeleted(),
                        accomNameLike(accomSearchDto.getSearchQuery()),
                        accomTypeEq(accomSearchDto.getAccomType()),
                        gradeEq(accomSearchDto.getGrade()),
                        statusEq(accomSearchDto.getAccomStatus())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<MainAccomDto> getMainAccomPage(AccomSearchDto accomSearchDto, Pageable pageable) {

        QAccom accom = QAccom.accom;
        QAccomImg accomImg = QAccomImg.accomImg;

        List<MainAccomDto> content = queryFactory
                .select(new QMainAccomDto(
                        accom.id,
                        accom.accomName,
                        accom.grade,
                        accom.accomDetail,
                        accomImg.imgUrl,
                        accom.pricePerNight,
                        accom.location,
                        accom.roomCount,
                        accom.avgRating,
                        accom.reviewCount
                ))
                .from(accom)
                .join(accomImg).on(
                        accomImg.accom.eq(accom),
                        accomImg.id.eq(repImageIdSubQuery(accom))
                )
                .where(
                        notDeleted(),
                        accomNameLike(accomSearchDto.getSearchQuery()),
                        accomTypeEq(accomSearchDto.getAccomType()),
                        gradeEq(accomSearchDto.getGrade()),
                        statusEq(accomSearchDto.getAccomStatus())
                )
                .orderBy(accom.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(accom.count())
                .from(accom)
                .where(
                        notDeleted(),
                        accomNameLike(accomSearchDto.getSearchQuery()),
                        accomTypeEq(accomSearchDto.getAccomType()),
                        gradeEq(accomSearchDto.getGrade()),
                        statusEq(accomSearchDto.getAccomStatus())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}