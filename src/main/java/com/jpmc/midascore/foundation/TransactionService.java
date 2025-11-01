package com.jpmc.midascore.foundation;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    
    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    
    public TransactionService(UserRepository userRepository, 
                            TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
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
        
        // All validations passed - process the transaction
        // Update balances by creating new Balance objects
        Balance newSenderBalance = new Balance(senderBalance.getAmount() - amount);
        Balance newRecipientBalance = new Balance(recipient.getBalance() + amount);
        
        sender.setBalance(newSenderBalance.getAmount());
        recipient.setBalance(newRecipientBalance.getAmount());
        
        // Save updated users
        userRepository.save(sender);
        userRepository.save(recipient);
        
        // Create and save transaction record
        TransactionRecord record = new TransactionRecord(
            sender,
            recipient,
            amount
        );
        transactionRecordRepository.save(record);
        
        return true;
    }
}