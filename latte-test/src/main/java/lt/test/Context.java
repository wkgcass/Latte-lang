package lt.test;

/**
 * test context. handles timeout and assertion errors
 */
class Context {
        private int timeout;
        private boolean finished;
        private AssertionError assertionError;

        Context(int timeout) {
                if (timeout < 0) throw new AssertionError("timeout: expected > 0 but got " + timeout);
                this.timeout = timeout;
        }

        void startAndWait() throws InterruptedException {
                long start = System.currentTimeMillis();
                while (true) {
                        if (assertionError != null) {
                                throw assertionError;
                        }
                        long current = System.currentTimeMillis();
                        if (current - start > timeout) break;
                        Thread.sleep(5);
                }
                if (finished) return;
                throw new AssertionError("timeout reached! " + timeout + " ms");
        }

        void finished() {
                this.timeout = 0;
                this.finished = true;
        }

        void assertFail(AssertionError err) {
                this.assertionError = err;
        }
}
