package lt.repl;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * handle ctrl-c (INT) event
 */
public class CtrlCHandlerImpl implements CtrlCHandler {
        private Runnable alert = null;
        private ExitCallback exitCallback = null;
        private final IO io;
        private int count;

        public CtrlCHandlerImpl(IO io) {
                this.io = io;
        }

        @Override
        public void setExitCallback(final ExitCallback exitCallback) {
                this.exitCallback = exitCallback;
        }

        @Override
        public void setAlert(Runnable alert) {
                this.alert = alert;
        }

        @Override
        public void handle() {
                ++count;
                if (count == 2) {
                        io.out.println();
                        exitCallback.exit();
                } else {
                        io.out.println("\n(To exit, press ^C again or type :q)");
                        if (alert != null) {
                                alert.run();
                        }
                        new Thread(new Runnable() {
                                @Override
                                public void run() {
                                        try {
                                                Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                                e.printStackTrace(io.err);
                                        }
                                        count = 0;
                                }
                        }).run();
                }
        }
}
