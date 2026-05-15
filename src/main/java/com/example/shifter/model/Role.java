package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * This class represents the role entity/model on the shifter database
 * This entity is used to define the roles of the application users
 * which later on will allow specific data access to each role user.
 */

@Entity
@Table (name = "roles")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor

/**
 * @Getter, @Setter, @NoArgsConstructor and @RequiredArgsConstructor
 * are from lombok library. It creates the getters/setters and constructors
 * automatically without the need to hard coding then and reducing boiler
 * plates.
 **/

public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (length = 20, unique = true, nullable = false)
    @NonNull
    private String name;

}
