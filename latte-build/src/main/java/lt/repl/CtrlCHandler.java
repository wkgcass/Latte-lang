package lt.repl;

/**
 * handle ctrl-c (INT) event
 */
public interface CtrlCHandler {
        void handle();

        void onAlert(Runnable alert);
}
