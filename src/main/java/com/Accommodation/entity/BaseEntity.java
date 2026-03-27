package com.Accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 🧾 BaseEntity
 *
 * ▶ 모든 엔티티의 작성자/수정자 정보를 자동으로 기록해주는 부모 클래스입니다.
 * ▶ 시간 기록 기능이 있는 BaseTimeEntity를 상속받아,
 *    시간 + 작성자/수정자 정보까지 통합적으로 관리합니다.
 */
@EntityListeners(value = {AuditingEntityListener.class})
// 🔹 JPA의 Auditing 기능을 사용할 수 있게 설정합니다.
//     → 엔티티 저장/수정 시 자동으로 createdBy, modifiedBy 값이 채워집니다.

@MappedSuperclass
// 🔹 이 클래스는 테이블로 생성되지 않고, 자식 클래스에 필드만 상속됩니다.

@Getter
public class BaseEntity extends BaseTimeEntity  {
                         // 🧭 BaseTimeEntity를 상속 → 생성 시간(regTime) / 수정 시간(updateTime) 자동 기록 기능 포함됨
        /**
         * ✍️ 최초 작성자 (사용자 ID 또는 이름)
         *
         * @CreatedBy 어노테이션을 통해, 엔티티가 처음 생성(persist)될 때 자동으로 작성자 정보가 설정됩니다.
         * 예: "admin", "user1" 등
         *
         * - @Column(updatable = false) 설정으로 인해, 저장 이후에는 변경되지 않도록 막습니다.
         * - 일반적으로 Spring Security의 로그인 사용자 이름이 들어갑니다.
         *
         * ⚠️ 동작을 위해 반드시 AuditorAware 구현한 자식 객체가 있어야 합니다!
         */
        @CreatedBy
        @Column(updatable = false)
        private String createBy; //최조 작성자  ID   또는 이름
        /**
         * ✏️ 최종 수정자
         *
         * @LastModifiedBy 어노테이션을 통해, 엔티티가 수정될 때마다 자동으로 수정자 정보가 갱신됩니다.
         * 예: 관리자나 사용자 ID 등
         *
         * - 수정이 발생할 때마다 현재 사용자 정보가 이 필드에 저장됩니다.
         */
        @LastModifiedBy
        private String modifiedBy; //최조 수정자 ID  또는 이음

}
