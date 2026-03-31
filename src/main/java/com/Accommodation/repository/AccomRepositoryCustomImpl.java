package com.Accommodation.repository;

import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;

import com.Accommodation.dto.QMainAccomDto;
import com.Accommodation.entity.QAccom;
import com.Accommodation.entity.QAccomImg;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.util.StringUtils;

import java.util.List;

public class AccomRepositoryCustomImpl implements AccomRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public AccomRepositoryCustomImpl(EntityManager em){

        this.queryFactory = new JPAQueryFactory(em);
    }

    private BooleanExpression accomNmLike(String searchQuery){

        return StringUtils.isEmpty(searchQuery)
                ?null
                : QAccom.accom.accomNm.like("%"+searchQuery+"%");

    }

    @Override
    public Page<MainAccomDto> getMainAccomPage(AccomSearchDto accomSearchDto, Pageable pageable) {

        QAccom accom = QAccom.accom;
        QAccomImg accomImg = QAccomImg.accomImg;

        List<MainAccomDto> content = queryFactory.select( new QMainAccomDto(
                accom.id,
                accom.accomNm,
                accom.price,
                accom.stars,
                accom.accomDetail,
                accom.starRating,
                accomImg.imgName
                )
        )
                .from(accomImg)
                .join(accomImg.accom, accom)
                .where(accomImg.repimgYn.eq("Y"))
                .where(accomNmLike(accomSearchDto.getSearchQuery()))
                .orderBy(accom.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(Wildcard.count)
                .from(accomImg)
                .join(accomImg.accom, accom)
                .where(accomImg.repimgYn.eq("Y"))
                .where(accomNmLike(accomSearchDto.getSearchQuery()))
                .fetchOne();




        return new PageImpl<>(content, pageable, total);
    }
}
