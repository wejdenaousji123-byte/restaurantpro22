package com.restaurantpro.repository;

import com.restaurantpro.model.Addition;
import com.restaurantpro.model.TableRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdditionRepository extends JpaRepository<Addition, Long> {
    Optional<Addition> findByTable(TableRestaurant table);
    Optional<Addition> findByTableAndStatut(TableRestaurant table, Addition.Statut statut);
}
