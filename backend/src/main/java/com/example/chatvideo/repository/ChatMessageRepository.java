package com.example.chatvideo.repository;

import com.example.chatvideo.model.AgencyEntity;
import com.example.chatvideo.model.CustomerEntity;
import com.example.chatvideo.model.MessageEntity;
import com.example.chatvideo.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<MessageEntity, Integer> {

    List<MessageEntity> findByAgencyAndCustomerOrderByTimestampDesc(AgencyEntity agency, CustomerEntity customer);
    List<MessageEntity> findBySender(UserEntity sender);

    @Query("SELECT m FROM MessageEntity m " +
            "WHERE m.agency = :agency " +
            "AND m.customer = :customer " +
            "AND m.timestamp >= :since")
    List<MessageEntity> findRecentMessages(AgencyEntity agency, CustomerEntity customer, LocalDateTime since);
    
    void deleteByTimestampBefore(LocalDateTime timestamp);
    long countByAgencyAndCustomer(AgencyEntity agency,CustomerEntity customer);
}
