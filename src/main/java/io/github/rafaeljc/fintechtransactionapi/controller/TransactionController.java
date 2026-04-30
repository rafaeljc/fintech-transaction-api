package io.github.rafaeljc.fintechtransactionapi.controller;

import io.github.rafaeljc.fintechtransactionapi.dto.TransactionRequest;
import io.github.rafaeljc.fintechtransactionapi.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Void> post(@Valid @RequestBody TransactionRequest request) {
        transactionService.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        transactionService.clear();
        return ResponseEntity.ok().build();
    }
}
