package com.admin.rechargeapp;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.verifone.commerce.CommerceConstants;
import com.verifone.commerce.CommerceEvent;
import com.verifone.commerce.CommerceListener;
import com.verifone.commerce.entities.Basket;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Payment;
import com.verifone.commerce.payment.BasketEvent;
import com.verifone.commerce.entities.Transaction;
import com.verifone.commerce.payment.BasketManager;
import com.verifone.commerce.payment.PaymentCompletedEvent;
import com.verifone.commerce.payment.TransactionEndedEvent;
import com.verifone.commerce.payment.TransactionEvent;
import com.verifone.commerce.payment.TransactionManager;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by admin.
 */



public class PaymentTerminal {

    private static PaymentTerminal instance;
    private static String TAG = "PaymentTerminal";
    private TransactionManager transactionManager;
    private CommerceListener commerceListener;
    private BridgeEvents listener;
    private BasketManager mBasketManager;
    protected PaymentTerminal() {

    }

    public static PaymentTerminal getInstance() {
        if (instance == null) {
            instance = new PaymentTerminal();
        }
        return instance;
    }

    public void startBridge(final Context context) {
        transactionManager = TransactionManager.getTransactionManager(context);
        //Use MODE_SIMULATOR for Simulator or MODE_DEVICE for payment terminal

        if (transactionManager == null) {
            //PaymentTerminal has not finished, so we need to wait
            final CountDownTimer countDownTimer = new CountDownTimer(100, 5) {
                @Override
                public void onTick(long l) {
                    transactionManager = TransactionManager.getTransactionManager(context);
                    if (transactionManager != null) {
                        transactionManager.setDebugMode(CommerceConstants.MODE_STUBS_DEBUG);
                        this.cancel();
                    }
                }

                @Override
                public void onFinish() {

                }
            };
            countDownTimer.start();
        } else {
            transactionManager.setDebugMode(CommerceConstants.MODE_STUBS_DEBUG);
        }

        //Now we set the listener
        //For possible commerceEvent values, check the documentation

        if (commerceListener == null) {
            commerceListener = new CommerceListener() {
                @Override
                public CommerceEvent.Response handleEvent(CommerceEvent event) {
                    //Session events
                    Log.d("Bridge event: ", event.getType());
                    if (event.getType().equals(CommerceEvent.SESSION_STARTED)) {
                        if (listener != null) {
                            listener.sessionStarted();
                            mBasketManager = null;
                        }
                    }
                    if (event.getType().equals(CommerceEvent.SESSION_CLOSED)) {
                        if (listener != null) {
                            listener.sessionStopped();
                        }
                    }

                    //basket events
                    if (event.getType().equals(BasketEvent.TYPE)) {

                        BasketEvent basketEvent = (BasketEvent) event;
                        if (basketEvent.getBasketAction() == BasketEvent.BasketAction.ADDED) {
                            listener.merchandiseAdded();
                        } else {
                            if (basketEvent.getBasketAction() == BasketEvent.BasketAction.REMOVED) {
                                //Not supported in ManualTransaction
                                listener.merchandiseDeleted();
                            } else {

                                listener.merchadiseUpdated();
                            }
                        }

                    }

                //payment events

                    if (event.getType().equals(TransactionEvent.TRANSACTION_PAYMENT_COMPLETED)) {
                        PaymentCompletedEvent paymentCompletedEvent = (PaymentCompletedEvent) event;
                        CommerceEvent.Response paymentInfo = paymentCompletedEvent.generateResponse();
                        Payment payment = paymentCompletedEvent.getPayment();
                        if (listener != null) {

                            if (payment.getAuthResult() == Payment.AuthorizationResult.AUTHORIZED_ONLINE ||
                                    payment.getAuthResult() == Payment.AuthorizationResult.AUTHORIZED_OFFLINE) {
                                listener.onSuccess(payment);
                            } else if (payment.getAuthResult() == Payment.AuthorizationResult.REJECTED_ONLINE ||
                                    payment.getAuthResult() == Payment.AuthorizationResult.REJECTED_OFFLINE) {
                                listener.onDeclined();
                            } else {
                                listener.onFailure();
                            }
                        }
                    }


                    if (event.getType().equals(TransactionEvent.SESSION_ENDED)) {
                        listener.sessionStopped();
                    }


                    return event.generateResponse();
                }
            };
        }
    }

    public void setEventsListener(BridgeEvents e) {
        this.listener = e;
    }

    public void startSession() {
        if (transactionManager == null) {
                if (transactionManager != null) {
                    transactionManager.startSession(commerceListener);
                }
        } else {
            transactionManager.startSession(commerceListener);
        }
    }

    public void stopSession() {
        transactionManager.endSession();
    }

    public void startBasket() {
        if (mBasketManager == null) {
            mBasketManager = basket();
        }
        mBasketManager.registerBasket(new Basket());
        listener.basketStarted();
    }

    public BasketManager basket() {
        BasketManager basket = null;
        if (mBasketManager == null) {
            basket = transactionManager.getBasketManager();
        } else {
            basket = mBasketManager;
        }
        return basket;
    }

    public void addMerchandise(Merchandise merchandise) {
        if (mBasketManager == null) {
            mBasketManager = basket();
        }
        mBasketManager.addMerchandise(merchandise);
    }

    public void updateMerchandise(Merchandise merchandise) {
        mBasketManager.modifyMerchandise(merchandise);
    }

    public void deleteMerchandise(Merchandise merchandise) {
        mBasketManager.removeMerchandise(merchandise);
    }

    public void finalizeBasket() {
//        mBasketManager.finalizeBasket();
        listener.basketFinalized();
    }

    public String currentTotal() {
        Transaction transaction = transactionManager.getTransaction();
        java.math.BigDecimal currenTotal = transaction.getAmount();

        return String.format("%.2f", currenTotal.floatValue());
    }

    public void startPayment() {
        Payment payment = new Payment();
        payment.setPaymentAmount(transactionManager.getTransaction().getAmount());
        transactionManager.startPayment(payment);

    }

    public interface BridgeEvents {
        public void sessionStarted();
        public void sessionStopped();
        public void basketStarted();
        public void merchandiseAdded();
        public void merchadiseUpdated();
        public void merchandiseDeleted();
        public void basketFinalized();
        public void onSuccess(Payment payment);
        public void onCancel();
        public void onDeclined();
        public void onFailure();

    }

}
