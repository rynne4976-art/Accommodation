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
import com.Accommodation.util.AccomSearchKeywordUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public class AccomRepositoryCustomImpl implements AccomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public AccomRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    private Predicate accomNameLike(String searchQuery) {
        AccomSearchKeywordUtils.SearchKeywords keywords = AccomSearchKeywordUtils.parse(searchQuery);
        if (keywords.isEmpty()) {
            return null;
        }

        QAccom accom = QAccom.accom;
        BooleanBuilder builder = new BooleanBuilder();
        BooleanBuilder directMatch = new BooleanBuilder();

        directMatch.or(accom.accomName.containsIgnoreCase(keywords.getOriginalQuery()));
        directMatch.or(accom.location.containsIgnoreCase(keywords.getOriginalQuery()));
        directMatch.or(accom.accomDetail.containsIgnoreCase(keywords.getOriginalQuery()));
        directMatch.or(noSpace(accom.accomName).contains(keywords.getCompactQuery()));
        directMatch.or(noSpace(accom.location).contains(keywords.getCompactQuery()));
        directMatch.or(noSpace(accom.accomDetail).contains(keywords.getCompactQuery()));
        builder.or(directMatch);

        BooleanBuilder semanticMatch = new BooleanBuilder();
        if (!keywords.getMatchedRegions().isEmpty()) {
            for (String region : keywords.getMatchedRegions()) {
                BooleanBuilder regionMatch = new BooleanBuilder();
                for (String term : AccomSearchKeywordUtils.getRegionTerms(region)) {
                    regionMatch.or(accom.accomName.containsIgnoreCase(term));
                    regionMatch.or(accom.location.containsIgnoreCase(term));
                    regionMatch.or(accom.accomDetail.containsIgnoreCase(term));
                    regionMatch.or(noSpace(accom.accomName).contains(AccomSearchKeywordUtils.compact(term)));
                    regionMatch.or(noSpace(accom.location).contains(AccomSearchKeywordUtils.compact(term)));
                    regionMatch.or(noSpace(accom.accomDetail).contains(AccomSearchKeywordUtils.compact(term)));
                }
                semanticMatch.and(regionMatch);
            }
        }

        if (!keywords.getMatchedAccomTypes().isEmpty()) {
            semanticMatch.and(accom.accomType.in(keywords.getMatchedAccomTypes()));
        }

        for (String token : keywords.getTextTokens()) {
            BooleanBuilder tokenMatch = new BooleanBuilder();
            tokenMatch.or(accom.accomName.containsIgnoreCase(token));
            tokenMatch.or(noSpace(accom.accomName).contains(AccomSearchKeywordUtils.compact(token)));
            if (token.length() >= 2) {
                tokenMatch.or(accom.location.containsIgnoreCase(token));
                tokenMatch.or(accom.accomDetail.containsIgnoreCase(token));
                tokenMatch.or(noSpace(accom.location).contains(AccomSearchKeywordUtils.compact(token)));
                tokenMatch.or(noSpace(accom.accomDetail).contains(AccomSearchKeywordUtils.compact(token)));
            }
            semanticMatch.and(tokenMatch);
        }

        if (semanticMatch.hasValue()) {
            builder.or(semanticMatch);
        }

        return builder.hasValue() ? builder.getValue() : null;
    }

    private StringExpression noSpace(StringExpression source) {
        return Expressions.stringTemplate("replace(coalesce({0}, ''), ' ', '')", source);
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
                        accom.guestCount,
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
