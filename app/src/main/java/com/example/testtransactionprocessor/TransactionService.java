package com.example.testtransactionprocessor;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.UUID;

import co.poynt.api.model.AdjustTransactionRequest;
import co.poynt.api.model.EMVData;
import co.poynt.api.model.Processor;
import co.poynt.api.model.ProcessorResponse;
import co.poynt.api.model.ProcessorStatus;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCheckCardListener;
import co.poynt.os.services.v1.IPoyntTransactionService;
import co.poynt.os.services.v1.IPoyntTransactionServiceListener;

public class TransactionService extends Service {
    private final String TAG = getClass().getName();
    public TransactionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPoyntTransactionService.Stub mBinder = new IPoyntTransactionService.Stub(){

        /*
            Currently not supported
         */
        @Override
        public void createTransaction(co.poynt.api.model.Transaction transaction, String requestId,
                                      IPoyntTransactionServiceListener callback) throws RemoteException {

        }


        // transactions with TransactionAction.AUTHORIZE and TransactionAction.SALE
        // go to processTransaction
        @Override
        public void processTransaction(Transaction transaction, String requestId,
                                       IPoyntTransactionServiceListener callback) throws RemoteException {
            showToast("From processTransaction");
            Log.d(TAG, "From processTransaction");

            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID());
            }
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(Calendar.getInstance());
            }
            if (transaction.getUpdatedAt() == null) {
                transaction.setUpdatedAt(Calendar.getInstance());
            }
            // add processor response (HAPPY PATH)
            ProcessorResponse processorResponse = new ProcessorResponse();
//        processorResponse.setApprovalCode("123456");
//        processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
//        processorResponse.setStatus(ProcessorStatus.Successful);
//        processorResponse.setStatusMessage("Approved");

            // add processor response (UNHAPPY PATH)
            //processorResponse.setApprovalCode("123456");
//        processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
//        processorResponse.setStatus(ProcessorStatus.Failure);
//        processorResponse.setStatusMessage("Processor decline. Over the limit");

            processorResponse.setTransactionId(transaction.getId().toString());
            processorResponse.setAcquirer(Processor.MOCK);
            processorResponse.setProcessor(Processor.MOCK);
            transaction.setProcessorResponse(processorResponse);

            // remove track data and PAN (for now it's actually required)
//        transaction.getFundingSource().getCard().setTrack1data(null);
//        transaction.getFundingSource().getCard().setTrack2data(null);
//        transaction.getFundingSource().getCard().setTrack3data(null);
//        transaction.getFundingSource().getCard().setNumber(null);

            // temporary workaround to get transaction
            transaction.getFundingSource().getCard().setExpirationYear(2020);
            transaction.getFundingSource().getCard().setExpirationMonth(12);


            if (transaction.getAction() == TransactionAction.AUTHORIZE) {

                // hardcoding a specific amount to trigger a decline
                if (transaction.getAmounts().getOrderAmount() == 456){
                    transaction.setStatus(TransactionStatus.DECLINED);
                    processorResponse.setStatus(ProcessorStatus.Failure);
                    processorResponse.setStatusMessage("Processor decline. Over the limit");

                }else {
                    transaction.setStatus(TransactionStatus.AUTHORIZED);
                    processorResponse.setApprovalCode("123456");
                    processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
                    processorResponse.setStatus(ProcessorStatus.Successful);
                    processorResponse.setStatusMessage("Approved");
                }

            } else {
                transaction.setStatus(TransactionStatus.CAPTURED);
            }
            try {
                callback.onResponse(transaction, requestId, null);
            } catch (RemoteException e) {
                e.printStackTrace();
                PoyntError poyntError = new PoyntError();
                poyntError.setCode(PoyntError.CARD_DECLINE);
                try {
                    callback.onResponse(transaction, requestId, poyntError);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }


        }

        @Override
        public void voidTransaction(String s, EMVData emvData, String requestId,
                                    IPoyntTransactionServiceListener callback) throws RemoteException {
            showToast("From voidTransaction");
        }

        @Override
        public void captureTransaction(String transactionId, co.poynt.api.model.AdjustTransactionRequest captureRequest,
                                       String requestId, IPoyntTransactionServiceListener callback) throws RemoteException {
            showToast("From captureTransaction");
        }

        // Captures all authorized transactions through Poynt Cloud
        @Override
        public void captureAllTransactions(String requestId) throws RemoteException {
            showToast("From captureAllTransactions");
        }

        //Updates the given transaction in the Poynt Cloud and updates local Poynt Transaction content provider.
        @Override
        public void updateTransaction(String transactionId, AdjustTransactionRequest updateRequest, String requestId,
                                      IPoyntTransactionServiceListener callback) throws RemoteException {
            showToast("from updateTransaction");
        }

        @Override
        public void reverseTransaction(String originalRequestId, String originalTransactionId, EMVData emvData, String requestId) throws RemoteException {
            showToast("from reverseTransaction");
        }

        @Override
        public void getTransaction(String transactionId, String requestId, IPoyntTransactionServiceListener callback)
                throws RemoteException {
            showToast("from getTransaction");
        }

        // currently not supported
        @Override
        public void saveTransaction(Transaction transaction, String requestId) throws RemoteException {
            showToast("From saveTransaction");
        }

        // reserved
        @Override
        public void checkCard(Payment payment, String serviceCode, String cardHolderName, String expirationDate,
                              String last4, String binRange, String AID, String applicationLabel, String panSequenceNumber,
                              String issuerCountryCode, String encryptedPAN, IPoyntCheckCardListener callback) throws RemoteException {
            showToast("From checkCard");
        }

        private void showToast(final String message) {
            Handler handler = new Handler(getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TransactionService.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
}
