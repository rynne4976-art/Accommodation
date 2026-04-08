package com.Accommodation.repository;

import com.Accommodation.dto.WishListDto;
import com.Accommodation.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByMemberEmailAndAccomId(String email, Long accomId);

    Optional<Wish> findByMemberEmailAndAccomId(String email, Long accomId);

    long countByMemberEmail(String email);

    @Query("""
            select new com.Accommodation.dto.WishListDto(
                a.id,
                a.accomName,
                a.accomType,
                a.grade,
                a.accomDetail,
                a.location,
                a.pricePerNight,
                coalesce(
                    (select rep.imgUrl
                     from AccomImg rep
                     where rep.id = (
                         select min(repCandidate.id)
                         from AccomImg repCandidate
                         where repCandidate.accom = a
                           and repCandidate.repImgYn = 'Y'
                     )),
                    (select fallback.imgUrl
                     from AccomImg fallback
                     where fallback.id = (
                         select min(fallbackCandidate.id)
                         from AccomImg fallbackCandidate
                         where fallbackCandidate.accom = a
                     )),
                    ''
                ),
                coalesce((select avg(r.rating) from Review r where r.accom = a), 0.0),
                (select count(r) from Review r where r.accom = a)
            )
            from Wish w
            join w.accom a
            where w.member.email = :email
            order by w.regTime desc
            """)
    java.util.List<WishListDto> findWishListDtosByMemberEmailOrderByRegTimeDesc(@Param("email") String email);
}
