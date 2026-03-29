package com.cuutrominhbach.service;

import com.cuutrominhbach.dto.response.TransactionStatementResponse;
import com.cuutrominhbach.entity.TransactionHistory;
import com.cuutrominhbach.entity.User;
import com.cuutrominhbach.repository.TransactionHistoryRepository;
import com.cuutrominhbach.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionHistoryRepository txRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionHistoryRepository txRepository, UserRepository userRepository) {
        this.txRepository = txRepository;
        this.userRepository = userRepository;
    }

    public List<TransactionStatementResponse> getHistory(Long viewingUserId, boolean isAdmin) {
        List<TransactionHistory> txs;
        if (isAdmin) {
            txs = txRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            txs = txRepository.findByUserId(viewingUserId);
        }

        // 1. Thu thập tất cả userId cần thiết
        Set<Long> userIds = new HashSet<>();
        for (TransactionHistory tx : txs) {
            if (tx.getFromUserId() != null) userIds.add(tx.getFromUserId());
            if (tx.getToUserId() != null) userIds.add(tx.getToUserId());
        }

        // 2. Fetching Map tại RAM (N+1 Query Optimization)
        Map<Long, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFullName() != null ? u.getFullName() : (u.getUsername() != null ? u.getUsername() : "Ẩn danh")));

        // 3. Map to DTO
        return txs.stream().map(tx -> {
            TransactionStatementResponse res = new TransactionStatementResponse();
            res.setId(tx.getId());
            res.setType(tx.getType() != null ? tx.getType().name() : "N/A");
            res.setEventType(resolveEventType(tx.getType(), tx.getNote(), tx.getBatchId()));
            res.setAmount(tx.getAmount() != null ? tx.getAmount() : 0L);
            res.setNote(tx.getNote());
            res.setTxHash(tx.getTxHash());
            res.setCreatedAt(tx.getCreatedAt());

            res.setFromName(tx.getFromUserId() != null ? userNames.getOrDefault(tx.getFromUserId(), "Hệ thống / Quỹ") : "Hệ thống / Quỹ");
            res.setToName(tx.getToUserId() != null ? userNames.getOrDefault(tx.getToUserId(), "Hệ thống / Quỹ") : "Hệ thống / Quỹ");

            if (isAdmin) {
                // Admin xem trung lập, không bị đổi màu
                res.setIsPlus(null);
            } else {
                // User xem: tiền về ví là Plus, tiền ra khỏi ví là Minus
                if (viewingUserId.equals(tx.getToUserId())) {
                    res.setIsPlus(true);
                } else if (viewingUserId.equals(tx.getFromUserId())) {
                    res.setIsPlus(false);
                } else {
                    res.setIsPlus(tx.getType() == TransactionHistory.TxType.IN);
                }
            }

            return res;
        }).collect(Collectors.toList());
    }

    private String resolveEventType(TransactionHistory.TxType type, String note, Long batchId) {
        if (type == null) return "UNKNOWN";

        if (type == TransactionHistory.TxType.DONATE)           return "Gửi Quyên Góp";
        if (type == TransactionHistory.TxType.ALLOCATE_ESCROW)  return "Khoá Quỹ Lô";
        if (type == TransactionHistory.TxType.RECEIVE_RELIEF)   return "Nhận Viện Trợ";
        if (type == TransactionHistory.TxType.PAY_SHOP)         return "Thanh Toán Shop";
        if (type == TransactionHistory.TxType.AIRDROP)          return "Được Airdrop";
        if (type == TransactionHistory.TxType.WITHDRAW)         return "Rút Tiền Sinh Hoạt";

        if (note == null || note.isBlank()) return type.name();
        String n = note.trim().toLowerCase();
        if (n.startsWith("nạp tiền"))           return "Nạp Token";
        if (n.startsWith("nhận cứu trợ"))       return "Nhận Viện Trợ";
        return type.name();
    }
}
