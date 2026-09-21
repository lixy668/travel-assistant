package com.lixinyang.travelassistant.repository;

import com.lixinyang.travelassistant.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUsernameOrderByApplyTimeDesc(String username);

    List<Invoice> findAllByOrderByApplyTimeDesc();

    List<Invoice> findByStatusOrderByApplyTimeDesc(String status);

    boolean existsByOrderId(Long orderId);
}
