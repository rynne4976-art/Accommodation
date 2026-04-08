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
import com.Accommodation.entity.QAccomOperationDay;
import com.Accommodation.entity.QAccomOperationPolicy;
import com.Accommodation.entity.QReview;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.time.LocalDate;

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

    private BooleanExpression priceGoe(Integer minPrice) {
        return minPrice == null ? null : QAccom.accom.pricePerNight.goe(minPrice);
    }

    private BooleanExpression priceLoe(Integer maxPrice) {
        return maxPrice == null ? null : QAccom.accom.pricePerNight.loe(maxPrice);
    }

    private BooleanExpression ratingGoe(Double minRating) {
        QAccom accom = QAccom.accom;
        return minRating == null ? null : avgRatingExpression(accom).goe(minRating);
    }

    private BooleanExpression notDeleted() {
        return QAccom.accom.deleted.isFalse();
    }

    private BooleanExpression availableForStay(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            return null;
        }

        QAccom accom = QAccom.accom;
        QAccomOperationDay operationDay = new QAccomOperationDay("operationDaySub");
        long requiredStayDays = checkOutDate.toEpochDay() - checkInDate.toEpochDay();

        return JPAExpressions
                .select(operationDay.count())
                .from(operationDay)
                .where(
                        operationDay.accom.eq(accom),
                        operationDay.operationDate.goe(checkInDate),
                        operationDay.operationDate.lt(checkOutDate)
                )
                .eq(requiredStayDays);
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

    private NumberExpression<Double> avgRatingExpression(QAccom accom) {
        QReview review = new QReview("reviewAvgSub");

        return Expressions.numberTemplate(
                Double.class,
                "coalesce(({0}), {1})",
                JPAExpressions
                        .select(review.rating.avg())
                        .from(review)
                        .where(review.accom.eq(accom)),
                0.0
        );
    }

    private NumberExpression<Long> reviewCountExpression(QAccom accom) {
        QReview review = new QReview("reviewCountSub");

        return Expressions.numberTemplate(
                Long.class,
                "coalesce(({0}), {1})",
                JPAExpressions
                        .select(review.count())
                        .from(review)
                        .where(review.accom.eq(accom)),
                0L
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
                        statusEq(accomSearchDto.getAccomStatus()),
                        priceGoe(accomSearchDto.getMinPrice()),
                        priceLoe(accomSearchDto.getMaxPrice()),
                        ratingGoe(accomSearchDto.getMinRating()),
                        availableForStay(accomSearchDto.getCheckInDate(), accomSearchDto.getCheckOutDate())
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
                        statusEq(accomSearchDto.getAccomStatus()),
                        priceGoe(accomSearchDto.getMinPrice()),
                        priceLoe(accomSearchDto.getMaxPrice()),
                        ratingGoe(accomSearchDto.getMinRating()),
                        availableForStay(accomSearchDto.getCheckInDate(), accomSearchDto.getCheckOutDate())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<MainAccomDto> getMainAccomPage(AccomSearchDto accomSearchDto, Pageable pageable) {

        QAccom accom = QAccom.accom;
        QAccomImg accomImg = QAccomImg.accomImg;
        QAccomOperationPolicy policy = QAccomOperationPolicy.accomOperationPolicy;
        NumberExpression<Double> avgRatingExpr = avgRatingExpression(accom);
        NumberExpression<Long> reviewCountExpr = reviewCountExpression(accom);

        List<MainAccomDto> content = queryFactory
                .select(new QMainAccomDto(
                        accom.id,
                        accom.accomName,
                        accom.accomType,
                        accom.grade,
                        accom.accomDetail,
                        accomImg.imgUrl,
                        accom.pricePerNight,
                        accom.location,
                        accom.roomCount,
                        avgRatingExpr,
                        reviewCountExpr.intValue(),
                        policy.checkInTime,
                        policy.checkOutTime
                ))
                .from(accom)
                .join(accomImg).on(
                        accomImg.accom.eq(accom),
                        accomImg.id.eq(repImageIdSubQuery(accom))
                )
                .leftJoin(policy).on(policy.accom.eq(accom))
                .where(
                        notDeleted(),
                        accomNameLike(accomSearchDto.getSearchQuery()),
                        accomTypeEq(accomSearchDto.getAccomType()),
                        gradeEq(accomSearchDto.getGrade()),
                        statusEq(accomSearchDto.getAccomStatus()),
                        priceGoe(accomSearchDto.getMinPrice()),
                        priceLoe(accomSearchDto.getMaxPrice()),
                        ratingGoe(accomSearchDto.getMinRating()),
                        availableForStay(accomSearchDto.getCheckInDate(), accomSearchDto.getCheckOutDate())
                )
                .orderBy(avgRatingExpr.desc(), reviewCountExpr.desc(), accom.id.desc())
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
                        statusEq(accomSearchDto.getAccomStatus()),
                        priceGoe(accomSearchDto.getMinPrice()),
                        priceLoe(accomSearchDto.getMaxPrice()),
                        ratingGoe(accomSearchDto.getMinRating()),
                        availableForStay(accomSearchDto.getCheckInDate(), accomSearchDto.getCheckOutDate())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
