package com.jpmc.midascore.foundation;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final RestTemplate restTemplate;

    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.restTemplate = new RestTemplate(); // Create RestTemplate instance
    }

    @Transactional
    public boolean processTransaction(Transaction transaction) {
        // Validate and process the transaction
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        // Check if sender and recipient are valid
        if (sender == null || recipient == null) {
            return false; // Invalid sender or recipient
        }

        float amount = transaction.getAmount();
        Balance transactionBalance = new Balance(amount);

        // Check if sender has sufficient balance
        Balance senderBalance = new Balance(sender.getBalance());
        if (senderBalance.isLessThan(transactionBalance)) {
            return false; // Insufficient balance
        }

        // ✅ Send transaction to Incentive API
        Incentive incentive = null;
        try {
            ResponseEntity<Incentive> response = restTemplate.postForEntity(
                    "http://localhost:8080/incentive",
                    transaction,
                    Incentive.class
            );
            incentive = response.getBody();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to fetch incentive: " + e.getMessage());
            incentive = new Incentive();
            incentive.setAmount(0);
        }

        double incentiveAmount = (incentive != null) ? incentive.getAmount() : 0;

        // ✅ Update balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + (float) incentiveAmount);

        // Save updated users
        userRepository.save(sender);
        userRepository.save(recipient);

        // Create and save transaction record (store incentive too if your entity supports it)
        TransactionRecord record = new TransactionRecord(
                sender,
                recipient,
                amount
        );
        transactionRecordRepository.save(record);

        return true;
    }
}
