package com.jpmc.midascore.kafka;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.foundation.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
// import java.util.Optional;

@Service
public class TransactionListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @KafkaListener(topics = "transactions", groupId = "midas-core")
    public void processTransaction(Transaction txn) {
        UserRecord senderOpt = userRepository.findById(txn.getSenderId());
        UserRecord recipientOpt = userRepository.findById(txn.getRecipientId());

        if (senderOpt==null || recipientOpt==null) {
            return; // invalid sender or recipient
        }

        UserRecord sender = senderOpt;
        UserRecord recipient = recipientOpt;

        if (sender.getBalance() >= txn.getAmount()) {
            sender.setBalance(sender.getBalance() - txn.getAmount());
            recipient.setBalance(recipient.getBalance() + txn.getAmount());

            userRepository.save(sender);
            userRepository.save(recipient);

            TransactionRecord record = new TransactionRecord(sender, recipient, txn.getAmount());
            transactionRecordRepository.save(record);
        }
        // else discard silently
    }
}
