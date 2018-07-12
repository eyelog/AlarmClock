package ru.eyelog.alarmclock.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Приёмник для действия "com.android.vending.billing.PURCHASES_UPDATED" из Play store.
 * Возможно, что внутрипрограммная покупка может быть произведена без вызова метода getBuyIntent(),
 * например с помощью промо-кода. Если само приложение в этот момент не было запущено, то когда оно
 * будет запущено, с помощью метода getPurchases(), будет достаточно уведомления.
 * А если приложение, во время приобритения работает в фоновом режиме сообщение для данного приёмника
 * будет отображено как объект покупки.
 */
public class IabBroadcastReceiver extends BroadcastReceiver {

    // Слушатель для приёма посылок.
    public interface IabBroadcastListener {
        void receivedBroadcast();
    }

     // The Intent action that this Receiver should filter for.
    public static final String ACTION = "com.android.vending.billing.PURCHASES_UPDATED";

    private final IabBroadcastListener mListener;

    public IabBroadcastReceiver(IabBroadcastListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mListener != null) {
            mListener.receivedBroadcast();
        }
    }
}
