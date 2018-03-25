package lt.repl;

/**
 * handle ctrl-c (INT) event
 */
public interface CtrlCHandler {
        interface ExitCallback {
                void exit();
        }

        void setExitCallback(ExitCallback exitCallback);

        void setAlert(Runnable alert);

        void handle();
}
