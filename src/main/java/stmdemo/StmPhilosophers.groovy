package stmdemo

import org.multiverse.api.StmUtils;
import org.multiverse.api.references.*;
import static org.multiverse.api.StmUtils.*;

public class StmPhilosophers {

    static class Fork {
        TxnRef<String> owner = newTxnRef();
    }

    static class PhilosopherThread extends Thread {
        String name
        int meals
        Fork left
        Fork right
        TxnInteger mealsEaten = newTxnInteger(0);

        public PhilosopherThread(String name, int meals, Fork left, Fork right) {
            this.name = name
            this.meals = meals
            this.left = left
            this.right = right
        }

        @Override
        public void run() {
            for (m in 0..<meals) {
                atomic(new Runnable(){
                    public void run(){
                        // give up for a while if I can't grab forks on both sides
                        if (!(left.owner.isNull() && right.owner.isNull())) {
                            retry()
                        }

                        // grab forks!
                        left.owner.set(name)
                        right.owner.set(name)
                    }
                });

                atomic(new Runnable(){
                    public void run(){
                        mealsEaten.increment()
                        left.owner.set(null)
                        right.owner.set(null)
                    }
                });
            }
        }

        public boolean done() {
            return mealsEaten.atomicGet() >= meals
        }

        @Override
        public String toString() {
            return name + " is " + mealsEaten.atomicGet() * 100.0 / meals + "% done, done status = " + done() + " mealsEaten = " + mealsEaten

        }
    }

    static class CameraThread extends Thread {
        int interval
        List forks
        List philosophers

        public CameraThread(int interval, List forks, List philosophers) {
            this.interval = interval
            this.forks = forks
            this.philosophers = philosophers
        }

        @Override
        public void run() {
            boolean done = false
            while (!done) {
                Thread.sleep(interval);

                def (image, isDone) = getImage();
                println(image)
                done = isDone
            }
        }

        def getImage() {
            def buffer = new StringBuilder()
            def done = true

            atomic(new Runnable() {
                public void run() {
                    forks.eachWithIndex { Fork fork, int i ->
                        buffer << "fork ${i} is owned by ${fork.owner.atomicGet()}\n"
                    }

                    philosophers.each {PhilosopherThread philo ->
                        buffer << philo.toString() << "\n"
                        done = done & philo.done()
                    }
                }
            })

            return [buffer.toString(), done]
        }
    }


    static Long time(List names, int meals) {
        def forks = names.collect { new Fork() }
        def philosophers = []

        names.eachWithIndex { String name, i ->
            philosophers << new PhilosopherThread(name, meals, forks[i], forks[(i+1) % forks.size()])
        }

        def camera = new CameraThread(20, forks, philosophers)
        def start = System.currentTimeMillis()

        camera.start()

        philosophers.each { PhilosopherThread thread ->
            thread.start()
        }

        philosophers.each { PhilosopherThread thread ->
            thread.join()
        }

        long elapsed = System.currentTimeMillis() - start
        camera.join()
        return elapsed
    }

    public static void main(String[] args) {
        int meals = 100000

        long timeElapsed = time(['Aristotle', 'Plato', 'Pythagoras', 'Socrates'], meals)

        System.out.println("Each meal took " + ((timeElapsed * 1000) / meals) + " usec on average!");
    }
}