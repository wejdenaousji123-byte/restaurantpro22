package com.restaurantpro.repository;

import com.restaurantpro.model.Notification;
import com.restaurantpro.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireAndLueFalseOrderByHeureCreationDesc(Utilisateur dest);
    List<Notification> findByDestinataireOrderByHeureCreationDesc(Utilisateur dest);
    long countByDestinataireAndLueFalse(Utilisateur dest);
}
