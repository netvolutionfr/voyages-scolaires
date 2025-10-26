package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"parent_id","child_id"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class ParentChild {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq", allocationSize = 50)
    Long id;

    @ManyToOne(optional=false) @JoinColumn(name="parent_id")
    private User parent; // role = PARENT

    @ManyToOne(optional=false) @JoinColumn(name="child_id")
    private User child;
}