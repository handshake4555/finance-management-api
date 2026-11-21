package org.example.finance.service;

import org.example.finance.model.FinancialAccount;
import org.example.finance.repo.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {
    private static final List<String> TYPES = List.of("SAVINGS", "CURRENT", "CASH", "CREDIT_CARD", "OTHER");
    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public List<FinancialAccount> list(String owner) {
        return accounts.findByOwnerAndActiveTrue(owner);
    }

    public FinancialAccount get(String owner, Long id) {
        return accounts.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    public FinancialAccount create(String owner, String name, String type, String currency, BigDecimal opening) {
        validateType(type);
        if (opening != null && opening.signum() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opening balance cannot be negative");
        return accounts.save(new FinancialAccount(owner, name, type, currency == null ? "INR" : currency, opening == null ? BigDecimal.ZERO : opening));
    }

    @Transactional
    public FinancialAccount update(String owner, Long id, String name, String type, String currency) {
        validateType(type);
        var account = accounts.lockOwned(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        account.name = name;
        account.type = type;
        if (currency != null) account.currency = currency;
        return account;
    }

    @Transactional
    public void deactivate(String owner, Long id) {
        accounts.lockOwned(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")).active = false;
    }

    @Transactional
    public FinancialAccount lockOwned(String owner, Long id) {
        return accounts.lockOwned(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private void validateType(String type) {
        if (!TYPES.contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported account type");
    }
}
