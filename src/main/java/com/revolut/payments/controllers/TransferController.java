package com.revolut.payments.controllers;

import com.revolut.payments.dto.RequestDTO;
import com.revolut.payments.dto.ResponseDTO;
import com.revolut.payments.dto.TransferRequestDTO;
import com.revolut.payments.models.Account;
import com.revolut.payments.models.Transfer;
import com.revolut.payments.repositories.AccountRepository;
import com.revolut.payments.repositories.TransferRepository;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

public class TransferController extends Controller {
    private static TransferController instance;
    private AccountRepository accountRepository;
    private TransferRepository transferRepository;

    public TransferController() {
        accountRepository = AccountRepository.getInstance();
        transferRepository = TransferRepository.getInstance();
    }

    @Override
    protected ResponseDTO create(final RequestDTO requestDTO) {
        final TransferRequestDTO transferRequest = new TransferRequestDTO.Builder()
                .setSourceId(String.valueOf(requestDTO.getBodyParam("source")))
                .setTransaction(String.valueOf(requestDTO.getBodyParam("transaction")))
                .setDestinationId(String.valueOf(requestDTO.getBodyParam("destination")))
                .build();
        if (StringUtils.isBlank(transferRequest.getSourceId())
            || StringUtils.isBlank(transferRequest.getTransaction())
            || StringUtils.isBlank(transferRequest.getDestinationId())) {
            return new ResponseDTO.Builder()
                    .setHttpStatus(400)
                    .setBody("Bad Request")
                    .build();
        }
        final Account destinationAccount = accountRepository.fetchOne(Integer.parseInt(transferRequest.getDestinationId()));
        if (destinationAccount == null) {
            return new ResponseDTO.Builder()
                    .setHttpStatus(409)
                    .setBody("Destination account does not exists.")
                    .build();
        }
        final Account sourceAccount = accountRepository.fetchOne(Integer.parseInt(transferRequest.getSourceId()));
        if (sourceAccount == null) {
            return new ResponseDTO.Builder()
                    .setHttpStatus(409)
                    .setBody("Source account does not exists.")
                    .build();
        }
        final double remainingSourceFunds = Double.parseDouble(sourceAccount.getBalance()) - Double.parseDouble(transferRequest.getTransaction());
        if (remainingSourceFunds < 0) {
            return new ResponseDTO.Builder()
                    .setHttpStatus(409)
                    .setBody("Requested transaction exceeds the source account funds.")
                    .build();
        }
        final double updatedDestinationFunds = Double.parseDouble(destinationAccount.getBalance()) + Double.parseDouble(transferRequest.getTransaction());
        final Transfer transfer = transferRepository.create(transferRequest);
        accountRepository.update(new Account.Builder(sourceAccount).setBalance(String.valueOf(remainingSourceFunds)).build());
        accountRepository.update(new Account.Builder(destinationAccount).setBalance(String.valueOf(updatedDestinationFunds)).build());
        return new ResponseDTO.Builder()
                .setHttpStatus(201)
                .setBody(transfer)
                .build();
    }

    @Override
    protected ResponseDTO update(final RequestDTO requestDTO) {
        return null;
    }

    @Override
    protected ResponseDTO getMany(final RequestDTO requestDTO) {
        final TransferRequestDTO transferRequest = new TransferRequestDTO.Builder()
                .addFilter("status", requestDTO.getQueryParam("status"))
                .addFilter("source", requestDTO.getQueryParam("source"))
                .addFilter("destination", requestDTO.getQueryParam("destination"))
                .addFilter("transaction", requestDTO.getQueryParam("transaction"))
                .build();
        final List<Transfer> transfers = transferRepository.fetchMany(transferRequest);
        return new ResponseDTO.Builder()
                .setHttpStatus(200)
                .setBody(transfers)
                .build();
    }

    @Override
    protected ResponseDTO getById(final RequestDTO requestDTO) {
        final Transfer transfer = transferRepository.fetchOne(Integer.parseInt(requestDTO.getUrlParam()));
        if (transfer == null) {
            return new ResponseDTO.Builder()
                    .setHttpStatus(404)
                    .setBody("Resource not found")
                    .build();
        }
        return new ResponseDTO.Builder()
                .setHttpStatus(200)
                .setBody(transfer)
                .build();
    }

    @Override
    protected ResponseDTO deactivate(final RequestDTO requestDTO) {
        final Transfer transfer = transferRepository.fetchOne(Integer.parseInt(requestDTO.getUrlParam()));
        if (transfer == null) {
            return new ResponseDTO.Builder()
                    .setHttpStatus(404)
                    .setBody("Resource not found")
                    .build();
        }
        create(new RequestDTO()
                .addBodyParam("source", transfer.getDestinationId())
                .addBodyParam("transaction", transfer.getTransaction())
                .addBodyParam("destination", transfer.getSourceId()));
        transferRepository.delete(transfer.getId());
        return new ResponseDTO.Builder()
                .setHttpStatus(202)
                .setBody(transfer)
                .build();
    }

    public static TransferController getInstance() {
        if (instance == null) {
            instance = new TransferController();
        }
        return instance;
    }
}
