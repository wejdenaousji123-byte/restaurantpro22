package com.restaurantpro.repository;

import com.restaurantpro.model.TableRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TableRestaurantRepository extends JpaRepository<TableRestaurant, Long> {
    List<TableRestaurant> findByStatut(TableRestaurant.Statut statut);
    Optional<TableRestaurant> findByNumero(Integer numero);
    boolean existsByNumero(Integer numero);
}
