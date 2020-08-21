package weiner.noah.noshake.posenet.test.noshake;

import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

public class TimerFunctions {
    //testing scheduling a task every 4 seconds
    float startTime;
    Timer timer;
    TimerTask timerTask;

    Handler handler = new Handler();
    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 4000);
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    //this is a function to initialize the timertask with a specific runnable/action to do
    public void initializeTimerTask() {
        //instantiate a new timertask
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() { //post it to be run immediately by Looper
                    public void run() {
                        //reset the clock
                        startTime = System.currentTimeMillis();
                    }
                });
            }
        };
    }
}
