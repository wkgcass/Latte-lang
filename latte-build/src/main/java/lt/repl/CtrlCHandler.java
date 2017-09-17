package lt.repl;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * handle ctrl-c (INT) event
 */
public class CtrlCHandler {
        private Runnable alert = null;

        public void handle() {
                SignalHandler handler = new SignalHandler() {
                        private int count = 0;

                        @Override
                        public void handle(Signal signal) {
                                if (!signal.getName().equals("INT")) {
                                        return;
                                }
                                ++count;
                                if (count == 2) {
                                        System.exit(2); // SIGINT
                                } else {
                                        System.out.println("\n(To exit, press ^C again or type :q)");
                                        if (alert != null) {
                                                alert.run();
                                        }
                                        new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                        try {
                                                                Thread.sleep(500);
                                                        } catch (InterruptedException e) {
                                                                e.printStackTrace();
                                                        }
                                                        count = 0;
                                                }
                                        }).run();
                                }
                        }
                };
                Signal.handle(new Signal("INT"), handler);
        }

        public void onAlert(Runnable alert) {
                this.alert = alert;
        }
}
