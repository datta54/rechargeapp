package com.admin.rechargeapp;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.verifone.commerce.entities.Merchandise;
import com.verifone.commerce.entities.Payment;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Recharge Application</h1>
 * Recharge app is an example for the Commerce Platform Payment API.
 * <p>
 * MainActivity class handles all the UI changes and the Interface methods implementation
 * which are declared in PaymentTerminal class.
 *
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private PaymentTerminal bridge = null;
    private PaymentTerminal.BridgeEvents paymentListener = null;
    private boolean terminalOpen = false;
    private boolean mBasketOpen = false;
    private Merchandise mCurrentMerchandise = null;
    private List<Merchandise> items;
    private MerchandiseArrayAdapter<Merchandise> adapter;
    private ListView mMerchandiseListView;
    Button addMerchandise = null;
    Button startPayment = null;
    EditText quantity = null;
    EditText description = null;
    EditText price = null;
    TextView payStatus,tvCancelTransaction;
    String TAG="MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addMerchandise = (Button) findViewById(R.id.buttonAdd);
        startPayment=(Button)findViewById(R.id.btnPay);
        payStatus = (TextView) findViewById(R.id.textViewPayStatus);
        tvCancelTransaction=(TextView)findViewById(R.id.tvCancelTransaction);
        addMerchandise.setEnabled(true);
        startPayment.setEnabled(false);
        tvCancelTransaction.setEnabled(false);
        quantity = (EditText) findViewById(R.id.editTextQty);
        quantity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    description.requestFocus();
                    return true;
                }
                return false;
            }
        });
        description = (EditText) findViewById(R.id.editTextDescription);
        description.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    price.requestFocus();
                    return true;
                }
                return false;
            }
        });
        price = (EditText) findViewById(R.id.editTextPrice);
        mMerchandiseListView = (ListView) findViewById(R.id.merchandiseList);
    }
    @Override
    protected void onResume() {
        super.onResume();
        initBridge();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (terminalOpen) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bridge.stopSession();
                    addMerchandise.setEnabled(true);
                    startPayment.setEnabled(false);
                    payStatus.setVisibility(View.GONE);
                    for (int i = 0; i < items.size(); i++) {
                        Merchandise merchandise = items.get(i);
                        removeMerchandise(merchandise);
                    }
                    items.clear();
                    mMerchandiseListView.setAdapter(null);
                    cleanFields();
                }
            });
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (terminalOpen) {
            bridge.stopSession();
        }
    }
    /**   initBridge() is method where all the Interface method implementation are there.  */
    private void initBridge() {
        bridge = PaymentTerminal.getInstance();
        bridge.startBridge(this);
        openSession();
        final Context context = MainActivity.this;

        paymentListener = new PaymentTerminal.BridgeEvents() {
            /**  this method call when session is started and then call startBasket() method. */
            @Override
            public void sessionStarted() {
                terminalOpen = true;
                mBasketOpen = false;
                Log.d(TAG, "Session Started");
                startBasket();
            }
            /**  this method call when session is stopped.  */
            @Override
            public void sessionStopped() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                terminalOpen = false;
                startPayment.setEnabled(false);
                    }
                });
            }
            /**
             *   this method call when basket is started and then here we assign
             *   some UI Listview and arrayadapter.
             */
            @Override
            public void basketStarted() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBasketOpen = true;
                        mMerchandiseListView.setVisibility(View.VISIBLE);
                        items = new ArrayList<Merchandise>();
                        adapter = new MerchandiseArrayAdapter<Merchandise>(MainActivity.this, items);
                        addMerchandise();
                    }
                });
            }
            /**
             *   this method call when merchandise added to basket and then here we assign
             *   some UI Listview and arrayadapter. So we can see the records on UI.
             */
            @Override
            public void merchandiseAdded() {
                if (mCurrentMerchandise != null) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startPayment.setEnabled(true);
                            items.add(mCurrentMerchandise);
                            adapter = new MerchandiseArrayAdapter<Merchandise>(MainActivity.this, items);
                            mMerchandiseListView.setAdapter(adapter);
                            cleanFields();
                            mCurrentMerchandise = null;
                            bridge.currentTotal();
                            payStatus.setVisibility(View.VISIBLE);
                            payStatus.setText("Swipe, Tap or Insert Card");
                        }
                        });
                }
            }

            @Override
            public void merchadiseUpdated() {

            }
            @Override
            public void onCancel() {
                Log.d(TAG, "on  Cancel");
            }
            /**
             *   this method call when we delete the added merchandise from basket.
             */
            @Override
            public void merchandiseDeleted() {
                if (mCurrentMerchandise != null) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            items.remove(mCurrentMerchandise);
                            adapter = new MerchandiseArrayAdapter<Merchandise>(MainActivity.this, items);
                            mMerchandiseListView.setAdapter(adapter);
                            mCurrentMerchandise = null;
                            cleanFields();
                        }
                    });
                }
            }
            /**   this method call when basket is finalized  */
            @Override
            public void basketFinalized() {
                mBasketOpen = false;
            }

           /* @Override
            public void onCancel() {
                Log.d(TAG, "on  Cancel");
            }
*/
            @Override
            public void onSuccess(Payment payment) {
                Log.d(TAG, "on Success ");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMerchandise.setEnabled(true);
                        startPayment.setEnabled(false);
                        tvCancelTransaction.setEnabled(false);
                        payStatus.setVisibility(View.VISIBLE);
                        payStatus.setText("Pay sucess!!!!");
                        closeSession();
                    }
                });
            }
            @Override
            public void onDeclined() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMerchandise.setEnabled(false);
                        tvCancelTransaction.setEnabled(true);
                        payStatus.setVisibility(View.VISIBLE);
                        payStatus.setText("Card Declined");
                        startPayment.setEnabled(true);
                    }
                });
            }
            @Override
            public void onFailure() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMerchandise.setEnabled(false);
                        tvCancelTransaction.setEnabled(true);
                        payStatus.setVisibility(View.VISIBLE);
                        payStatus.setText("Pay fail!!!!");
                        startPayment.setEnabled(true);
                    }
                });
            }
        };
        bridge.setEventsListener(paymentListener);
    }
        //Clear textView Value
        private void cleanFields() {
            quantity.setText("1");
            description.setText(" ");
            price.setText("0.00");
        }

        public void openSession() {
            if (!terminalOpen) {
                /**Starts a session through the Transaction Manager, reserving the payment terminal for this application.
                {@code bridge.startSession();}*/
                bridge.startSession();
            }
        }

        public void closeSession() {
            if (terminalOpen) {
                /**Ends the session through the Transaction Manager, unreserving the payment terminal.
                 {@code bridge.stopSession();}*/
                bridge.stopSession();
            }
        }
        public void startBasket() {
            if (!mBasketOpen) {
                bridge.startBasket();
            }
        }
        public void removeMerchandise(Merchandise merchandise) {
            bridge.deleteMerchandise(merchandise);
        }

        public void startPayment(View v) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBasketOpen) {
                        bridge.finalizeBasket();
                    }
                    bridge.startPayment();
                    startPayment.setEnabled(false);
                    addMerchandise.setEnabled(false);
                    tvCancelTransaction.setEnabled(false);
                    // mMerchandiseListView.setVisibility(View.GONE);
                    mMerchandiseListView.setAdapter(null);
                    cleanFields();
                    mMerchandiseListView.setVisibility(View.GONE);
                }
            });
        }

        public void addMerchandise(View v) {
            if (terminalOpen) {
                addMerchandise();
            }
            else
            {
                initBridge();

            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        public void addMerchandise() {
                if (mBasketOpen) {
                    if (mCurrentMerchandise == null) {
                        if (!description.getText().toString().trim().equals("") && !price.getText().toString().trim().equals("")) {
                            Merchandise merchandise = new Merchandise();
                            int qty = Integer.parseInt(quantity.getText().toString());
                            merchandise.setQuantity(qty);
                            merchandise.setDescription(description.getText().toString());//"Mobile No."+" "+
                            Double itemPrice = Double.parseDouble(price.getText().toString());
                            merchandise.setUnitPrice(new java.math.BigDecimal(itemPrice));
                            Double totalPrice = itemPrice * qty;
                            merchandise.setAmount(new java.math.BigDecimal(totalPrice));
                            merchandise.setExtendedPrice(new java.math.BigDecimal(totalPrice));
                            mCurrentMerchandise = merchandise;
                            bridge.addMerchandise(merchandise);
                            tvCancelTransaction.setEnabled(true);
                        } else {
                            Toast.makeText(this, "Enter Mobile number and Amount", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        }
        /*
        * Cancel Transaction Call
        */
        public void CancelTransaction(View v)
        {
            if (terminalOpen) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("Are you sure you want to stop this transaction?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        closeSession();
                                        addMerchandise.setEnabled(true);
                                        startPayment.setEnabled(false);
                                        payStatus.setVisibility(View.GONE);
                                        for(int i=0;i<items.size();i++) {
                                            Merchandise merchandise = items.get(i);
                                            removeMerchandise(merchandise);
                                        }
                                        items.clear();
                                        mMerchandiseListView.setVisibility(View.VISIBLE);
                                        mMerchandiseListView.setAdapter(null);
                                        cleanFields();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .show();
            }
        }

        public class MerchandiseArrayAdapter<T> extends ArrayAdapter<Merchandise> {
            private final Context context;
            private final List<Merchandise> merchandises;

            public MerchandiseArrayAdapter(Context context, List<Merchandise> m) {
                super(context, -1, m);
                this.context = context;
                this.merchandises = m;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                LinearLayout row = (LinearLayout) inflater.inflate(R.layout.merchandiselayout, parent, false);
                final Merchandise merchandise = merchandises.get(position);
                row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                if (merchandise != null) {
                    TextView qty = (TextView) row.findViewById(R.id.textViewQty);
                    String qtyStr = Integer.toString(merchandise.getQuantity());
                    qty.setText(qtyStr);
                    TextView desc = (TextView) row.findViewById(R.id.textViewDescription);
                    desc.setText(merchandise.getDescription());
                    float priceFloat = merchandise.getExtendedPrice().floatValue();
                    String priceStr = String.format("%.2f", priceFloat);
                    TextView priceTextView = (TextView) row.findViewById(R.id.textViewPrice);
                    priceTextView.setText(priceStr);
                    Button deleteButton = (Button) row.findViewById(R.id.buttonDelete);
                    final Merchandise currentMerchandize = merchandise;
                    deleteButton.setTag(position);
                    deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mCurrentMerchandise = currentMerchandize;
                            removeMerchandise(currentMerchandize);
                            Integer index = (Integer) view.getTag();
                            items.remove(index.intValue());
                            notifyDataSetChanged();
                        }
                    });
                }
                return row;
            }


        }

}
