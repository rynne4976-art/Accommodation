package com.Accommodation.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    보통은 테이블에 등록일, 수정일, 등록자, 수정자를 모두  다 넣어 주지만
    어떤 ~ 다른 테이블은 등록자, 수정자를 넣지 않고  등록일, 수정일 만 저장하는 테이블도 있을수 있습니다.

    그런 등록일, 수정일 만 저장하는 엔티티(테이블)는 BaseTimeEntity 클래스만 상속받아 사용할수 있도록 BaseTimeEntity 클래스 선언
     - JPA Auditing 기능을 사용해서 엔티티의 생성 시간(regTime)과 수정 시간(updateTime)을 자동으로 기록해주는 공통 부모 클래스입니다

 * 🕒 BaseTimeEntity: JPA 엔티티들의 '생성 시간'과 '수정 시간'을 자동으로 관리하는 부모 클래스입니다.
 *
 * ✅ 공통 기능을 여기서 정의하고,
 * ✅ 실제 엔티티 클래스(Item, Order, Member 등)는 이 클래스를 상속만 하면 됩니다.
 *
 * 👉 즉, 모든 엔티티에 '자동으로 시간 기록' 기능이 들어가게 됩니다!
 *
 * 예: 상품이 처음 DB에 저장되면 regTime이 자동 기록되고,
 *     수정될 때마다 updateTime이 갱신됩니다.
 */
@EntityListeners(value = {AuditingEntityListener.class})
// 🔹 JPA 이벤트(생성/수정 등)가 발생할 때 Auditing 기능이 작동되도록 설정합니다.
//    → 내부적으로 regTime/updateTime을 자동 설정해주는 리스너 클래스가 작동됩니다.

@MappedSuperclass
// 🔹 이 클래스는 "테이블과 직접 매핑되지 않고", 하위 엔티티 클래스에 필드만 상속됩니다.
//    → DB에는 BaseTimeEntity라는 테이블이 생기지 않음!
//    → 대신, 자식 클래스 테이블에 regTime, updateTime 컬럼이 생성됩니다.

@Getter @Setter   //Lombok.jar라이브러리 파일에서 제공하는 getter, setter 자동으로 만들어 주는 어노테이션
public class BaseTimeEntity {

    /**
     * ⏰ 엔티티가 처음 저장될 때 자동으로 기록되는 "생성 시간"
     *
     * 예) 상품이 처음 등록되었을 때 → regTime에 현재 시간이 자동 저장됨
     *
     * @CreatedDate: persist() 시점에 값이 자동 생성됨
     * @Column(updatable = false): 이후에 수정되지 않도록 막음
     */
       @CreatedDate
       @Column(updatable = false)
       private LocalDateTime regTime;


    /**
     * 🔄 엔티티가 수정될 때마다 자동으로 갱신되는 "수정 시간"
     *
     * 예) 상품 정보가 변경되었을 때 → updateTime에 현재 시간이 자동으로 반영됨
     *
     * @LastModifiedDate: merge() 또는 flush() 시점에 현재 시간으로 갱신됨
     */
      @LastModifiedDate
       private LocalDateTime updateTime;

    /*
     * 🔒 사용 전 필수 조건!
     * 이 Auditing 기능을 활성화하려면 다음 설정이 반드시 필요합니다:
     *
     * 1️⃣ @EnableJpaAuditing 어노테이션을 메인 클래스나 설정 public class AuditConfig 클래스에 붙여야 함
     *
     * 예:
     * @Configuration
     * @EnableJpaAuditing
     * public class AuditConfig  {}
     *
     * 2️⃣ Spring Boot의 시간 설정이 반드시 LocalDateTime과 호환되어야 함
     */

}
