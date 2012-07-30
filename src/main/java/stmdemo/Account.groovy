package stmdemo;

import org.multiverse.api.StmUtils;
import org.multiverse.api.references.*;

import java.util.Date;

import static org.multiverse.api.StmUtils.*;

public class Account {

    final TxnRef<Date> lastUpdate;
    final TxnInteger balance;

    public Account(int balance){
        this.lastUpdate = StmUtils.newTxnRef(new Date());
        this.balance = newTxnInteger(balance);
    }

    public void incBalance(final int amount, final Date date){
        atomic(new Runnable(){
            public void run(){
                balance.increment(amount);
                lastUpdate.set(date);

                if(balance.get()<0){
                    System.out.println("Balance became negative: " + balance.get());
                    throw new IllegalStateException("Not enough money");
                }
            }
        });
    }

    public static void transfer(final Account from, final Account to, final int amount) {
        atomic(new Runnable(){
            public void run(){
                Date date = new Date();

                from.incBalance(-amount, date);
                to.incBalance(amount, date);
             }
        });
    }

    public static void main(String[] args) {
        final Account adam = new Account(5000);
        final Account betty = new Account(5000);
        final Account calvin = new Account(50000);

        Thread payToAdam = new Thread() {
            @Override
            public void run() {
                while (true) {
                    transfer(calvin, adam, 1000);
                    Thread.sleep(200);
                    println("\nCalvin has " + calvin.balance.atomicGet())
                    println("Adam has " + adam.balance.atomicGet());
                }
            }
        };

        Thread payToBetty = new Thread() {
            @Override
            public void run() {
                while (true) {
                    transfer(calvin, betty, 1000);
                    Thread.sleep(400);
                    println("\nCalvin has " + calvin.balance.atomicGet())
                    println("Betty  has " + betty.balance.atomicGet());
                }
            }
        };

        payToAdam.start()
        payToBetty.start()

        payToAdam.join()
        payToBetty.join()

        println("\nAdam has ${adam.balance.atomicGet()}, Betty has ${betty.balance.atomicGet()}, Calvin has ${calvin.balance.atomicGet()}");
    }

}
