package com.flashtract.invoice.model.validation;

import com.flashtract.invoice.model.Contract;
import com.flashtract.invoice.model.User;
import com.flashtract.invoice.model.UserType;
import com.flashtract.invoice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class ContractUserValidator implements Validator {

    private final UserRepository userRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return Contract.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target != null && target instanceof Contract) {
            Contract contract = (Contract) target;
            if (ObjectUtils.isEmpty(contract.getUserId())) {
                return;
            }
            Optional<User> possibleUser = userRepository.findById(contract.getUserId());
            if (possibleUser.isEmpty()) {
                String error = "User ID " + contract.getUserId() + " not found";
                log.error(error);
                errors.rejectValue("userId", "", error);
                return;
            }
            if(possibleUser.get().getUserType() != UserType.Vendor) {
                String error = "Unable to create contract because user is not of type Vendor";
                log.error(error);
                errors.rejectValue("userId", "", error);
            }
        }
    }
}
