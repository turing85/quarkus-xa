package de.turing85.quarkus.xa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "number", schema = "public")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Number {
  @Id
  @SequenceGenerator(name = "NumberIdGenerator", sequenceName = "number__seq__id",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NumberIdGenerator")
  @Column(name = "id", nullable = false, updatable = false, unique = true)
  @Setter(AccessLevel.PROTECTED)
  private Long id;

  @Column(name = "value", nullable = false)
  private long value;

  public static Number of(long value) {
    return new Number(null, value);
  }
}
